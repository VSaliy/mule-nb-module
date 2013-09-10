package org.mule.module.async.config;

import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.module.async.config.factories.AsyncChoiceRouterFactoryBean;
import org.mule.module.async.config.factories.AsyncMessageProcessorFilterPairFactoryBean;
import org.mule.module.async.netty.processor.NettyClient;
import org.mule.module.async.netty.source.NettyMessageSource;

public class AsyncNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("flow", new AsyncFlowDefinitionParser());
        registerBeanDefinitionParser("netty-source", new ChildDefinitionParser("messageSource", NettyMessageSource.class));
        registerBeanDefinitionParser("netty-client", new ChildDefinitionParser("messageProcessor", NettyClient.class));


        registerBeanDefinitionParser("choice", new ChildDefinitionParser("messageProcessor", AsyncChoiceRouterFactoryBean.class));
        registerBeanDefinitionParser("when", (ChildDefinitionParser)new ChildDefinitionParser("route", AsyncMessageProcessorFilterPairFactoryBean.class).registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[]{
                "expression"}, new String[]{"{http://www.mulesoft.org/schema/mule/core}abstractFilterType"})));
        registerBeanDefinitionParser("otherwise", new ChildDefinitionParser("defaultRoute", AsyncMessageProcessorFilterPairFactoryBean.class));

    }
}
