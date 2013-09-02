package org.mule.module.nb.processor;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.nb.MessageProcessorCallback;

/**
 *
 */
public interface NBMessageProcessor extends MessageProcessor
{
    void process(MuleEvent event, MessageProcessorCallback callback);
}
