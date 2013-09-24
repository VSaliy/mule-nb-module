/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.async.pattern.router.AsyncMessageProcessorChain;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class AsynchMessageProcessorContainer extends AbstractLifecycleDelegateMessageProcessor
{

    private List<MessageProcessor> messageProcessors;
    private AsyncMessageProcessorChain processorChain;

    public List<MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
    }

    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        AsyncMessageProcessorChainBuilder chainBuilder = createAsyncMessageProcessorChainBuilder();
        chainBuilder.chain(getMessageProcessors());
        try
        {
            setProcessorChain(chainBuilder.build());
        }
        catch (MuleException e)
        {
            e.printStackTrace();
        }
        super.initialise();
    }

    private AsyncMessageProcessorChainBuilder createAsyncMessageProcessorChainBuilder()
    {

        return new AsyncMessageProcessorChainBuilder();
    }

    @Override
    protected Collection<?> getLifecycleManagedObjects()
    {
        return Arrays.asList(getProcessorChain());
    }

    public AsyncMessageProcessorChain getProcessorChain()
    {
        return processorChain;
    }

    public void setProcessorChain(AsyncMessageProcessorChain processorChain)
    {
        this.processorChain = processorChain;
    }
}
