package org.mule.module.async;

import org.mule.api.source.MessageSource;

/**
 * Message Source that knows about Address
 */
public interface AddressAwareMessageSource extends MessageSource
{

    /**
     * This returns the address of the endpoint.  When this contains a template, it may not be a URI
     *
     * @return the address on which the endpoint sends or receives data
     */
    String getAddress();
}
