package org.mule.module.async.netty.source;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.async.internal.MuleEventFactory;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;

import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.util.CharsetUtil;

public class NettyServerHandler extends SimpleChannelUpstreamHandler
{


    private AsyncMessageProcessor asyncMessageProcessor;

    protected MuleEventFactory muleEventFactory = null;

    public NettyServerHandler(MuleEventFactory muleEventFactory, AsyncMessageProcessor asyncMessageProcessor)
    {
        this.muleEventFactory = muleEventFactory;
        this.asyncMessageProcessor = asyncMessageProcessor;
    }


    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event)
    {
        final HttpRequest request = (HttpRequest) event.getMessage();

        System.out.println("NettyServerHandler.messageReceived");

        final Channel channel = event.getChannel();
        try
        {
            final MuleEvent muleEvent = muleEventFactory.create(request, Charset.defaultCharset().name());
            asyncMessageProcessor.process(muleEvent, new MessageProcessorCallback()
            {
                @Override
                public void onSuccess(MuleEvent result)
                {
                    try
                    {
                        final MuleMessage message = result.getMessage();
                        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                        Object content_type = message.getOutboundProperty("Content-Type");
                        if (content_type != null)
                        {
                            content_type = "text/plain";
                        }
                        response.setHeader("Content-Type", content_type);
                        response.setContent(ChannelBuffers.copiedBuffer(message.getPayload().toString(), CharsetUtil.ISO_8859_1));
                        channel.write(response).addListener(ChannelFutureListener.CLOSE);
                    }
                    catch (Exception e)
                    {
                        //Todo Handle exception
                        e.printStackTrace();
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

        }
        System.out.println("NettyServerHandler.messageReceived + finished");

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        System.out.println("NettyServerHandler.exceptionCaught");
        e.getCause().printStackTrace();
    }
}


