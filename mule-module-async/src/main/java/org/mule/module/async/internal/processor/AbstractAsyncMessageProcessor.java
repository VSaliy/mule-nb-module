/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;

public abstract class AbstractAsyncMessageProcessor implements AsyncMessageProcessor, MuleContextAware
{

    private MuleContext muleContext;

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        final MuleEventFuture future = new MuleEventFuture();
        process(event, new FutureMessageProcessorCallback(future));
        return future.get();
    }

    public static void doProcess(MessageProcessor messageProcessor, MuleEvent event, MessageProcessorCallback callback)
    {
        if (messageProcessor instanceof AsyncMessageProcessor)
        {
            ((AsyncMessageProcessor) messageProcessor).process(event, callback);
        }
        else
        {
            try
            {
                MuleEvent process = messageProcessor.process(event);
                callback.onSuccess(process);
            }
            catch (MuleException e)
            {
                callback.onException(event, e);
            }
            catch (Exception e)
            {
                callback.onException(event, new MessagingException(event, e));
            }
        }
    }

}
