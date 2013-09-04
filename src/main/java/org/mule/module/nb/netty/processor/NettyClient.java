/**
 *
 */
package org.mule.module.nb.netty.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transport.PropertyScope;
import org.mule.module.nb.processor.MessageProcessorCallback;
import org.mule.module.nb.processor.NBMessageProcessor;
import org.mule.transport.http.HttpConnector;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;

import java.io.IOException;

public class NettyClient implements NBMessageProcessor, MuleContextAware
{

    private String baseUrl;
    private String uri;
    private MuleContext context;

    public NettyClient()
    {
        uri = "#[payload]";
    }

    @Override
    public void process(final MuleEvent event, final MessageProcessorCallback callback)
    {
        String uriEvaluated = context.getExpressionManager().parse(uri, event).toString();
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        try
        {
            String url = baseUrl + uriEvaluated;
            System.out.println("url = " + url);
            asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>()
            {

                @Override
                public Response onCompleted(Response response) throws Exception
                {

                    event.getMessage().setPayload(response.getResponseBody());
                    FluentCaseInsensitiveStringsMap headers = response.getHeaders();
                    String contentType = response.getContentType();
                    event.getMessage().setProperty(HttpConnector.HTTP_HEADERS, headers, PropertyScope.OUTBOUND);
                    event.getMessage().setProperty("Content-Type", contentType, PropertyScope.OUTBOUND);
                    callback.onSuccess(event);
                    return response;
                }

                @Override
                public void onThrowable(Throwable t)
                {
                    callback.onException(event, new MessagingException(event, t, NettyClient.this));
                }
            });
        }
        catch (IOException e)
        {
            callback.onException(event, new MessagingException(event, e, this));
        }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return null;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri(String uri)
    {
        this.uri = uri;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }
}
