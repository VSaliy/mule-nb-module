package org.mule.performance;

/**
 *
 */
public class SyncHttpLoadTestCase extends AbstractHttpLoadTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "sync-mule-config.xml";
    }
}
