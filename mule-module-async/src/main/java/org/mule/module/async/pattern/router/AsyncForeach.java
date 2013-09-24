/**
 *
 */
package org.mule.module.async.pattern.router;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.async.internal.processor.AsynchMessageProcessorContainer;
import org.mule.module.async.internal.processor.ExpressionMessageSequenceSplittingStrategy;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.routing.MessageSequence;
import org.mule.util.collection.SplittingStrategy;

public class AsyncForeach extends AsynchMessageProcessorContainer
{

    private SplittingStrategy<MuleEvent, MessageSequence<?>> strategy;
    private String collectionExpression;

    @Override
    public void process(MuleEvent event, MessageProcessorCallback callback)
    {

        final MessageSequence<?> messageSequence = getStrategy().split(event);
        try
        {
            processNext(messageSequence, event, new ForeachMessageProcessorCallback(event, messageSequence, callback));
        }
        catch (Exception e)
        {
            callback.onException(event, new MessagingException(event, e, this));
        }
    }

    private void processNext(MessageSequence<?> messageSequence, MuleEvent originalEvent, ForeachMessageProcessorCallback callback)
    {
        if (messageSequence.hasNext())
        {
            final MuleMessage message = createMessage(messageSequence.next(), originalEvent.getMessage());
            final DefaultMuleEvent muleEvent = new DefaultMuleEvent(message, originalEvent, originalEvent.getSession());
            getProcessorChain().process(muleEvent, callback);
        }
        else
        {
            callback.getParent().onSuccess(originalEvent);
        }

    }

    private MuleMessage createMessage(Object payload, MuleMessage originalMessage)
    {
        if (payload instanceof MuleMessage)
        {
            return (MuleMessage) payload;
        }
        MuleMessage message = new DefaultMuleMessage(originalMessage, getMuleContext());
        message.setPayload(payload);
        return message;
    }

    public SplittingStrategy<MuleEvent, MessageSequence<?>> getStrategy()
    {
        return strategy;
    }

    public void setStrategy(SplittingStrategy<MuleEvent, MessageSequence<?>> strategy)
    {
        this.strategy = strategy;
    }

    public String getCollectionExpression()
    {
        return collectionExpression;
    }

    public void setCollectionExpression(String collectionExpression)
    {
        this.collectionExpression = collectionExpression;
    }

    @Override
    public void initialise() throws InitialisationException
    {

        if (getStrategy() == null)
        {
            setStrategy(new ExpressionMessageSequenceSplittingStrategy(getCollectionExpression()));
        }
        super.initialise();
    }


    private class ForeachMessageProcessorCallback implements MessageProcessorCallback
    {

        private MessageProcessorCallback parent;
        private MuleEvent originalEvent;
        private MessageSequence<?> messageSequence;

        private ForeachMessageProcessorCallback(MuleEvent originalEvent, MessageSequence<?> messageSequence, MessageProcessorCallback parent)
        {
            this.originalEvent = originalEvent;
            this.messageSequence = messageSequence;
            this.parent = parent;
        }

        @Override
        public void onSuccess(MuleEvent event)
        {
            try
            {
                processNext(messageSequence, originalEvent, this);
            }
            catch (Exception e)
            {
                onException(originalEvent, new MessagingException(event, e, AsyncForeach.this));
            }

        }

        @Override
        public void onException(MuleEvent event, MuleException e)
        {
            getParent().onException(event, e);
        }

        private MessageProcessorCallback getParent()
        {
            return parent;
        }
    }
}
