/**
 *
 */
package org.mule.module.async.netty.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.async.netty.processor.NettyClient;
import org.mule.module.async.netty.source.NettyMessageSource;

public class NettyNamesapceHandler    extends org.mule.config.spring.handlers.AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        //Netty Module TODO refactor to new module
        registerBeanDefinitionParser("netty-source", new ChildDefinitionParser("messageSource", NettyMessageSource.class));
        registerBeanDefinitionParser("netty-client", new ChildDefinitionParser("messageProcessor", NettyClient.class));
    }
}
