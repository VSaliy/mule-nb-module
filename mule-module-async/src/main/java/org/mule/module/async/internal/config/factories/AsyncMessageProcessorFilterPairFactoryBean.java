/**
 *
 */
package org.mule.module.async.internal.config.factories;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.routing.filter.Filter;
import org.mule.module.async.internal.processor.AsyncMessageProcessorChainBuilder;
import org.mule.processor.chain.AbstractMessageProcessorChainBuilder;
import org.mule.routing.MessageProcessorFilterPair;
import org.mule.routing.filters.AcceptAllFilter;
import org.mule.routing.filters.ExpressionFilter;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

public class AsyncMessageProcessorFilterPairFactoryBean
        implements FactoryBean<MessageProcessorFilterPair>,
        MuleContextAware
{

    private List<MessageProcessor> messageProcessors;
    private Filter filter = new ExpressionFilter();

    public void setFilter(Filter filter)
    {
        this.filter = filter;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public void setExpression(String expression)
    {
        ((ExpressionFilter) filter).setExpression(expression);
    }

    public void setEvaluator(String evaluator)
    {
        ((ExpressionFilter) filter).setEvaluator(evaluator);
    }

    public void setCustomEvaluator(String customEvaluator)
    {
        ((ExpressionFilter) filter).setCustomEvaluator(customEvaluator);
    }

    @Override
    public MessageProcessorFilterPair getObject() throws Exception
    {
        AbstractMessageProcessorChainBuilder builder = new AsyncMessageProcessorChainBuilder();

        for (MessageProcessor processor : messageProcessors)
        {
            builder.chain((MessageProcessor) processor);
        }

        return createFilterPair(builder);
    }

    private MessageProcessorFilterPair createFilterPair(MessageProcessorChainBuilder builder) throws Exception
    {
        if (filter == null)
        {
            return new MessageProcessorFilterPair(builder.build(), AcceptAllFilter.INSTANCE);
        }
        else
        {
            return new MessageProcessorFilterPair(builder.build(), filter);
        }
    }

    @Override
    public Class<MessageProcessorFilterPair> getObjectType()
    {
        return MessageProcessorFilterPair.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        if (filter != null && filter instanceof MuleContextAware)
        {
            ((MuleContextAware) filter).setMuleContext(context);
        }
    }
}
