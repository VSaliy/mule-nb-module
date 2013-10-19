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

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;

public class NettyMuleMessageFactory extends DefaultMuleMessageFactory
{


    public static final String HTTP_CONTENT_LENGTH = HttpConnector.HTTP_PREFIX + "content-length";

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
        final Map<String, String> headersMap = new HashMap<String, String>();
        for (Map.Entry<String, String> header : headers)
        {

            headersMap.put(header.getKey(), header.getValue());
        }

        //Fill All the properties
        inboundProperties.put(HttpConnector.HTTP_QUERY_PARAMS, HttpUriHelper.parseQueryParams(request.getUri(), message.getEncoding()));
        inboundProperties.put(HttpConnector.HTTP_QUERY_STRING, HttpUriHelper.parseQueryString(request.getUri()));
        inboundProperties.put(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, HttpUriHelper.parsePath(request.getUri()));
        inboundProperties.put(HTTP_CONTENT_LENGTH, HttpHeaders.getContentLength(request));
        //Add them at root level and under http.headers. Is ugly but is the mule way :(
        inboundProperties.put(HttpConnector.HTTP_HEADERS, headersMap);
        inboundProperties.putAll(headersMap);
        inboundProperties.put(HttpConnector.HTTP_METHOD_PROPERTY, request.getMethod().getName());
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
