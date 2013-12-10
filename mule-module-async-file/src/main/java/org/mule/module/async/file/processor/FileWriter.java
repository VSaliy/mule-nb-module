package org.mule.module.async.file.processor;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.async.processor.AbstractAsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.ThreadNameHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileWriter extends AbstractAsyncMessageProcessor implements Initialisable
{

    private String path;
    private ExecutorService ioExecutorService;

    private void closeChannel(AsynchronousFileChannel fileChannel)
    {
        try
        {
            fileChannel.close();
        }
        catch (IOException e)
        {
            //TODO(pablo.kraan): logger error/use exception strategy
            e.printStackTrace();
        }
    }

    public void process(final MuleEvent event, final MessageProcessorCallback callback)
    {

        try
        {
            final ExpressionManager expressionManager = event.getMuleContext().getExpressionManager();
            Path filePath;
            if (expressionManager.isExpression(path))
            {
                final String evaluatedPath = (String) expressionManager.parse(path, event);
                filePath = Paths.get(evaluatedPath);
            }
            else
            {
                filePath = Paths.get(path);
            }
            //TODO(pablo.kraan): needs a executor service
            Set<StandardOpenOption> openOptions = new HashSet<StandardOpenOption>();
            openOptions.add(StandardOpenOption.WRITE);
            openOptions.add(StandardOpenOption.CREATE);

            final AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(filePath, openOptions, ioExecutorService);

            CompletionHandler<Integer, Object> handler = new CompletionHandler<Integer, Object>()
            {
                @Override
                public void completed(Integer result, Object attachment)
                {
                    try
                    {
                        //System.out.println("Thread: " + Thread.currentThread().getName() + " File Write Completed with Result:"
                        //                   + result);
                        callback.onSuccess(event);
                    }
                    finally
                    {
                        closeChannel(fileChannel);
                    }
                }

                @Override
                public void failed(Throwable e, Object attachment)
                {
                    try
                    {
                        System.err.println("File Write Failed Exception:");
                        e.printStackTrace();
                        callback.onException(event, new DefaultMuleException(e));
                    }
                    finally
                    {
                        closeChannel(fileChannel);
                    }
                }
            };

            fileChannel.write(ByteBuffer.wrap(event.getMessage().getPayloadAsString().getBytes()), 0, null, handler);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error writing file", e);
        }
        finally
        {

        }
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public void initialise() throws InitialisationException
    {
        ioExecutorService = Executors.newFixedThreadPool(8, new NamedThreadFactory(
                String.format("%s%s.io", ThreadNameHelper.getPrefix(getMuleContext()), "FileToString"),
                getMuleContext().getExecutionClassLoader()
        ));
    }
}
