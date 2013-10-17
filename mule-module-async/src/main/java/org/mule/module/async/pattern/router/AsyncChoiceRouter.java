/**
 *
 */
package org.mule.module.async.pattern.router;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.api.routing.RoutePathNotFoundException;
import org.mule.api.routing.RouterStatisticsRecorder;
import org.mule.api.routing.SelectiveRouter;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.MessageFactory;
import org.mule.management.stats.RouterStatistics;
import org.mule.module.async.internal.processor.AbstractLifecycleDelegateMessageProcessor;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.routing.MessageProcessorFilterPair;
import org.mule.util.NotificationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.ListUtils;

public class AsyncChoiceRouter extends AbstractLifecycleDelegateMessageProcessor implements SelectiveRouter, RouterStatisticsRecorder, MessageProcessorContainer, AsyncMessageProcessor
{

    private final List<MessageProcessorFilterPair> conditionalMessageProcessors = new ArrayList<MessageProcessorFilterPair>();
    private MessageProcessor defaultProcessor;
    private RouterStatistics routerStatistics;

    public AsyncChoiceRouter()
    {
        routerStatistics = new RouterStatistics(RouterStatistics.TYPE_OUTBOUND);
    }


    public void addRoute(MessageProcessor processor, Filter filter)
    {
        MessageProcessorFilterPair addedPair = new MessageProcessorFilterPair(processor, filter);
        conditionalMessageProcessors.add(transitionLifecycleManagedObjectForAddition(addedPair));
    }

    public void removeRoute(MessageProcessor processor)
    {
        updateRoute(processor, new RoutesUpdater()
        {
            public void updateAt(int index)
            {
                MessageProcessorFilterPair removedPair = conditionalMessageProcessors.remove(index);

                transitionLifecycleManagedObjectForRemoval(removedPair);
            }
        });
    }

    public void updateRoute(final MessageProcessor processor, final Filter filter)
    {
        updateRoute(processor, new RoutesUpdater()
        {
            public void updateAt(int index)
            {
                MessageProcessorFilterPair addedPair = new MessageProcessorFilterPair(processor, filter);

                MessageProcessorFilterPair removedPair = conditionalMessageProcessors.set(index,
                                                                                          transitionLifecycleManagedObjectForAddition(addedPair));

                transitionLifecycleManagedObjectForRemoval(removedPair);
            }
        });
    }

    public void setDefaultRoute(MessageProcessor processor)
    {
        defaultProcessor = processor;
    }

    @Override
    public void process(MuleEvent event, MessageProcessorCallback callback)
    {

        MessageProcessor selectedProcessors = selectProcessors(event);
        if (selectedProcessors != null)
        {
            doProcess(selectedProcessors, event, callback);
        }
        else if (defaultProcessor != null)
        {
            doProcess(defaultProcessor, event, callback);
        }
        else
        {
            if (getRouterStatistics() != null && getRouterStatistics().isEnabled())
            {
                getRouterStatistics().incrementNoRoutedMessage();
            }
            callback.onException(event, new RoutePathNotFoundException(
                    MessageFactory.createStaticMessage("Can't process message because no route has been found matching any filter and no default route is defined"),
                    event, this));
        }

    }


    protected MessageProcessor selectProcessors(MuleEvent event)
    {

        for (MessageProcessorFilterPair mpfp : getConditionalMessageProcessors())
        {
            if (mpfp.getFilter().accept(event.getMessage()))
            {
                return mpfp.getMessageProcessor();
            }
        }

        return null;
    }

    @Override
    protected Collection<?> getLifecycleManagedObjects()
    {
        if (defaultProcessor == null)
        {
            return conditionalMessageProcessors;
        }

        return ListUtils.union(conditionalMessageProcessors, Collections.singletonList(defaultProcessor));
    }


    public List<MessageProcessorFilterPair> getConditionalMessageProcessors()
    {
        return Collections.unmodifiableList(conditionalMessageProcessors);
    }


    private interface RoutesUpdater
    {

        void updateAt(int index);
    }

    private void updateRoute(MessageProcessor processor, RoutesUpdater routesUpdater)
    {
        synchronized (conditionalMessageProcessors)
        {
            for (int i = 0; i < conditionalMessageProcessors.size(); i++)
            {
                if (conditionalMessageProcessors.get(i).getMessageProcessor().equals(processor))
                {
                    routesUpdater.updateAt(i);
                }
            }
        }
    }

    public RouterStatistics getRouterStatistics()
    {
        return routerStatistics;
    }

    public void setRouterStatistics(RouterStatistics routerStatistics)
    {
        this.routerStatistics = routerStatistics;
    }


    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        List<MessageProcessor> messageProcessors = new ArrayList<MessageProcessor>();
        for (MessageProcessorFilterPair cmp : conditionalMessageProcessors)
        {
            messageProcessors.add(cmp.getMessageProcessor());
        }
        messageProcessors.add(defaultProcessor);
        NotificationUtils.addMessageProcessorPathElements(messageProcessors, pathElement);
    }

    @Override
    public String toString()
    {
        return String.format("%s [flow-construct=%s, started=%s]", getClass().getSimpleName(),
                             getFlowConstruct() != null ? getFlowConstruct().getName() : null, isStarted());
    }
}
