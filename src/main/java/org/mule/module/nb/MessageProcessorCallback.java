package org.mule.module.nb;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;

/**
 *
 */
public interface MessageProcessorCallback
{
    void onSuccess(MuleEvent event);

    void onException(MuleEvent event, MuleException e);
}
