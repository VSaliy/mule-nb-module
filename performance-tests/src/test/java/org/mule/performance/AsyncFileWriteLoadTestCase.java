package org.mule.performance;

import org.mule.api.store.ObjectStore;

import java.util.concurrent.Callable;

/**
 *
 */
public class AsyncFileWriteLoadTestCase extends AbstractFileWriteLoadTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "async-file-write-config.xml";
    }

    @Override
    protected Callable<Integer> createLoadTestTask(int i)
    {
        return new AsyncLoadTestTask(i);
    }

    private static class AsyncLoadTestTask implements Callable<Integer>
    {

        private int taskId;

        public AsyncLoadTestTask(int taskId)
        {
            this.taskId = taskId;
        }

        public Integer call() throws Exception
        {
            final ObjectStore objectStore = muleContext.getRegistry().lookupObject("objectStore");
            for (int message = 0; message < MESSAGE_PER_THREAD; message++)
            {
                objectStore.store(requestCount.addAndGet(1), SAMPLE_MESSAGE);
            }

            return MESSAGE_PER_THREAD;
        }
    }
}
