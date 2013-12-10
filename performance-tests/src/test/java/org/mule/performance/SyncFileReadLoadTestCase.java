package org.mule.performance;

/**
 *
 */
public class SyncFileReadLoadTestCase extends AbstractFileReadLoadTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "sync-file-read-confing.xml";
    }
}
