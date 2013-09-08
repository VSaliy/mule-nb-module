/**
 *
 */
package org.mule.module.async;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.transport.MuleMessageFactory;

public class DefaultMuleEventFactory implements MuleEventFactory
{

    private MuleMessageFactory muleMessageFactory;
    private FlowConstruct flowConstruct;
    private MessageExchangePattern exchangePattern;

    public DefaultMuleEventFactory(MuleMessageFactory muleMessageFactory, FlowConstruct flowConstruct, MessageExchangePattern exchangePattern)
    {
        this.muleMessageFactory = muleMessageFactory;
        this.flowConstruct = flowConstruct;
        this.exchangePattern = exchangePattern;
    }

    @Override
    public MuleEvent create(Object transportMessage, String encoding) throws Exception
    {
        exchangePattern = MessageExchangePattern.REQUEST_RESPONSE;
        return new DefaultMuleEvent(muleMessageFactory.create(transportMessage, encoding), exchangePattern, flowConstruct);

    }
}
