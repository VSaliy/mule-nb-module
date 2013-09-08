/**
 *
 */
package org.mule.module.async.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;

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

}
