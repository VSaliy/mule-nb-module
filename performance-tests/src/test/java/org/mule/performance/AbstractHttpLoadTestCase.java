package org.mule.performance;

import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

public abstract class AbstractHttpLoadTestCase extends FunctionalTestCase
{

    public static final int THREAD_COUNT = 1;
    public static final int MESSAGE_PER_THREAD = 1;

    @Test
    public void testName() throws Exception
    {
        long startTimestamp = System.currentTimeMillis();
        generateLoad(THREAD_COUNT);
        long endTimestamp = System.currentTimeMillis();

        logger.error("LOAD TEST TIME: " + (endTimestamp - startTimestamp) + "ms");
    }

    @Override
    public int getTestTimeoutSecs()
    {
        return 500000;
    }

    private void generateLoad(int threadCount) throws InterruptedException, ExecutionException
    {
        final Collection<Callable<Integer>> solvers = new ArrayList<Callable<Integer>>(threadCount);
        for (int i = 0; i < threadCount; i++)
        {
            solvers.add(new LoadTestTask(i));
        }
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);

        CompletionService<Integer> ecs = new ExecutorCompletionService<Integer>(exec);
        for (Callable<Integer> s : solvers)
        {
            ecs.submit(s);
        }

        Integer count = 0;

        for (int i = 0; i < threadCount; ++i)
        {
            count = count + ecs.take().get();
            logger.error("Current record processed count: " + count);
        }

        logger.error("Finishing load generation");
    }

    private static class LoadTestTask implements Callable<Integer>
    {

        private int taskId;

        public LoadTestTask(int taskId)
        {
            this.taskId = taskId;
        }

        public Integer call() throws Exception
        {
            LocalMuleClient client = muleContext.getClient();

            String url = "http://localhost:7575";

            for (int message = 0; message < MESSAGE_PER_THREAD; message++)
            {
                GetMethod httpGet = new GetMethod(url);
                int statusCode = new HttpClient().executeMethod(httpGet);
                //assertEquals(200, statusCode);
                if (statusCode != 200)
                {
                    System.out.println("Status code: " + statusCode);
                }
                //MuleMessage response = muleContext.getClient().send("vm://testInput", TEST_MESSAGE, null);
                //System.out.println("RESPONSE: " + response.getPayloadAsString());
            }

            return MESSAGE_PER_THREAD;
        }
    }

}
