/**
 *
 */
package org.mule.module.nb.processor.netty;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.processor.LoggerMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.nb.processor.NBFlow;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.Mockito;

public class HttpNettyMessageSourceTest
{

    @Test
    public void test() throws Exception
    {
        HttpNettyMessageSource httpNettyMessageSource = new HttpNettyMessageSource();
        httpNettyMessageSource.setPort(1234);
        MuleContext muleContext = Mockito.mock(MuleContext.class);
        MuleConfiguration muleConfiguration = Mockito.mock(MuleConfiguration.class);
        Mockito.when(muleConfiguration.getDefaultResponseTimeout()).thenReturn(10000);
        Mockito.when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
        NBFlow flow = new NBFlow("Test", muleContext);
        flow.setMessageProcessors(Arrays.<MessageProcessor>asList(new LoggerMessageProcessor()));
        flow.setMessageSource(httpNettyMessageSource);
        flow.initialise();
        flow.start();
        Thread.sleep(100000000);

    }

}
