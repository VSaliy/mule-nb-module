/**
 *
 */
package org.mule.module.nb;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

public class NBMessageProcessorChainListener
{
    public void firePreInvokeMessageProcessor(MuleEvent processedEvent, MessageProcessor next)
    {

    }

    public void firePostInvokeMessageProcessor(MuleEvent processedEvent, MessageProcessor next)
    {

    }

    public void firePostExceptionInvokeMessageProcessor(MuleEvent processedEvent, MessageProcessor next, MuleException muleException)
    {

    }
}
