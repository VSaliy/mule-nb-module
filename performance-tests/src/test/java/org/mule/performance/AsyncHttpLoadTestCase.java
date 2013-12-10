package org.mule.performance;

public class AsyncHttpLoadTestCase extends AbstractHttpLoadTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "async-mule-config.xml";
    }
}
