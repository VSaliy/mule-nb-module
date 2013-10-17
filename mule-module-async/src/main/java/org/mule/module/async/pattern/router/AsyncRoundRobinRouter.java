/**
 *
 */
package org.mule.module.async.pattern.router;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.async.internal.processor.AbstractLifecycleDelegateMessageProcessor;
import org.mule.module.async.internal.processor.AsyncMessageProcessorHelper;
import org.mule.module.async.processor.MessageProcessorCallback;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncRoundRobinRouter extends AbstractLifecycleDelegateMessageProcessor
{

    private List<MessageProcessor> routes;

    private AtomicInteger index = new AtomicInteger(-1);

    @Override
    protected Collection<?> getLifecycleManagedObjects()
    {
        return routes;
    }

    @Override
    public void process(MuleEvent event, MessageProcessorCallback callback)
    {
        if (!routes.isEmpty())
        {
            MessageProcessor messageProcessor = routes.get(getNextRoute());
            AsyncMessageProcessorHelper.doProcess(messageProcessor, event, callback);
        }
        else
        {
            callback.onSuccess(event);
        }

    }


    private int getNextRoute()
    {

        while (true)
        {
            int lastIndex = index.get();
            int nextIndex = (lastIndex + 1) % routes.size();
            if (index.compareAndSet(lastIndex, nextIndex))
            {
                return nextIndex;
            }
        }
    }

    public void setRoutes(List<MessageProcessor> routes)
    {
        this.routes = routes;
    }
}
