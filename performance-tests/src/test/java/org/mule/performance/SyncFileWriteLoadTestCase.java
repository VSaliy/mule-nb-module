package org.mule.performance;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;

import java.util.concurrent.Callable;

/**
 *
 */
public class SyncFileWriteLoadTestCase extends AbstractFileWriteLoadTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "sync-file-write-config.xml";
    }

    @Override
    protected Callable<Integer> createLoadTestTask(int i)
    {
        return new SyncLoadTestTask(i);
    }

    private static class SyncLoadTestTask implements Callable<Integer>
    {

        private int taskId;

        public SyncLoadTestTask(int taskId)
        {
            this.taskId = taskId;
        }

        public Integer call() throws Exception
        {
            final LocalMuleClient client = muleContext.getClient();
            for (int message = 0; message < MESSAGE_PER_THREAD; message++)
            {

                final MuleMessage request = new DefaultMuleMessage(SAMPLE_MESSAGE, muleContext);
                request.setOutboundProperty("key", requestCount.addAndGet(1));
                client.dispatch("vm://testIn", request);
            }

            return MESSAGE_PER_THREAD;
        }
    }

}
