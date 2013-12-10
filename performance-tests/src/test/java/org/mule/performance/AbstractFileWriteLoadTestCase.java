package org.mule.performance;

import org.mule.api.MuleException;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.file.filters.FilenameWildcardFilter;
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

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public abstract class AbstractFileWriteLoadTestCase extends FunctionalTestCase
{
    public static final int THREAD_COUNT = 50;
    public static final int MESSAGE_PER_THREAD = 50;
    public static final int EXPECTED_RESPONSES = THREAD_COUNT * MESSAGE_PER_THREAD;

    public static final Object lock = new Object();
    public static volatile int responseCount = 0;
    public static String SAMPLE_MESSAGE;

    protected static final AtomicInteger requestCount = new AtomicInteger(0);

    protected AbstractFileWriteLoadTestCase()
    {
        //setStartContext(false);
    }

    @Before
    public void setUp() throws Exception
    {
        File outputFolder = new File("/tmp/testOut");
        final String[] list = outputFolder.list(new FilenameWildcardFilter("*.txt"));
        if (list == null)
        {
            return;
        }
        for (String file : list)
        {
            final File file1 = new File(outputFolder, file);
            file1.delete();
        }
    }

    @Test
    public void testName() throws Exception
    {
        final URL resource = getClass().getClassLoader().getResource("fileContent.txt");
        final File file = new File(resource.toURI());
        SAMPLE_MESSAGE = FileUtils.readFileToString(file);

        generateLoad(THREAD_COUNT);

        AbstractFlowConstruct testFlow = muleContext.getRegistry().lookupObject("test");
        testFlow.start();

        long startTimestamp = System.currentTimeMillis();
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
            solvers.add(createLoadTestTask(i));
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

    protected abstract Callable<Integer> createLoadTestTask(int i);

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

        //TODO(pablo.kraan): check that all the files were written
        //assertEquals(EXPECTED_RESPONSES, requestCount.get());
        //final File folder = new File("/tmp/testIn");
        //prober.check(new Probe()
        //{
        //    public boolean isSatisfied()
        //    {
        //
        //        String[] list = folder.list();
        //
        //        return list.length == 0;
        //    }
        //
        //    public String describeFailure()
        //    {
        //        return "Failed to delete al the input files";
        //    }
        //});
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