package org.mule.module.async.internal.config.factories;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.async.pattern.router.AsyncFlowRef;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class AsyncFlowRefFactoryBean
        implements FactoryBean<MessageProcessor>, ApplicationContextAware, MuleContextAware, Initialisable,
        Disposable
{

    private String refName;
    private ApplicationContext applicationContext;
    private MuleContext muleContext;
    private MessageProcessor referencedMessageProcessor;
    private ConcurrentMap<String, MessageProcessor> referenceCache = new ConcurrentHashMap<String, MessageProcessor>();

    public void setName(String name)
    {
        this.refName = name;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (refName.isEmpty())
        {
            throw new InitialisationException(CoreMessages.objectIsNull("flow reference is empty"), this);
        }
        else
        {
            // No need to initialize because message processor will be injected into and managed by parent
            referencedMessageProcessor = createFlowRef();
        }
    }

    @Override
    public void dispose()
    {
        for (MessageProcessor processor : referenceCache.values())
        {
            if (processor instanceof Disposable)
            {
                ((Disposable) processor).dispose();
            }
        }
        referenceCache = null;
    }

    @Override
    public MessageProcessor getObject() throws Exception
    {

        return referencedMessageProcessor;

    }


    protected MessageProcessor createFlowRef()
    {
        if (muleContext.getExpressionManager().isExpression(refName))
        {
            return new AsyncFlowRef(new AsyncFlowRef.MessageProcessorReference()
            {
                @Override
                public MessageProcessor get(MuleEvent event)
                {
                    String name = muleContext.getExpressionManager().parse(refName, event);
                    return (MessageProcessor) applicationContext.getBean(name);
                }
            });
        }
        else
        {
            final MessageProcessor referencedFlow = ((MessageProcessor) applicationContext.getBean(refName));
            if (referencedFlow == null)
            {
                throw new MuleRuntimeException(CoreMessages.objectIsNull(refName));
            }
            if (referencedFlow instanceof FlowConstruct)
            {
                return new AsyncFlowRef(new AsyncFlowRef.MessageProcessorReference()
                {
                    @Override
                    public MessageProcessor get(MuleEvent event)
                    {
                        return referencedFlow;
                    }
                });
            }
            else
            {
                return referencedFlow;
            }
        }
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public Class<?> getObjectType()
    {
        return MessageProcessor.class;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}