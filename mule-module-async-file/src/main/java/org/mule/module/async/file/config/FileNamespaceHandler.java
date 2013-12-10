package org.mule.module.async.file.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.async.file.processor.FileWriter;
import org.mule.module.async.file.source.FileMessageSource;
import org.mule.module.async.file.source.FileToStringTransformer;

public class FileNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        registerBeanDefinitionParser("file-writer", new ChildDefinitionParser("messageProcessor", FileWriter.class));
        registerBeanDefinitionParser("file-source", new ChildDefinitionParser("messageSource", FileMessageSource.class));
        registerBeanDefinitionParser("file-to-string", new ChildDefinitionParser("messageProcessor", FileToStringTransformer.class));
    }
}
