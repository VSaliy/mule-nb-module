package org.mule.module.async.processor;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

/**
 *
 */
public interface AsyncMessageProcessor extends MessageProcessor
{
    void process(MuleEvent event, MessageProcessorCallback callback);
}
