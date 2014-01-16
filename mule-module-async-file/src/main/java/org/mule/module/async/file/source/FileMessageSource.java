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
    private ExecutorService dispatcherExecutorService;

    //Convert this two
    public static final int DEFAULT_MAX_CONCURRENT_FILES = Runtime.getRuntime().availableProcessors() * 2;
    public static final int DEFAULT_POLLING_INTERVAL = 1000;

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
        watcherWorker.setName("Folder-Listener");
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

        private Object monitor = new Object();
        private ConcurrentHashSet processingFiles = new ConcurrentHashSet();

        public void run()
        {
            while (true)
            {
                if (checkMaxConcurrentFiles())
                {
                    poll();
                }
                try
                {
                    //TODO(pablo.kraan): add a poll attribute
                    Thread.sleep(DEFAULT_POLLING_INTERVAL);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        private boolean checkMaxConcurrentFiles()
        {
            return processingFiles.size() < DEFAULT_MAX_CONCURRENT_FILES;
        }

        private void poll()
        {
            final File folder = new File(path);
            try
            {
                final File[] files = folder.listFiles();

                for (int i = 0; i < files.length; i++)
                {
                    while (!checkMaxConcurrentFiles())
                    {
                        synchronized (monitor)
                        {
                            monitor.wait();
                        }
                    }
                    File file = files[i];

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

                        processFile(file);
                    }
                }
            }
            catch (Exception e)
            {
                //TODO(pablo.kraan): need an exception strategy?
                //getConnector().getMuleContext().getExceptionListener().handleException(e);
            }
        }

        private void processFile(final File file)
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

                            onFileProcessed(file);
                        }

                        public void onException(MuleEvent event, MuleException e)
                        {
                            onFileProcessed(file);
                        }
                    });

                    return null;
                }
            });
        }

        private void onFileProcessed(File file)
        {
            file.delete();
            processingFiles.remove(file.getName());
            synchronized (monitor)
            {
                monitor.notifyAll();
            }
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
