/**
 *
 */
package org.mule.module.async.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

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
