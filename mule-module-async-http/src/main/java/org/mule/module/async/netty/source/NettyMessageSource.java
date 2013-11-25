/**
 *
 */
package org.mule.module.async.netty.source;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.module.async.internal.DefaultMuleEventFactory;
import org.mule.module.async.processor.AsyncMessageProcessor;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.ThreadNameHelper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class NettyMessageSource implements MessageSource, Initialisable, Startable, Stoppable, MuleContextAware, FlowConstructAware
{

    private AsyncMessageProcessor asyncMessageProcessor;
    private ServerBootstrap bootstrap;
    private int port;
    private String host;
    private FlowConstruct flowConstruct;
    private MuleContext muleContext;

    @Override
    public void setListener(MessageProcessor listener)
    {
        asyncMessageProcessor = (AsyncMessageProcessor) listener;
    }


    @Override
    public void initialise() throws InitialisationException
    {
        // Configure the server.
        if (bootstrap == null)
        {
            final NioServerSocketChannelFactory channelFactory;

            final ExecutorService bossExecutor = Executors.newCachedThreadPool(new NamedThreadFactory(
                    String.format("%s%s.boss", ThreadNameHelper.getPrefix(muleContext), "Netty"),
                    muleContext.getExecutionClassLoader()
            ));

            final NamedThreadFactory threadFactory = new NamedThreadFactory(
                    String.format("%s.worker", ThreadNameHelper.dispatcher(muleContext, "Netty")),
                    muleContext.getExecutionClassLoader()
            );

            final ThreadingProfile tp = muleContext.getDefaultMessageDispatcherThreadingProfile();


            final ThreadPoolExecutor dispatcherExecutor = new ThreadPoolExecutor(32, 32, tp.getThreadTTL(),
                                                                                 TimeUnit.MILLISECONDS,
                                                                                 new ArrayBlockingQueue<Runnable>(1000),
                                                                                 threadFactory,
                                                                                 new ThreadPoolExecutor.AbortPolicy()
            );

            channelFactory = new NioServerSocketChannelFactory(bossExecutor,
                                                               dispatcherExecutor,
                                                               Runtime.getRuntime().availableProcessors() * 2);
            bootstrap = new ServerBootstrap(channelFactory);
            // Enable TCP_NODELAY to handle pipelined requests without latency.
            bootstrap.setOption("child.tcpNoDelay", true);

            // Set up the event pipeline factory.

            try
            {
                URI uri = new URI("http://" + getHost() + ":" + port);
                DefaultMuleEventFactory muleEventFactory = new DefaultMuleEventFactory(new NettyMuleMessageFactory(muleContext), uri, flowConstruct, MessageExchangePattern.REQUEST_RESPONSE);
                bootstrap.setPipelineFactory(new NettyServerPipelineFactory(asyncMessageProcessor, muleEventFactory));
            }
            catch (URISyntaxException e)
            {
                e.printStackTrace();
            }

        }
    }


    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    @Override
    public void start() throws MuleException
    {
        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(getHost(), port));
    }

    @Override
    public void stop() throws MuleException
    {

    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
