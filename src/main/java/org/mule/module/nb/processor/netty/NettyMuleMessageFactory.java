/**
 *
 */
package org.mule.module.nb.processor.netty;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.transport.DefaultMuleMessageFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        message.addInboundProperties(inboundProperties);
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        HttpRequest request = (HttpRequest) transportMessage;
        return request.getContent();
    }
}
