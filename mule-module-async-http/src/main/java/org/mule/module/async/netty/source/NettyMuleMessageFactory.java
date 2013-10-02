/**
 *
 */
package org.mule.module.async.netty.source;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.transport.DefaultMuleMessageFactory;
import org.mule.transport.http.HttpConnector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class NettyMuleMessageFactory extends DefaultMuleMessageFactory
{



    public NettyMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class<?>[] {HttpRequest.class};
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        final HttpRequest request = (HttpRequest) transportMessage;
        final List<Map.Entry<String, String>> headers = request.getHeaders();
        final Map<String, Object> inboundProperties = new HashMap<String, Object>();
        for (Map.Entry<String, String> header : headers)
        {
            inboundProperties.put(header.getKey(), header.getValue());
        }

        inboundProperties.put(HttpConnector.HTTP_METHOD_PROPERTY, request.getMethod());
        inboundProperties.put(HttpConnector.HTTP_REQUEST_PROPERTY, request.getUri());
        inboundProperties.put(HttpConnector.HTTP_VERSION_PROPERTY, request.getProtocolVersion().toString());

        message.addInboundProperties(inboundProperties);
    }



    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        Object payload;
        HttpRequest request = (HttpRequest) transportMessage;


        // If http method is GET we use the request uri as the payload.
        if (request.getMethod().equals(HttpMethod.GET))
        {
            payload = request.getUri();
        }
        else
        {
            payload = request.getContent();
        }

        return payload;
    }
}
