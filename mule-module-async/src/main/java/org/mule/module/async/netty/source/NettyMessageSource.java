/**
 *
 */
package org.mule.module.async.netty.source;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
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

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

public class NettyMessageSource implements MessageSource, Initialisable, Startable, Stoppable, MuleContextAware, FlowConstructAware
{

    private AsyncMessageProcessor asyncMessageProcessor;
    private ServerBootstrap bootstrap;
    private int port;
    private FlowConstruct flowConstruct;
    private MuleContext context;

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
            channelFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                                                               Executors.newCachedThreadPool(),
                                                               Runtime.getRuntime().availableProcessors() * 2);
            bootstrap = new ServerBootstrap(channelFactory);
            // Enable TCP_NODELAY to handle pipelined requests without latency.
            bootstrap.setOption("child.tcpNoDelay", true);
            // Set up the event pipeline factory.
            DefaultMuleEventFactory muleEventFactory = new DefaultMuleEventFactory(new NettyMuleMessageFactory(context), flowConstruct, MessageExchangePattern.REQUEST_RESPONSE);
            bootstrap.setPipelineFactory(new NettyServerPipelineFactory(asyncMessageProcessor, muleEventFactory));
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

    @Override
    public void start() throws MuleException
    {
        // Bind and start to accept incoming connections.
        bootstrap.bind(new InetSocketAddress(port));
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
        this.context = context;
    }
}
