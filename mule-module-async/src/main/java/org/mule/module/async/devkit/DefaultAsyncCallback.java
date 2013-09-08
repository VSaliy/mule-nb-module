/**
 *
 */
package org.mule.module.async.devkit;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.transport.PropertyScope;
import org.mule.module.async.processor.MessageProcessorCallback;

import java.util.Map;

public class DefaultAsyncCallback implements AsyncCallback
{

    private MuleEvent event;
    private MessageProcessorCallback messageProcessorCallback;

    public DefaultAsyncCallback(MuleEvent event, MessageProcessorCallback messageProcessorCallback)
    {
        this.event = event;
        this.messageProcessorCallback = messageProcessorCallback;
    }

    @Override
    public void onSuccess(Object payload, Map<String, Object> flowVars)
    {

        this.event.getMessage().setPayload(payload);
        if (flowVars != null)
        {
            this.event.getMessage().addProperties(flowVars, PropertyScope.INVOCATION);
        }

        this.messageProcessorCallback.onSuccess(this.event);


    }

    @Override
    public void onSuccess(Object payload)
    {
        onSuccess(payload, null);
    }

    @Override
    public void onException(MuleException e)
    {
        this.messageProcessorCallback.onException(this.event, e);
    }
}
