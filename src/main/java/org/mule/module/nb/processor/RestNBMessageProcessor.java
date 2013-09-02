/**
 *
 */
package org.mule.module.nb.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.nb.MessageProcessorCallback;

public class RestNBMessageProcessor implements NBMessageProcessor
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
