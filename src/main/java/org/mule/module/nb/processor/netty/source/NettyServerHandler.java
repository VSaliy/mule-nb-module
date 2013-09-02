package org.mule.module.nb.processor.netty.source;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.nb.MessageProcessorCallback;
import org.mule.module.nb.MuleEventFactory;
import org.mule.module.nb.processor.NBMessageProcessor;
import org.mule.util.ExceptionUtils;

import java.io.InputStream;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.stream.ChunkedStream;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

public class NettyServerHandler extends SimpleChannelUpstreamHandler
{


    private NBMessageProcessor nbMessageProcessor;

    protected MuleEventFactory muleEventFactory = null;

    public NettyServerHandler(MuleEventFactory muleEventFactory, NBMessageProcessor nbMessageProcessor)
    {
        this.muleEventFactory = muleEventFactory;
        this.nbMessageProcessor = nbMessageProcessor;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event)
    {
        final HttpRequest request = (HttpRequest) event.getMessage();


        final Channel channel = event.getChannel();
        try
        {
            // TODO create a NettyMuleMessageFactory
            final MuleEvent muleEvent = muleEventFactory.create(request, Charset.defaultCharset().name());
            nbMessageProcessor.process(muleEvent, new MessageProcessorCallback()
            {
                @Override
                public void onSuccess(MuleEvent result)
                {

                    try
                    {
                        final MuleMessage message = result.getMessage();
                        final ExceptionPayload exceptionPayload = message.getExceptionPayload();
                        if (exceptionPayload == null)
                        {
                            // happy path with no errors
                            final Object payload = message.getPayload();
                            if (payload instanceof InputStream)
                            {
                                // wrap incoming streams
                                final ChunkedStream stream = new ChunkedStream(message.getPayload(InputStream.class));
                                channel.getPipeline().addLast("streamer", new ChunkedWriteHandler());
                                channel.write(stream).addListener(ChannelFutureListener.CLOSE);
                            }
                            else if (payload instanceof ChunkedStream)
                            {
                                // chunked stream doesn't implement InputStream
                                channel.getPipeline().addLast("streamer", new ChunkedWriteHandler());
                                channel.write(payload).addListener(ChannelFutureListener.CLOSE);
                            }
                            else if (payload instanceof ChannelBuffer)
                            {
                                // native ChannelBuffer format
                                channel.write(payload).addListener(ChannelFutureListener.CLOSE);
                            }
                            else
                            {
                                // otherwise dump bytes
                                final ChannelBuffer out = ChannelBuffers.wrappedBuffer(message.getPayloadAsBytes());
                                channel.write(out).addListener(ChannelFutureListener.CLOSE);

                            }
                        }
                        else
                        {
                            // got an exception payload in the response
                            // send an error message from the root exception
                            channel.getPipeline().addLast("encoder", new StringEncoder(Charset.defaultCharset()));
                            final String rootCause = ExceptionUtils.getRootCauseMessage(exceptionPayload.getException());

                            // TODO check bytes encoding
                            channel.write(rootCause).addListener(ChannelFutureListener.CLOSE);
                        }
                    }
                    catch (Exception e)
                    {
                        //Todo Handle exception
                        e.printStackTrace();
                    }
                    finally
                    {
                        channel.close();
                    }
                }
                @Override
                public void onException(MuleEvent event, MuleException e)
                {
                    channel.close();
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            channel.close();
        }

    }


}


