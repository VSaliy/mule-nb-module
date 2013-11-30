/**
 *
 */
package org.mule.module.async.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.module.async.internal.processor.FutureMessageProcessorCallback;
import org.mule.module.async.internal.processor.MuleEventFuture;

/**
 * Base class for AsyncMessage Processor. Adapts Synchronous Message Processors with Async Ones using A Future
 */
public abstract class AbstractAsyncMessageProcessor implements AsyncMessageProcessor, MuleContextAware, FlowConstructAware
{

    private MuleContext muleContext;
    private FlowConstruct flowConstruct;

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
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    @Override
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        final MuleEventFuture future = new MuleEventFuture();
        process(event, new FutureMessageProcessorCallback(future));
        return future.get();
    }


}
