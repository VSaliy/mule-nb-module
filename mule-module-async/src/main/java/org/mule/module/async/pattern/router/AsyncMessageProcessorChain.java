/**
 *
 */
package org.mule.module.async.pattern.router;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.internal.processor.AsyncMessageProcessorChainListener;
import org.mule.module.async.internal.processor.FutureMessageProcessorCallback;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.module.async.internal.processor.MuleEventFuture;
import org.mule.processor.chain.AbstractMessageProcessorChain;

import java.util.Iterator;
import java.util.List;

public class AsyncMessageProcessorChain extends AbstractMessageProcessorChain implements AsyncMessageProcessor

{

    private AsyncMessageProcessorChainListener chainListener;


    public AsyncMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
        chainListener = new AsyncMessageProcessorChainListener();
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        final MuleEventFuture future = new MuleEventFuture();
        process(event, new FutureMessageProcessorCallback(future));
        return future.get();
    }


    @Override
    public void process(final MuleEvent event, final MessageProcessorCallback callback)
    {
        final Iterator<MessageProcessor> messageProcessors = getMessageProcessors().iterator();
        processNext(messageProcessors, event, callback);
    }

    private void processNext(final Iterator<MessageProcessor> messageProcessors, final MuleEvent event, final MessageProcessorCallback callback)
    {


        MuleEvent processedEvent = event;
        while (messageProcessors.hasNext())
        {
            if (VoidMuleEvent.getInstance().equals(event))
            {   //If Void Event stop Processing
                break;
            }
            MessageProcessor next = messageProcessors.next();
            try
            {
                final NBMessageProcessorChainCallback chainCallback = new NBMessageProcessorChainCallback(messageProcessors, next, callback);
                chainListener.firePreInvokeMessageProcessor(processedEvent, next);
                if (next instanceof AsyncMessageProcessor)
                {
                    ((AsyncMessageProcessor) next).process(processedEvent, chainCallback);
                    //Return since is asynch.
                    return;
                }
                else
                {
                    processedEvent = next.process(processedEvent);
                    chainListener.firePostInvokeMessageProcessor(processedEvent, next);
                }
            }
            catch (MuleException e)
            {
                chainListener.firePostExceptionInvokeMessageProcessor(processedEvent, next, e);
                callback.onException(processedEvent, e);
                return;
            }
            catch (Exception e)
            {
                final MessagingException messagingException = new MessagingException(event, e, next);
                chainListener.firePostExceptionInvokeMessageProcessor(processedEvent, next, messagingException);
                callback.onException(processedEvent, messagingException);
                return;
            }
        }
        callback.onSuccess(processedEvent);
    }


    private class NBMessageProcessorChainCallback implements MessageProcessorCallback
    {

        private final Iterator<MessageProcessor> messageProcessors;
        private MessageProcessor messageProcessor;
        private final MessageProcessorCallback callback;

        public NBMessageProcessorChainCallback(Iterator<MessageProcessor> messageProcessors, MessageProcessor messageProcessor, MessageProcessorCallback callback)
        {
            this.messageProcessors = messageProcessors;
            this.messageProcessor = messageProcessor;
            this.callback = callback;
        }

        @Override
        public void onSuccess(MuleEvent event)
        {
            chainListener.firePostInvokeMessageProcessor(event, messageProcessor);
            if (messageProcessors.hasNext())
            {
                processNext(messageProcessors, event, callback);
            }
            else
            {   //Chain was finished
                callback.onSuccess(event);
            }
        }

        @Override
        public void onException(MuleEvent event, MuleException e)
        {
            chainListener.firePostExceptionInvokeMessageProcessor(event, messageProcessor, e);
            //Propagate to parent
            callback.onException(event, e);
        }
    }
}
