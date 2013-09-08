/**
 *
 */
package org.mule.module.async.netty;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Ignore;
import org.junit.Test;

public class NettyMessageSourceTest   extends FunctionalTestCase
{

    @Test
    @Ignore
    public void test() throws Exception
    {
        System.out.println("Hola");
        Thread.sleep(10000000);

    }

    @Override
    protected String getConfigResources()
    {
        return "nb-simple.xml";
    }
}
