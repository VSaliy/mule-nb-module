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


        Path absolutePath = (Path) event.getMessage().getPayload();

        AsynchronousFileChannel fileChannel = null;
        try
        {
            fileChannel = AsynchronousFileChannel.open(Paths.get(absolutePath.toUri()), EnumSet.of(StandardOpenOption.READ), ioExecutorService);
        }
        catch (IOException e)
        {
            callback.onException(event, new DefaultMuleException(e));
        }

        //TODO(pablo.kraan): need to manage the case where the buffer is not big engouh to hold the full file
        //TODO(pablo.kraan): need support for streaming?
        ByteBuffer buf = ByteBuffer.allocate(1024);


        //System.out.println("Starting read....");
        fileChannel.read(buf, 0, fileChannel, new FileReadCompletionHandler(event, callback, absolutePath, buf));
        //System.out.println("Read started");
    }

    public void initialise() throws InitialisationException
    {
        ioExecutorService = Executors.newFixedThreadPool(8, new NamedThreadFactory(
                String.format("%s%s.io", ThreadNameHelper.getPrefix(getMuleContext()), "FileToString"),
                getMuleContext().getExecutionClassLoader()
        ));

        dispatcherExecutorService = Executors.newFixedThreadPool(8, new NamedThreadFactory(
                String.format("%s%s.dispatcher", ThreadNameHelper.getPrefix(getMuleContext()), "FileToString"),
                getMuleContext().getExecutionClassLoader()
        ));
    }


    private class FileReadCompletionHandler implements CompletionHandler<Integer, AsynchronousFileChannel>
    {

        private MuleEvent event;
        private MessageProcessorCallback callback;
        private Path path;
        private StringBuilder builder;
        private ByteBuffer buffer;
        int pos = 0;

        public FileReadCompletionHandler(MuleEvent event, MessageProcessorCallback callback, Path path, ByteBuffer buffer)
        {
            this.event = event;
            this.callback = callback;
            this.path = path;
            builder = new StringBuilder();

            this.buffer = buffer;
        }

        public void completed(Integer result, AsynchronousFileChannel attachment)
        {
            System.out.println("Thread name: " + Thread.currentThread().getName());
            //System.out.println("Bytes read = " + result);
            if (result != -1)
            {

                builder.append(new String(buffer.array()));

                pos += result;  // don't read the same text again.
                ////System.out.println(new String(buffer.array()));
                buffer.clear();  // reset the buffer so you can read more.

                // initiate another asynchronous read, with this.
                attachment.read(buffer, pos, attachment, this);
            }
            else
            {
                dispatcherExecutorService.submit(new Callable<Void>()
                {
                    public Void call() throws Exception
                    {
                        event.getMessage().setPayload(builder.toString());
                        callback.onSuccess(event);

                        return null;
                    }
                });
            }
        }

        public void failed(Throwable exc, AsynchronousFileChannel attachment)
        {
        }
    }
}
