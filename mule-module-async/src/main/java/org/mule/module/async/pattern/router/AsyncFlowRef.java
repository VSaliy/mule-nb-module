/**
 *
 */
package org.mule.module.async.pattern.router;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.async.processor.AbstractAsyncMessageProcessor;
import org.mule.module.async.pattern.AsyncMessageProcessorHelper;
import org.mule.module.async.processor.MessageProcessorCallback;

public class AsyncFlowRef extends AbstractAsyncMessageProcessor
{

    private MessageProcessorReference messageProcessor;

    public AsyncFlowRef(MessageProcessorReference messageProcessor)
    {
        this.messageProcessor = messageProcessor;
    }

    @Override
    public void process(MuleEvent event, MessageProcessorCallback callback)
    {
        AsyncMessageProcessorHelper.doProcess(messageProcessor.get(event), event, callback);
    }

    public static interface MessageProcessorReference
    {

        MessageProcessor get(MuleEvent event);
    }


}
