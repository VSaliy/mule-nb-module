package org.mule.module.async.internal.config;

import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributesAndChildren;
import org.mule.module.async.internal.config.factories.AsyncChoiceRouterFactoryBean;
import org.mule.module.async.internal.config.factories.AsyncMessageProcessorFilterPairFactoryBean;
import org.mule.module.async.pattern.router.AsyncForeach;

public class AsyncNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("flow", new AsyncFlowDefinitionParser());
        registerMuleBeanDefinitionParser("foreach", new ChildDefinitionParser("messageProcessor", AsyncForeach.class)).addAlias("collection", "collectionExpression");

        registerBeanDefinitionParser("choice", new ChildDefinitionParser("messageProcessor", AsyncChoiceRouterFactoryBean.class));
        registerBeanDefinitionParser("when", (ChildDefinitionParser) new ChildDefinitionParser("route", AsyncMessageProcessorFilterPairFactoryBean.class).registerPreProcessor(new CheckExclusiveAttributesAndChildren(new String[] {
                "expression"}, new String[] {"{http://www.mulesoft.org/schema/mule/core}abstractFilterType"})));
        registerBeanDefinitionParser("otherwise", new ChildDefinitionParser("defaultRoute", AsyncMessageProcessorFilterPairFactoryBean.class));


    }
}
