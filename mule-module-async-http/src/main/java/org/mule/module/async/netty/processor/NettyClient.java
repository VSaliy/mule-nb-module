/**
 *
 */
package org.mule.module.async.netty.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.transport.PropertyScope;
import org.mule.module.async.internal.processor.AbstractAsyncMessageProcessor;
import org.mule.module.async.netty.utils.NettyUtils;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;

import java.io.IOException;

import org.jboss.netty.handler.codec.http.HttpMethod;

public class NettyClient extends AbstractAsyncMessageProcessor implements Lifecycle
{

    private String baseUrl;
    private String uri;
    private String method;
    private AsyncHttpClient asyncHttpClient;


    public NettyClient()
    {
        uri = "#[payload]";
        method = HttpMethod.GET.getName();
    }


    @Override
    public void process(final MuleEvent event, final MessageProcessorCallback callback)
    {
        final String uriEvaluated = getMuleContext().getExpressionManager().parse(uri, event).toString();

        try
        {

            final String url = baseUrl + uriEvaluated;

            AsyncHttpClient.BoundRequestBuilder requestBuilder = null;

            if (method.equalsIgnoreCase(HttpMethod.POST.getName()))
            {
                requestBuilder = asyncHttpClient.preparePost(url);
            }
            else if (method.equalsIgnoreCase(HttpMethod.GET.getName()))
            {
                requestBuilder = asyncHttpClient.prepareGet(url);
            }
            else if (method.equalsIgnoreCase(HttpMethod.PUT.getName()))
            {
                requestBuilder = asyncHttpClient.preparePut(url);
            }
            else if (method.equalsIgnoreCase(HttpMethod.OPTIONS.getName()))
            {
                requestBuilder = asyncHttpClient.prepareOptions(url);
            }
            else if (method.equalsIgnoreCase(HttpMethod.HEAD.getName()))
            {
                requestBuilder = asyncHttpClient.prepareHead(url);
            }
            else if (method.equalsIgnoreCase(HttpMethod.DELETE.getName()))
            {
                requestBuilder = asyncHttpClient.prepareDelete(url);
            }



            requestBuilder.execute(new ResponseAsyncCompletionHandler(event, callback));
        }
        catch (IOException e)
        {
            callback.onException(event, new MessagingException(event, e, this));
        }
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

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }


    @Override
    public void dispose()
    {
    }

    @Override
    public void initialise() throws InitialisationException
    {
        AsyncHttpClientConfig.Builder builder = new AsyncHttpClientConfig.Builder();
        asyncHttpClient = new AsyncHttpClient();
    }

    @Override
    public void start() throws MuleException
    {
    }

    @Override
    public void stop() throws MuleException
    {
    }

    private class ResponseAsyncCompletionHandler extends AsyncCompletionHandler<Response>
    {

        private final MuleEvent event;
        private final MessageProcessorCallback callback;

        public ResponseAsyncCompletionHandler(MuleEvent event, MessageProcessorCallback callback)
        {
            this.event = event;
            this.callback = callback;
        }

        @Override
        public Response onCompleted(Response response) throws Exception
        {

            final NettyUtils nettyUtils = new NettyUtils();
            event.getMessage().setPayload(response.getResponseBody());
            final FluentCaseInsensitiveStringsMap headers = response.getHeaders();
            final String contentType = response.getContentType();
            event.getMessage().setProperty(HttpConnector.HTTP_HEADERS, headers, PropertyScope.OUTBOUND);
            event.getMessage().setProperty(HttpConstants.HEADER_CONTENT_TYPE, contentType, PropertyScope.OUTBOUND);
            event.getMessage().setEncoding(nettyUtils.getEncoding(contentType));
            callback.onSuccess(event);
            return response;
        }

        @Override
        public void onThrowable(Throwable t)
        {
            callback.onException(event, new MessagingException(event, t, NettyClient.this));
        }
    }
}
