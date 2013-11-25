/**
 *
 */
package org.mule.module.async.internal;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.session.DefaultMuleSession;

import java.net.URI;

public class DefaultMuleEventFactory implements MuleEventFactory
{

    private MuleMessageFactory muleMessageFactory;
    private URI uri;
    private FlowConstruct flowConstruct;
    private MessageExchangePattern exchangePattern;

    public DefaultMuleEventFactory(MuleMessageFactory muleMessageFactory, URI uri, FlowConstruct flowConstruct, MessageExchangePattern exchangePattern)
    {
        this.muleMessageFactory = muleMessageFactory;
        this.uri = uri;
        this.flowConstruct = flowConstruct;
        this.exchangePattern = exchangePattern;
    }

    @Override
    public MuleEvent create(Object transportMessage, String encoding) throws Exception
    {
        exchangePattern = MessageExchangePattern.REQUEST_RESPONSE;
        return new DefaultMuleEvent(muleMessageFactory.create(transportMessage, encoding), uri, exchangePattern, flowConstruct, new DefaultMuleSession());

    }
}
