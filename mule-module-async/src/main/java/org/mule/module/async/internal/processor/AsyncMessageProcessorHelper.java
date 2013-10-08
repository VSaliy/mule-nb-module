/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;

public class AsyncMessageProcessorHelper
{

    public static void doProcess(MessageProcessor messageProcessor,MuleEvent event, MessageProcessorCallback callback)
    {
        if (messageProcessor instanceof AsyncMessageProcessor)
        {
            ((AsyncMessageProcessor) messageProcessor).process(event, callback);
        }
        else
        {
            try
            {
                MuleEvent result = messageProcessor.process(event);
                callback.onSuccess(result);
            }
            catch (MuleException e)
            {
                callback.onException(event, e);
            }
        }
    }
}
