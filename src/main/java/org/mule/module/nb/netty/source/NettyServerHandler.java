package org.mule.module.nb.netty.source;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.nb.MuleEventFactory;
import org.mule.module.nb.processor.MessageProcessorCallback;
import org.mule.module.nb.processor.NBMessageProcessor;
import org.mule.transport.http.HttpConnector;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
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

        System.out.println("NettyServerHandler.messageReceived");

        final Channel channel = event.getChannel();
        try
        {
            final MuleEvent muleEvent = muleEventFactory.create(request, Charset.defaultCharset().name());
            nbMessageProcessor.process(muleEvent, new MessageProcessorCallback()
            {
                @Override
                public void onSuccess(MuleEvent result)
                {
                    try
                    {
                        final MuleMessage message = result.getMessage();
                        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                        response.setHeader("Content-Type", message.getOutboundProperty("Content-Type"));
                        Object outboundProperty = message.getOutboundProperty(HttpConnector.HTTP_HEADERS);
                        if (outboundProperty instanceof Map)
                        {
                            Set<Map.Entry<String, Object>> entries = ((Map<String, Object>) outboundProperty).entrySet();
                            for (Map.Entry<String, Object> entry : entries)
                            {
                                response.setHeader(entry.getKey(), entry.getValue());
                            }
                        }
                        response.setContent(ChannelBuffers.copiedBuffer(message.getPayload().toString(), CharsetUtil.UTF_8));
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
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception
    {
        System.out.println("NettyServerHandler.channelClosed");
        //super.channelClosed(ctx, e);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        System.out.println("NettyServerHandler.exceptionCaught");
        e.getCause().printStackTrace();
        //super.exceptionCaught(ctx, e);
    }
}


