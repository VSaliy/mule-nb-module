/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.module.async.pattern.router.AsyncMessageProcessorChain;
import org.mule.processor.chain.AbstractMessageProcessorChainBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AsyncMessageProcessorChainBuilder extends AbstractMessageProcessorChainBuilder
{


    public AsyncMessageProcessorChainBuilder()
    {
    }

    public AsyncMessageProcessorChainBuilder(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    /**
     * This builder supports the chaining together of message processors that intercept and also those that
     * don't. While one can iterate over message processor intercepting message processors need to be chained
     * together. One solution is make all message processors intercepting (via adaption) and chain them all
     * together, this results in huge stack traces and recursive calls with adaptor. The alternative is to
     * build the chain in such a way that we iterate when we can and chain where we need to. <br>
     * We iterate over the list of message processor to be chained together in reverse order collecting up
     * those that can be iterated over in a temporary list, as soon as we have an intercepting message
     * processor we create a DefaultMessageProcessorChain using the temporary list and set it as a listener of
     * the intercepting message processor and then we continue with the algorithm
     */
    public AsyncMessageProcessorChain build() throws MuleException
    {
        LinkedList<MessageProcessor> tempList = new LinkedList<MessageProcessor>();

        // Start from last but one message processor and work backwards
        for (int i = processors.size() - 1; i >= 0; i--)
        {
            MessageProcessor processor = initializeMessageProcessor(processors.get(i));
            if (processor instanceof InterceptingMessageProcessor)
            {
                InterceptingMessageProcessor interceptingProcessor = (InterceptingMessageProcessor) processor;
                // Processor is intercepting so we can't simply iterate
                if (i + 1 < processors.size())
                {
                    // The current processor is not the last in the list
                    if (tempList.isEmpty())
                    {
                        interceptingProcessor.setListener(initializeMessageProcessor(processors.get(i + 1)));
                    }
                    else if (tempList.size() == 1)
                    {
                        interceptingProcessor.setListener(tempList.get(0));
                    }
                    else
                    {
                        interceptingProcessor.setListener(createMessageProcessorChain(tempList));
                    }
                }
                tempList = new LinkedList<MessageProcessor>(Collections.singletonList(processor));
            }
            else
            {
                // Processor is not intercepting so we can invoke it using iteration
                // (add to temp list)
                tempList.addFirst(initializeMessageProcessor(processor));
            }
        }
        // Create the final chain using the current tempList after reserve iteration is complete. This temp
        // list contains the first n processors in the chain that are not intercepting.. with processor n+1
        // having been injected as the listener of processor n
        return createMessageProcessorChain(tempList);


    }


    protected AsyncMessageProcessorChain createMessageProcessorChain(LinkedList<MessageProcessor> tempList)
    {
        return new AsyncMessageProcessorChain("Non Blocking Chain of " + name,
                                           new ArrayList<MessageProcessor>(tempList));
    }

    public MessageProcessorChainBuilder chain(MessageProcessor... processors)
    {
        for (MessageProcessor messageProcessor : processors)
        {
            this.processors.add(messageProcessor);
        }
        return this;
    }

    public MessageProcessorChainBuilder chain(List<MessageProcessor> processors)
    {
        if (processors != null)
        {
            this.processors.addAll(processors);
        }
        return this;
    }

    public MessageProcessorChainBuilder chain(MessageProcessorBuilder... builders)
    {
        for (MessageProcessorBuilder messageProcessorBuilder : builders)
        {
            this.processors.add(messageProcessorBuilder);
        }
        return this;
    }


}
