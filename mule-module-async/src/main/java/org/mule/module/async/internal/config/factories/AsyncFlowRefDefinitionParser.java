package org.mule.module.async.internal.config.factories;

import org.mule.config.spring.parsers.generic.AutoIdUtils;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class AsyncFlowRefDefinitionParser extends ChildDefinitionParser
{

    public AsyncFlowRefDefinitionParser()
    {
        super("messageProcessor", AsyncFlowRefFactoryBean.class);
    }

    @Override
    protected void parseChild(Element element, ParserContext parserContext, BeanDefinitionBuilder builder)
    {
        super.parseChild(element, parserContext, builder);
    }

    public String getBeanName(Element element)
    {
        return AutoIdUtils.uniqueValue("flow-ref." + element.getAttribute(ATTRIBUTE_NAME));
    }

    @Override
    protected void checkElementNameUnique(Element element)
    {
        // We want to check element name exists
    }

}
