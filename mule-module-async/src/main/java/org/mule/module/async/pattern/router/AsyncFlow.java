package org.mule.module.async.pattern.router;

import org.mule.DefaultMuleEvent;
import org.mule.api.GlobalNameableObject;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.construct.Pipeline;
import org.mule.api.processor.DefaultMessageProcessorPathElement;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.processor.MessageProcessorPathElement;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.api.source.MessageSource;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.module.async.internal.processor.AsyncMessageProcessorChainBuilder;
import org.mule.module.async.internal.processor.FutureMessageProcessorCallback;
import org.mule.module.async.internal.processor.MuleEventFuture;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.util.NotificationUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AsyncFlow extends AbstractFlowConstruct implements Pipeline, AsyncMessageProcessor
{


    private MessageSource messageSource;
    private AsyncMessageProcessor asyncChain;
    private List<MessageProcessor> messageProcessors = Collections.emptyList();
    private Map<MessageProcessor, String> flowMap = new LinkedHashMap<MessageProcessor, String>();
    private ProcessingStrategy processingStrategy;
    private MessageProcessorChainBuilder chainBuilder;


    public AsyncFlow(String name, MuleContext muleContext)
    {
        super(name, muleContext);
    }

    @Override
    public String getConstructType()
    {
        return "Flow";
    }


    @Override
    public void setMessageSource(MessageSource messageSource)
    {
        this.messageSource = messageSource;
    }

    @Override
    protected void doStart() throws MuleException
    {
        //TODO HACK TWO use this to avoid exception when changing event from thread to thread :(
        ThreadSafeAccess.AccessControl.setAssertMessageAccess(false);
        super.doStart();
        startIfStartable(asyncChain);
        startIfStartable(messageSource);
        createFlowMap();
    }

    @Override
    protected void doInitialise() throws MuleException
    {
        super.doInitialise();

        if (getChainBuilder() == null)
        {
            setChainBuilder(new AsyncMessageProcessorChainBuilder(this));
        }
        if (getProcessingStrategy() == null)
        {
            setProcessingStrategy(new SynchronousProcessingStrategy());
        }

        asyncChain = buildChain();

        if (messageSource != null)
        {
            messageSource.setListener(this);
        }


        injectFlowConstructMuleContext(messageSource);
        injectFlowConstructMuleContext(asyncChain);
        initialiseIfInitialisable(messageSource);
        initialiseIfInitialisable(asyncChain);
    }

    public AsyncMessageProcessor getAsyncChain()
    {
        return asyncChain;
    }

    private void createFlowMap()
    {
        DefaultMessageProcessorPathElement pipeLinePathElement = new DefaultMessageProcessorPathElement(null, getName());
        addMessageProcessorPathElements(pipeLinePathElement);
        flowMap = NotificationUtils.buildPaths(pipeLinePathElement);

    }

    private AsyncMessageProcessor buildChain() throws MuleException
    {
        MessageProcessorChainBuilder messageProcessorChainBuilder = getChainBuilder();
        getProcessingStrategy().configureProcessors(getMessageProcessors(),
                                                    new NBStageNameSource(getName()), messageProcessorChainBuilder, muleContext);
        return (AsyncMessageProcessor) messageProcessorChainBuilder.build();
    }


    public MessageProcessorChainBuilder getChainBuilder()
    {
        return chainBuilder;
    }

    public void setChainBuilder(MessageProcessorChainBuilder chainBuilder)
    {
        this.chainBuilder = chainBuilder;
    }

    @Override
    public MessageSource getMessageSource()
    {
        return messageSource;
    }

    @Override
    public void setMessageProcessors(List<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        return messageProcessors;
    }

    @Override
    public void setProcessingStrategy(ProcessingStrategy processingStrategy)
    {
        this.processingStrategy = processingStrategy;
    }

    @Override
    public ProcessingStrategy getProcessingStrategy()
    {
        return processingStrategy;
    }

    @Override
    public String getProcessorPath(MessageProcessor processor)
    {
        return flowMap.get(processor);
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement)
    {
        String prefix = "processors";
        MessageProcessorPathElement processorPathElement = pathElement.addChild(prefix);

        //Only MP till first InterceptingMessageProcessor should be used to generate the Path,
        // since the next ones will be generated by the InterceptingMessageProcessor because they are added as an inned chain
        List<MessageProcessor> filteredMessageProcessorList = new ArrayList<MessageProcessor>();
        for (MessageProcessor messageProcessor : getMessageProcessors())
        {
            if (messageProcessor instanceof InterceptingMessageProcessor)
            {
                filteredMessageProcessorList.add(messageProcessor);
                break;
            }
            else
            {
                filteredMessageProcessorList.add(messageProcessor);
            }
        }

        NotificationUtils.addMessageProcessorPathElements(filteredMessageProcessorList, processorPathElement);

        if (exceptionListener instanceof MessageProcessorContainer)
        {
            MessageProcessorPathElement exceptionStrategyPathElement = pathElement.addChild(getExceptionStrategyPrefix());
            ((MessageProcessorContainer) exceptionListener).addMessageProcessorPathElements(exceptionStrategyPathElement);

        }

    }

    private String getExceptionStrategyPrefix()
    {
        String esPrefix = "es";
        String globalName = null;
        if (exceptionListener instanceof GlobalNameableObject)
        {
            globalName = ((GlobalNameableObject) exceptionListener).getGlobalName();
        }
        if (globalName != null)
        {
            esPrefix = globalName + "/es";
        }
        return esPrefix;
    }

    @Override
    public void process(MuleEvent event, MessageProcessorCallback callback)
    {
        final MuleEvent newEvent = new DefaultMuleEvent(event, this);
        asyncChain.process(newEvent, new FlowMessageProcessorCallback(callback));
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        final MuleEventFuture future = new MuleEventFuture();
        process(event, new FutureMessageProcessorCallback(future));
        return future.get();
    }

    @Override
    public boolean isSynchronous()
    {
        return this.processingStrategy.getClass().equals(SynchronousProcessingStrategy.class);
    }

    private class NBStageNameSource implements ProcessingStrategy.StageNameSource
    {

        private String name;

        public NBStageNameSource(String name)
        {
            this.name = name;
        }

        @Override
        public String getName()
        {
            return this.name;
        }
    }

    private class FlowMessageProcessorCallback implements MessageProcessorCallback
    {

        private MessageProcessorCallback parent;

        private FlowMessageProcessorCallback(MessageProcessorCallback parent)
        {
            this.parent = parent;
        }

        @Override
        public void onSuccess(MuleEvent event)
        {

            parent.onSuccess(event);
        }

        @Override
        public void onException(MuleEvent event, MuleException e)
        {
            try
            {
                MuleEvent muleEvent = getExceptionListener().handleException(e, event);
                if (muleEvent.getMessage().getExceptionPayload() != null)
                {
                    parent.onException(event, new MessagingException(event, e));
                }
                else
                {
                    parent.onSuccess(muleEvent);
                }
            }
            catch (Exception exception)
            {
                parent.onException(event, new MessagingException(event, e));
            }
        }
    }
}
