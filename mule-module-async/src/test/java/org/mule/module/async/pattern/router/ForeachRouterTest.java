/**
 *
 */
package org.mule.module.async.pattern.router;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ForeachRouterTest extends FunctionalTestCase
{


    @Test
    public void testForeach() throws Exception
    {
        AsyncFlow flow = (AsyncFlow) getFlowConstruct("test");
        flow.getAsyncChain().process(getTestEvent(new String[] {"TestData", "TestData"}), new MessageProcessorCallback()
        {

            @Override
            public void onSuccess(MuleEvent event)
            {

            }

            @Override
            public void onException(MuleEvent event, MuleException e)
            {

            }
        });
    }

    @Override
    protected String getConfigResources()
    {
        return "foreach-async-simple.xml";
    }
}
