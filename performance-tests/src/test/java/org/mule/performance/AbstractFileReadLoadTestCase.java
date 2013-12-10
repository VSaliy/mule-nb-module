package org.mule.performance;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.FileUtils;
import org.mule.util.concurrent.NamedThreadFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public abstract class AbstractFileReadLoadTestCase extends FunctionalTestCase
{
    public static final int THREAD_COUNT = 100;
    public static final int MESSAGE_PER_THREAD = 20;
    public static final int EXPECTED_RESPONSES = THREAD_COUNT * MESSAGE_PER_THREAD;

    public static final Object lock = new Object();
    public static volatile int responseCount = 0;
    public static String SAMPLE_MESSAGE;

    private static final AtomicInteger requestCount = new AtomicInteger(0);

    protected AbstractFileReadLoadTestCase()
    {
        setStartContext(false);
    }

    @Test
    public void testName() throws Exception
    {
        final URL resource = getClass().getClassLoader().getResource("fileContent.txt");
        final File file = new File(resource.toURI());
        SAMPLE_MESSAGE = FileUtils.readFileToString(file);

        generateLoad(THREAD_COUNT);

        long startTimestamp = System.currentTimeMillis();
        muleContext.start();
        verifyData();
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

        Collection<Callable<Integer>> solvers = new ArrayList<Callable<Integer>>(threadCount);
        for (int i = 0; i < threadCount; i++)
        {
            solvers.add(new LoadTestTask(i));
        }
        ExecutorService exec = Executors.newFixedThreadPool(threadCount, new NamedThreadFactory("testClient"));

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
            for (int message = 0; message < MESSAGE_PER_THREAD; message++)
            {
                File file = new File("/tmp/testIn/zaraza" + requestCount.addAndGet(1) + ".txt");
                FileUtils.writeStringToFile(file, SAMPLE_MESSAGE);
            }

            return MESSAGE_PER_THREAD;
        }
    }
    private void verifyData() throws MuleException
    {
        PollingProber prober = new PollingProber(30000, 10);
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return responseCount >= EXPECTED_RESPONSES;
            }

            public String describeFailure()
            {
                return String.format("Received %s messages but was expecting %s", responseCount, EXPECTED_RESPONSES);
            }
        });

        assertEquals(EXPECTED_RESPONSES, requestCount.get());
        final File folder = new File("/tmp/testIn");
        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {

                String[] list = folder.list();

                return list.length == 0;
            }

            public String describeFailure()
            {
                return "Failed to delete al the input files";
            }
        });
    }

    public static class TestCounter
    {

        public Object process(Object value)
        {

            synchronized (lock)
            {
                responseCount++;
            }
            return value;
        }
    }

}
