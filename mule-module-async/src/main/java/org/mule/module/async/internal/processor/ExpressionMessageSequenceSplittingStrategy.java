/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.MessageSequence;
import org.mule.routing.outbound.ArrayMessageSequence;
import org.mule.routing.outbound.CollectionMessageSequence;
import org.mule.routing.outbound.IteratorMessageSequence;
import org.mule.util.collection.SplittingStrategy;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class ExpressionMessageSequenceSplittingStrategy implements SplittingStrategy<MuleEvent, MessageSequence<?>>
{

    private String expression;

    public ExpressionMessageSequenceSplittingStrategy(String expression)
    {
        this.expression = expression;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public MessageSequence<?> split(MuleEvent event)
    {
        if (expression == null)
        {
            final MuleMessage msg = event.getMessage();
            if (msg instanceof MuleMessageCollection)
            {
                return new ArrayMessageSequence(((MuleMessageCollection) msg).getMessagesAsArray());
            }
            Object payload = msg.getPayload();
            return getMessageSequence(payload);
        }
        else
        {
            Object value = event.getMuleContext().getExpressionManager().evaluate(expression, event);
            return getMessageSequence(value);
        }
    }

    private MessageSequence<?> getMessageSequence(Object payload)
    {
        if (payload instanceof MessageSequence<?>)
        {
            return ((MessageSequence<?>) payload);
        }
        if (payload instanceof Iterator<?>)
        {
            return new IteratorMessageSequence<Object>(((Iterator<Object>) payload));
        }
        if (payload instanceof Collection)
        {
            return new CollectionMessageSequence(new LinkedList((Collection) payload));
        }
        if (payload instanceof Iterable<?>)
        {
            return new IteratorMessageSequence<Object>(((Iterable<Object>) payload).iterator());
        }
        if (payload instanceof Object[])
        {
            return new ArrayMessageSequence((Object[]) payload);
        }
        else
        {
            throw new IllegalArgumentException(CoreMessages.objectNotOfCorrectType(payload.getClass(),
                                                                                   new Class[] {Iterable.class, Iterator.class, MessageSequence.class, Collection.class})
                                                       .getMessage());
        }
    }

}
