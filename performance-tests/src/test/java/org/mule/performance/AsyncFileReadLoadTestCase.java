package org.mule.performance;

/**
 *
 */
public class AsyncFileReadLoadTestCase extends AbstractFileReadLoadTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "async-file-read-config.xml";
    }
}
