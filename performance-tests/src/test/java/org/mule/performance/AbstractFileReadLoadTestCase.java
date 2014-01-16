package org.mule.performance;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.FileUtils;
import org.mule.util.concurrent.NamedThreadFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public abstract class AbstractFileReadLoadTestCase extends FunctionalTestCase
{

    public static final int THREAD_COUNT = 1;
    public static final int MESSAGE_PER_THREAD = 1;
    public static final int EXPECTED_RESPONSES = THREAD_COUNT * MESSAGE_PER_THREAD;
    public static AtomicInteger responseCount = new AtomicInteger(0);
    public static String SAMPLE_MESSAGE;
    private final AtomicInteger requestCount = new AtomicInteger(0);


    protected AbstractFileReadLoadTestCase()
    {
        setStartContext(false);
    }

    @BeforeClass
    public static void setupTmpDirectory()
    {
        System.setProperty("directory", getTargetFolder());
    }


    @Before
    public void generateLoad() throws ExecutionException, InterruptedException, IOException, URISyntaxException
    {

        responseCount = new AtomicInteger(0);
        final URL resource = getClass().getClassLoader().getResource("fileContent.txt");
        final File file = new File(resource.toURI());
        SAMPLE_MESSAGE = FileUtils.readFileToString(file);
        generateLoad(THREAD_COUNT);
    }

    @Test
    public void loadTest() throws Exception
    {
        long startTimestamp = System.currentTimeMillis();
        muleContext.start();
        verifyData();
        long endTimestamp = System.currentTimeMillis();
        logger.error(" >>>>> LOAD TEST TIME: " + (endTimestamp - startTimestamp) + "ms");
    }

    private static String getTargetFolder()
    {
        return System.getProperty("java.io.tmpdir") + File.separator + "testIn";
    }


    @Override
    public int getTestTimeoutSecs()
    {
        return 500000;
    }

    private void generateLoad(int threadCount) throws InterruptedException, ExecutionException
    {
        File container = new File(getTargetFolder());
        try
        {
            FileUtils.deleteDirectory(container);
            container.mkdirs();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        Collection<Callable<Integer>> solvers = new ArrayList<Callable<Integer>>(threadCount);
        for (int i = 0; i < threadCount; i++)
        {
            solvers.add(new LoadTestTask());
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

        logger.error("Finishing load generation at " + getTargetFolder());
    }

    private class LoadTestTask implements Callable<Integer>
    {

        public LoadTestTask()
        {
        }

        public Integer call() throws Exception
        {

            for (int message = 0; message < MESSAGE_PER_THREAD; message++)
            {
                File file = new File(getTargetFolder(), "input-sample" + requestCount.addAndGet(1) + ".txt");
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
                return responseCount.get() >= EXPECTED_RESPONSES;
            }

            public String describeFailure()
            {
                return String.format("Received %s messages but was expecting %s", responseCount, EXPECTED_RESPONSES);
            }
        });

        assertEquals(EXPECTED_RESPONSES, requestCount.get());
        final File folder = new File(getTargetFolder());
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
            responseCount.incrementAndGet();
            return value;
        }
    }

}
