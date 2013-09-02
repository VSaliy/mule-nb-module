/**
 *
 */
package org.mule.module.nb;

import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.nb.processor.NBMessageProcessor;
import org.mule.processor.chain.AbstractMessageProcessorChain;

import java.util.Iterator;
import java.util.List;

public class NBMessageProcessorChain extends AbstractMessageProcessorChain implements NBMessageProcessor

{

    private NBMessageProcessorChainListener chainListener;


    public NBMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
        chainListener = new NBMessageProcessorChainListener();
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        return null;
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
                final MessageProcessorChainCallback chainCallback = new MessageProcessorChainCallback(messageProcessors, next, callback);
                processedEvent = invokeMessageProcessor(processedEvent, next, chainCallback);
            }
            catch (MuleException e)
            {
                chainListener.firePostExceptionInvokeMessageProcessor(processedEvent, next, e);
                callback.onException(processedEvent, e);
            }
            catch (Exception e)
            {
                final MessagingException messagingException = new MessagingException(event, e, next);
                chainListener.firePostExceptionInvokeMessageProcessor(processedEvent, next, messagingException);
                callback.onException(processedEvent, messagingException);
            }
        }
        callback.onSuccess(processedEvent);
    }


    private MuleEvent invokeMessageProcessor(MuleEvent processedEvent, MessageProcessor next, MessageProcessorChainCallback chainCallback) throws MuleException
    {
        chainListener.firePreInvokeMessageProcessor(processedEvent, next);
        if (next instanceof NBMessageProcessor)
        {
            ((NBMessageProcessor) next).process(processedEvent, chainCallback);
        }
        else
        {
            processedEvent = next.process(processedEvent);
            chainListener.firePostInvokeMessageProcessor(processedEvent, next);
        }
        return processedEvent;
    }


    private class MessageProcessorChainCallback implements MessageProcessorCallback
    {

        private final Iterator<MessageProcessor> messageProcessors;
        private MessageProcessor messageProcessor;
        private final MessageProcessorCallback callback;

        public MessageProcessorChainCallback(Iterator<MessageProcessor> messageProcessors, MessageProcessor messageProcessor, MessageProcessorCallback callback)
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
