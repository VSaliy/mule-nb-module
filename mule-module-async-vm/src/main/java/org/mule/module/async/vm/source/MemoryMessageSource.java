package org.mule.module.async.vm.source;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.module.async.AddressAwareMessageSource;
import org.mule.module.async.internal.DefaultMuleEventFactory;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.transport.DefaultMuleMessageFactory;
import org.mule.util.concurrent.ConcurrentHashSet;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.ThreadNameHelper;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemoryMessageSource implements AddressAwareMessageSource, Initialisable, Startable, Stoppable, MuleContextAware, FlowConstructAware
{

    private MessageProcessor messageProcessor;
    private FlowConstruct flowConstruct;
    private MuleContext muleContext;
    private Thread watcherWorker;
    private DefaultMuleEventFactory muleEventFactory;

    private ExecutorService dispatcherExecutorService;

    private ListableObjectStore<Serializable> objectStore;

    public MemoryMessageSource()
    {
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setPath(String path)
    {
        this.path = path;
    }

    private String path;

    @SuppressWarnings("UnusedDeclaration")
    public void setStore(ListableObjectStore<Serializable> objectStore)
    {
        this.objectStore = objectStore;
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        messageProcessor = listener;
    }


    @Override
    public void initialise() throws InitialisationException
    {
        dispatcherExecutorService = Executors.newFixedThreadPool(8, new NamedThreadFactory(
                String.format("%s%s.dispatcher", ThreadNameHelper.getPrefix(muleContext), "File"),
                muleContext.getExecutionClassLoader()
        ));

        try
        {
            muleEventFactory = new DefaultMuleEventFactory(new DefaultMuleMessageFactory(muleContext), getUri(), flowConstruct, MessageExchangePattern.REQUEST_RESPONSE);
        }
        catch (URISyntaxException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public URI getUri() throws URISyntaxException
    {
        return new URI("memory://local");
    }

    @Override
    public void start() throws MuleException
    {
        watcherWorker = new Thread(new FolderPoller());
        watcherWorker.setName("folder-poller");
        watcherWorker.start();
    }

    @Override
    public void stop() throws MuleException
    {

    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public String getAddress()
    {
        return Paths.get(path).toUri().toString();
    }

    public class FolderPoller implements Runnable
    {

        public void run()
        {
            ConcurrentHashSet processingFiles = new ConcurrentHashSet();

            while (true)
            {
                poll(processingFiles);

                try
                {
                    //TODO(pablo.kraan): add a poll attribute
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private void poll(final ConcurrentHashSet processingFiles)
        {
            final List<Serializable> keys;
            try
            {
                keys = objectStore.allKeys();
            }
            catch (ObjectStoreException e)
            {
                //TODO(pablo.kraan): invoke system exception strategy
                e.printStackTrace();
                throw new IllegalStateException(e);
            }

            for (final Serializable key : keys)
            {
                if (!processingFiles.add(key))
                {
                    continue;
                }

                dispatcherExecutorService.submit(new Callable<Void>()
                {
                    public Void call() throws Exception
                    {
                        //TODO(pablo.kraan): fix enconding
                        final Serializable payload = objectStore.remove(key);
                        final MuleEvent muleEvent = muleEventFactory.create(payload, "UTF-8");
                        muleEvent.getMessage().setInvocationProperty("key", key);

                        if (messageProcessor instanceof AsyncMessageProcessor)
                        {
                            final AsyncMessageProcessor asyncMessageProcessor = (AsyncMessageProcessor) messageProcessor;

                            asyncMessageProcessor.process(muleEvent, new MessageProcessorCallback()
                            {
                                public void onSuccess(MuleEvent event)
                                {
                                    processingFiles.remove(key);
                                }

                                public void onException(MuleEvent event, MuleException e)
                                {
                                    processingFiles.remove(key);
                                }
                            });

                            return null;
                        }
                        else
                        {
                            messageProcessor.process(muleEvent);
                            return null;
                        }
                    }
                });

            }
        }
    }

}
