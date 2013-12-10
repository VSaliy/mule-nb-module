package org.mule.module.async.vm.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.async.vm.source.MemoryMessageSource;

public class VmNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        registerBeanDefinitionParser("memory-source", new ChildDefinitionParser("messageSource", MemoryMessageSource.class));
    }
}
