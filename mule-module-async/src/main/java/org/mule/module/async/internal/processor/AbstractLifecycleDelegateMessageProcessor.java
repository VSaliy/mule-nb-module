/**
 *
 */
package org.mule.module.async.internal.processor;

import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.async.processor.AbstractAsyncMessageProcessor;

import java.util.Collection;

public abstract class AbstractLifecycleDelegateMessageProcessor extends AbstractAsyncMessageProcessor implements Lifecycle, FlowConstructAware, MuleContextAware
{

    private volatile boolean initialised = false;
    private volatile boolean started = false;


    public void initialise() throws InitialisationException
    {

        if (!initialised)
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
            initialised = true;

        }
    }

    public void start() throws MuleException
    {
        if (!started)
        {

            for (Object o : getLifecycleManagedObjects())
            {
                if (o instanceof Startable)
                {
                    ((Startable) o).start();
                }
            }
            started = true;

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

        started = false;

    }

    protected <O> O transitionLifecycleManagedObjectForAddition(O managedObject)
    {
        try
        {
            if ((getFlowConstruct() != null) && (managedObject instanceof FlowConstructAware))
            {
                ((FlowConstructAware) managedObject).setFlowConstruct(getFlowConstruct());
            }

            if ((getMuleContext() != null) && (managedObject instanceof MuleContextAware))
            {
                ((MuleContextAware) managedObject).setMuleContext(getMuleContext());
            }

            if ((initialised) && (managedObject instanceof Initialisable))
            {
                ((Initialisable) managedObject).initialise();
            }

            if ((started) && (managedObject instanceof Startable))
            {
                ((Startable) managedObject).start();
            }
        }
        catch (MuleException me)
        {
            throw new MuleRuntimeException(me);
        }

        return managedObject;
    }

    protected <O> O transitionLifecycleManagedObjectForRemoval(O managedObject)
    {
        try
        {
            if (managedObject instanceof Stoppable)
            {
                ((Stoppable) managedObject).stop();
            }

            if (managedObject instanceof Disposable)
            {
                ((Disposable) managedObject).dispose();
            }
        }
        catch (MuleException me)
        {
            throw new MuleRuntimeException(me);
        }

        return managedObject;
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

    public boolean isInitialised()
    {
        return initialised;
    }

    public boolean isStarted()
    {
        return started;
    }
}
