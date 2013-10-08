/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractLifecycleDelegateMessageProcessor extends AbstractAsyncMessageProcessor implements Lifecycle, FlowConstructAware, MuleContextAware
{

    private final AtomicBoolean initialised = new AtomicBoolean(false);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean starting = new AtomicBoolean(false);


    public void initialise() throws InitialisationException
    {

        if (!getInitialised().get())
        {
            for (Object o : getLifecycleManagedObjects())
            {
                if (o instanceof FlowConstructAware)
                {
                    ((FlowConstructAware) o).setFlowConstruct(getFlowConstruct());
                }
                if (o instanceof MuleContextAware)
                {
                    ((MuleContextAware) o).setMuleContext(getMuleContext());
                }
                if (o instanceof Initialisable)
                {
                    ((Initialisable) o).initialise();
                }
            }
            getInitialised().set(true);
        }
    }

    public void start() throws MuleException
    {
        if (!getStarted().get())
        {
            getStarting().set(true);
            for (Object o : getLifecycleManagedObjects())
            {
                if (o instanceof Startable)
                {
                    ((Startable) o).start();
                }
            }

            getStarted().set(true);
            getStarting().set(false);
        }

    }

    public void stop() throws MuleException
    {

        for (Object o : getLifecycleManagedObjects())
        {
            if (o instanceof Stoppable)
            {
                ((Stoppable) o).stop();
            }
        }

        getStarted().set(false);

    }

    public void dispose()
    {

        for (Object o : getLifecycleManagedObjects())
        {
            if (o instanceof Disposable)
            {
                ((Disposable) o).dispose();
            }
        }

    }

    protected abstract Collection<?> getLifecycleManagedObjects();

    public AtomicBoolean getInitialised()
    {
        return initialised;
    }

    public AtomicBoolean getStarted()
    {
        return started;
    }

    public AtomicBoolean getStarting()
    {
        return starting;
    }

}
