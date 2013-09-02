package org.mule.module.nb.config;

import org.mule.config.spring.handlers.MuleNamespaceHandler;

public class NBNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("flow", new RestFlowDefinitionParser());
    }
}
