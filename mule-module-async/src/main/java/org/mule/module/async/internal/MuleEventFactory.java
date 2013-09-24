package org.mule.module.async.internal;

import org.mule.api.MuleEvent;

/**
 *
 */
public interface MuleEventFactory
{
      MuleEvent  create(Object transportMessage, String encoding) throws Exception;
}
