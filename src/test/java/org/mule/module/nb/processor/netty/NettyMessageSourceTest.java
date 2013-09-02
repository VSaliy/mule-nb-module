/**
 *
 */
package org.mule.module.nb.processor.netty;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.processor.LoggerMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.nb.processor.NBFlow;
import org.mule.module.nb.processor.netty.source.NettyMessageSource;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class NettyMessageSourceTest
{

    @Test
    @Ignore
    public void test() throws Exception
    {
        NettyMessageSource httpNettyMessageSource = new NettyMessageSource();
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
