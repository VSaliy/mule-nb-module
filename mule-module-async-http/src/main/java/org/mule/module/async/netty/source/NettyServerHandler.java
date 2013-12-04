package org.mule.module.async.netty.source;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.async.internal.MuleEventFactory;
import org.mule.module.async.netty.utils.NettyUtils;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
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

public class NettyServerHandler extends SimpleChannelUpstreamHandler
{


    protected final transient Log log = LogFactory.getLog(getClass());

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
        final NettyUtils nettyUtils = new NettyUtils();
        final Channel channel = event.getChannel();

        try
        {
            final MuleEvent muleEvent = muleEventFactory.create(request, nettyUtils.getEncoding(request));
            asyncMessageProcessor.process(muleEvent, new MessageProcessorCallback()
            {
                @Override
                public void onSuccess(MuleEvent result)
                {
                    try
                    {
                        final MuleMessage message = result.getMessage();
                        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                        Object content_type = message.getOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
                        if (content_type == null)
                        {
                            content_type = "text/plain";
                        }
                        response.setHeader(HttpConstants.HEADER_CONTENT_TYPE, content_type);
                        Map<String, Object> headers = message.getOutboundProperty(HttpConnector.HTTP_HEADERS);
                        if (headers != null)
                        {
                            for (Map.Entry<String, Object> header : headers.entrySet())
                            {
                                Object value = header.getValue();
                                if (value instanceof Iterable)
                                {
                                    response.setHeader(header.getKey(), (Iterable) value);
                                }
                                else
                                {
                                    response.setHeader(header.getKey(), value);
                                }
                            }
                        }
                        ChannelBuffer content = ChannelBuffers.copiedBuffer(message.getPayload().toString(), Charset.forName(result.getMessage().getEncoding()));
                        response.setContent(content);
                        Integer status = message.getProperty(HttpConnector.HTTP_STATUS_PROPERTY, PropertyScope.OUTBOUND);
                        if (status != null)
                        {
                            response.setStatus(HttpResponseStatus.valueOf(status));
                        }
                        channel.write(response).addListener(ChannelFutureListener.CLOSE);
                    }
                    catch (Exception e)
                    {
                        log.error("Un expected exception while writing response", e);
                    }

                }

                @Override
                public void onException(MuleEvent event, MuleException e)
                {
                    HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
                    response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                    response.setHeader("Content-Type", "text/plain");

                    response.setContent(ChannelBuffers.copiedBuffer(e.getMessage(), Charset.forName(event.getMessage().getEncoding())));
                    channel.write(response).addListener(ChannelFutureListener.CLOSE);

                }
            });
        }
        catch (Exception e)
        {
            log.error("Un expected exception while writing response", e);
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
    {
        log.error("Un expected exception on channel", e.getCause());
    }
}


