package org.mule.module.async.file.source;

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
import org.mule.module.async.AddressAwareMessageSource;
import org.mule.module.async.internal.DefaultMuleEventFactory;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.transport.DefaultMuleMessageFactory;
import org.mule.util.concurrent.ConcurrentHashSet;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.util.lock.LockFactory;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileMessageSource implements AddressAwareMessageSource, Initialisable, Startable, Stoppable, MuleContextAware, FlowConstructAware
{

    private AsyncMessageProcessor asyncMessageProcessor;
    private FlowConstruct flowConstruct;
    private MuleContext muleContext;
    private Thread watcherWorker;
    private DefaultMuleEventFactory muleEventFactory;
    private Path dirPath;
    private LockFactory lockFactory;

    private ExecutorService dispatcherExecutorService;

    public FileMessageSource()
    {
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    private String path;

    @Override
    public void setListener(MessageProcessor listener)
    {
        asyncMessageProcessor = (AsyncMessageProcessor) listener;
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
        return Paths.get(path).toUri();
    }

    @Override
    public void start() throws MuleException
    {
        dirPath = Paths.get(path);

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
        lockFactory = muleContext.getLockFactory();
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
            while (true)
            {
                poll();

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

        private void poll()
        {
            File folder = new File(path);
            ConcurrentHashSet processingFiles = new ConcurrentHashSet();

            try
            {
                File[] files = folder.listFiles();

                for (File file : files)
                {

                    // don't process directories
                    if (file.isFile())
                    {
                        final String fileName = file.getName();
                        //TODO(pablo.kraan): use an object store to avoid processing the same file twice
                        //TODO(pablo.kraan): add file locking
                        if (!processingFiles.add(fileName))
                        {
                            continue;
                        }

                        if (file.exists())
                        {
                            processFile(file, processingFiles);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                //TODO(pablo.kraan): need an exception strategy?
                //getConnector().getMuleContext().getExceptionListener().handleException(e);
            }
        }

        private void processFile(final File file, final ConcurrentHashSet processingFiles)
        {

            //TODO(pablo.kraan): add fileAge
            //if (!isAgedFile(file, 500))
            //{
            //    processingFiles.remove(file.getName());
            //    return;
            //}

            final Path absolutePath = dirPath.resolve(file.getName());

            dispatcherExecutorService.submit(new Callable<Void>()
            {
                public Void call() throws Exception
                {
                    //TODO(pablo.kraan): fix enconding
                    final MuleEvent muleEvent = muleEventFactory.create(absolutePath, "UTF-8");
                    asyncMessageProcessor.process(muleEvent, new MessageProcessorCallback()
                    {
                        public void onSuccess(MuleEvent event)
                        {
                            //fileLock.unlock();
                            boolean deleted = file.delete();
                            //System.out.println(String.format("File '%s' deleted: %s", path.toString(), deleted));

                            processingFiles.remove(file.getName());


                        }

                        public void onException(MuleEvent event, MuleException e)
                        {
                            processingFiles.remove(file.getName());
                        }
                    });

                    return null;
                }
            });
        }
    }

    protected boolean isAgedFile(File file, long fileAge)
    {
        final long lastMod = file.lastModified();
        final long now = System.currentTimeMillis();
        final long thisFileAge = now - lastMod;

        if (thisFileAge < fileAge)
        {
            //if (logger.isDebugEnabled())
            //{
            //    logger.debug("The file has not aged enough yet, will return nothing for: " + file);
            //}

            return false;
        }

        return true;
    }
}
