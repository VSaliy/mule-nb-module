package org.mule.module.async.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.util.ProcessingStrategyUtils;
import org.mule.module.async.processor.AsyncFlow;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class RestFlowDefinitionParser extends OrphanDefinitionParser
{

    public RestFlowDefinitionParser()
    {
        super(AsyncFlow.class, true);
        addIgnored("abstract");
        addIgnored("name");
        addIgnored("processingStrategy");
    }

    @Override
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        builder.addConstructorArgValue(element.getAttribute(ATTRIBUTE_NAME));
        builder.addConstructorArgReference(MuleProperties.OBJECT_MULE_CONTEXT);
        ProcessingStrategyUtils.configureProcessingStrategy(element, builder,
                                                            ProcessingStrategyUtils.QUEUED_ASYNC_PROCESSING_STRATEGY);
        super.doParse(element, parserContext, builder);
    }
}
