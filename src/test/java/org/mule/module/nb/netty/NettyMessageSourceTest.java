/**
 *
 */
package org.mule.module.nb.netty;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class NettyMessageSourceTest   extends FunctionalTestCase
{

    @Test
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
