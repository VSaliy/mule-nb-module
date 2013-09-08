package org.mule.module.async.config;

import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.async.netty.processor.NettyClient;
import org.mule.module.async.netty.source.NettyMessageSource;

public class AsyncNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("flow", new RestFlowDefinitionParser());
        registerBeanDefinitionParser("netty-source", new ChildDefinitionParser("messageSource", NettyMessageSource.class));
        registerBeanDefinitionParser("netty-client", new ChildDefinitionParser("messageProcessor", NettyClient.class));
    }
}
