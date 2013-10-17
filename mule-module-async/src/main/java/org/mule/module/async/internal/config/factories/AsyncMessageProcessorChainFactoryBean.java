/**
 *
 */
package org.mule.module.async.internal.config.factories;

import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.module.async.internal.processor.AsyncMessageProcessorChainBuilder;

public class AsyncMessageProcessorChainFactoryBean extends MessageProcessorChainFactoryBean
{

    @Override
    protected MessageProcessorChainBuilder getBuilderInstance()
    {
        AsyncMessageProcessorChainBuilder asyncMessageProcessorChainBuilder = new AsyncMessageProcessorChainBuilder();
        asyncMessageProcessorChainBuilder.setName("processor chain '"+name+"'");
        return asyncMessageProcessorChainBuilder;
    }
}
