/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;

public class RestAsyncMessageProcessor implements AsyncMessageProcessor
{

    private String url;

    @Override
    public void process(MuleEvent event, MessageProcessorCallback callback)
    {
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
