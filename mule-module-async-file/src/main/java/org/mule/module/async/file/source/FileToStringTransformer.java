package org.mule.module.async.file.source;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
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
import java.util.EnumSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 */
public class FileToStringTransformer extends AbstractAsyncMessageProcessor implements Initialisable
{


    private ExecutorService ioExecutorService;
    //TODO(pablo.kraan): move this executor to the sasync flow
    private ExecutorService dispatcherExecutorService;

    public void process(MuleEvent event, MessageProcessorCallback callback)
    {

        AsynchronousFileChannel fileChannel;
        try
        {
            Path absolutePath = (Path) event.getMessage().getPayload();
            fileChannel = AsynchronousFileChannel.open(Paths.get(absolutePath.toUri()), EnumSet.of(StandardOpenOption.READ), ioExecutorService);
            //TODO(pablo.kraan): need to manage the case where the buffer is not big engouh to hold the full file
            //TODO(pablo.kraan): need support for streaming?
            ByteBuffer buf = ByteBuffer.allocate(1024);
            fileChannel.read(buf, 0, fileChannel, new FileReadCompletionHandler(event, callback, buf));
        }
        catch (Exception e)
        {
            callback.onException(event, new DefaultMuleException(e));
        }


    }

    public void initialise() throws InitialisationException
    {
        ioExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, new NamedThreadFactory(
                String.format("%s%s.io", ThreadNameHelper.getPrefix(getMuleContext()), "FileToString"),
                getMuleContext().getExecutionClassLoader()
        ));

        dispatcherExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, new NamedThreadFactory(
                String.format("%s%s.dispatcher", ThreadNameHelper.getPrefix(getMuleContext()), "FileToString"),
                getMuleContext().getExecutionClassLoader()
        ));
    }


    private class FileReadCompletionHandler implements CompletionHandler<Integer, AsynchronousFileChannel>
    {

        private MuleEvent event;
        private MessageProcessorCallback callback;
        private StringBuilder builder;
        private ByteBuffer buffer;
        private int pos = 0;

        public FileReadCompletionHandler(MuleEvent event, MessageProcessorCallback callback, ByteBuffer buffer)
        {
            this.event = event;
            this.callback = callback;
            this.builder = new StringBuilder();
            this.buffer = buffer;
        }

        public void completed(Integer result, AsynchronousFileChannel channel)
        {
            // System.out.println("Thread name: " + Thread.currentThread().getName());
            //System.out.println("Bytes read = " + result);
            if (result != -1)
            {
                builder.append(new String(buffer.array()));
                pos += result;  // don't read the same text again.
                buffer.clear();  // reset the buffer so you can read more.
                // initiate another asynchronous read, with this.
                channel.read(buffer, pos, channel, this);
            }
            else
            {
                close(channel);
                final String payload = builder.toString();
                dispatcherExecutorService.submit(new Callable<Void>()
                {
                    public Void call() throws Exception
                    {
                        event.getMessage().setPayload(payload);
                        callback.onSuccess(event);
                        return null;
                    }
                });
                builder = null;

            }
        }

        private void close(AsynchronousFileChannel attachment)
        {
            try
            {
                attachment.close();
            }
            catch (IOException e)
            {

            }
        }

        public void failed(Throwable exc, AsynchronousFileChannel attachment)
        {
        }
    }
}
