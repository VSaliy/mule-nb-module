package org.mule.module.async.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 * Callback used in Asynchronous Message Processors
 */
public interface MessageProcessorCallback
{

    void onSuccess(MuleEvent event);

    void onException(MuleEvent event, MuleException e);
}
