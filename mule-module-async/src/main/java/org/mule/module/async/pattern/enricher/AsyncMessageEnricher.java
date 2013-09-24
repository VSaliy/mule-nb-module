/**
 *
 */
package org.mule.module.async.pattern.enricher;

import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.enricher.MessageEnricher;
import org.mule.module.async.internal.processor.AsynchMessageProcessorContainer;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AsyncMessageEnricher extends AsynchMessageProcessorContainer
{

    private List<MessageEnricher.EnrichExpressionPair> enrichExpressionPairs = new ArrayList<MessageEnricher.EnrichExpressionPair>();

    @Override
    public void process(final MuleEvent originalEvent, final MessageProcessorCallback callback)
    {
        final MuleEvent eventToEnrich = DefaultMuleEvent.copy(originalEvent);
        getProcessorChain().process(eventToEnrich, new MessageProcessorCallback()
        {
            @Override
            public void onSuccess(MuleEvent enricherEventResult)
            {

                final ExpressionManager expressionManager = enricherEventResult.getMuleContext().getExpressionManager();
                if (enricherEventResult != null && !VoidMuleEvent.getInstance().equals(enricherEventResult))
                {
                    for (MessageEnricher.EnrichExpressionPair pair : enrichExpressionPairs)
                    {
                        enrich(originalEvent.getMessage(), enricherEventResult.getMessage(), pair.getSource(), pair.getTarget(),
                               expressionManager);
                    }
                }

                MuleEvent enrichedEvent = originalEvent;
                if (getMuleContext() != null
                    && getMuleContext().getConfiguration().isEnricherPropagatesSessionVariableChanges())
                {
                    enrichedEvent = new DefaultMuleEvent(originalEvent.getMessage(), originalEvent, originalEvent.getSession());
                }

                callback.onSuccess(enrichedEvent);

            }

            @Override
            public void onException(MuleEvent event, MuleException e)
            {
                callback.onException(event, e);
            }
        });

    }

    public List<MessageEnricher.EnrichExpressionPair> getEnrichExpressionPairs()
    {
        return enrichExpressionPairs;
    }

    public void setEnrichExpressionPairs(List<MessageEnricher.EnrichExpressionPair> enrichExpressionPairs)
    {
        this.enrichExpressionPairs = enrichExpressionPairs;
    }

    protected void enrich(MuleMessage currentMessage, MuleMessage enrichmentMessage, String sourceExpressionArg, String targetExpressionArg, ExpressionManager expressionManager)
    {
        if (StringUtils.isEmpty(sourceExpressionArg))
        {
            sourceExpressionArg = "#[payload:]";
        }

        Object enrichmentObject = expressionManager.evaluate(sourceExpressionArg, enrichmentMessage);
        if (enrichmentObject instanceof MuleMessage)
        {
            enrichmentObject = ((MuleMessage) enrichmentObject).getPayload();
        }

        if (!StringUtils.isEmpty(targetExpressionArg))
        {
            expressionManager.enrich(targetExpressionArg, currentMessage, enrichmentObject);
        }
        else
        {
            currentMessage.setPayload(enrichmentObject);
        }
    }


}
