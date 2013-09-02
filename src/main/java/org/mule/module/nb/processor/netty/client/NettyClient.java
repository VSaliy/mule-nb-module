/**
 *
 */
package org.mule.module.nb.processor.netty.client;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.nb.MessageProcessorCallback;
import org.mule.module.nb.processor.NBMessageProcessor;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.io.IOException;

public class NettyClient implements NBMessageProcessor
{

    private String url;

    @Override
    public void process(final MuleEvent event, final MessageProcessorCallback callback)
    {
        AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
        try
        {
            asyncHttpClient.prepareGet(url).execute(new AsyncCompletionHandler<Response>()
            {

                @Override
                public Response onCompleted(Response response) throws Exception
                {
                    event.getMessage().setPayload(response);
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

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}
