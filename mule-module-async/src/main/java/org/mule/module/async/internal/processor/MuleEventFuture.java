/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;

public class MuleEventFuture
{

    private MuleEvent event;
    private MuleException exception;
    private final Object monitor = new Object();

    public MuleEventFuture()
    {
    }

    public MuleEvent get() throws MuleException
    {
        synchronized (monitor)
        {
            if (event == null && exception == null)
            {
                try
                {
                    monitor.wait();
                }
                catch (InterruptedException e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
        }
        if (exception != null)
        {
            throw exception;
        }
        return event;
    }

    public void set(MuleEvent event)
    {
        synchronized (monitor)
        {
            this.event = event;
            monitor.notifyAll();
        }
    }

    public void set(MuleException exception)
    {

        synchronized (monitor)
        {
            this.exception = exception;
            monitor.notifyAll();
        }
    }


}
