/**
 *
 */
package org.mule.module.async.pattern.router;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.async.processor.MessageProcessorCallback;
import org.mule.tck.junit4.FunctionalTestCase;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class ChoicerRouterTest extends FunctionalTestCase
{

    @Test
    public void testChoice() throws Exception
    {
        AsyncFlow flow = (AsyncFlow) getFlowConstruct("test");
        flow.getAsyncChain().process(getTestEvent("TestData"), new MessageProcessorCallback()
        {

            @Override
            public void onSuccess(MuleEvent event)
            {
                Assert.assertThat((String) event.getMessage().getPayload(), CoreMatchers.is("good"));
            }

            @Override
            public void onException(MuleEvent event, MuleException e)
            {
                Assert.fail(e.getMessage());
            }
        });
    }

    @Override
    protected String getConfigResources()
    {
        return "choice-async-simple.xml";
    }
}
