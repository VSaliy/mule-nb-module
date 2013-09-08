/**
 *
 */
package org.mule.module.async.processor;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.Pipeline;
import org.mule.api.processor.MessageProcessor;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.context.notification.ServerNotificationManager;

public class AsyncMessageProcessorChainListener
{

    public void firePreInvokeMessageProcessor(MuleEvent event, MessageProcessor messageProcessor)
    {
        ServerNotificationManager notificationManager = event.getMuleContext().getNotificationManager();
        fireNotification(notificationManager, event.getFlowConstruct(), event, messageProcessor, null, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);

    }

    public void firePostInvokeMessageProcessor(MuleEvent event, MessageProcessor messageProcessor)
    {
        ServerNotificationManager notificationManager = event.getMuleContext().getNotificationManager();
        fireNotification(notificationManager, event.getFlowConstruct(), event, messageProcessor, null, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
    }

    public void firePostExceptionInvokeMessageProcessor(MuleEvent event, MessageProcessor messageProcessor, MuleException muleException)
    {
        ServerNotificationManager notificationManager = event.getMuleContext().getNotificationManager();
        MessagingException exception;
        if (muleException instanceof MessagingException)
        {
            exception = (MessagingException) muleException;
        }
        else
        {
            exception = new MessagingException(event, muleException);
        }
        fireNotification(notificationManager, event.getFlowConstruct(), event, messageProcessor, exception, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
    }


    protected void fireNotification(ServerNotificationManager serverNotificationManager, FlowConstruct flowConstruct, MuleEvent event, MessageProcessor processor, MessagingException exceptionThrown, int action)
    {
        if (event.isNotificationsEnabled() && serverNotificationManager != null
            && serverNotificationManager.isNotificationEnabled(MessageProcessorNotification.class))
        {
            if (flowConstruct instanceof Pipeline && ((Pipeline) flowConstruct).getProcessorPath(processor) != null)
            {
                serverNotificationManager.fireNotification(new MessageProcessorNotification(flowConstruct, event, processor, exceptionThrown, action));
            }
        }
    }
}
