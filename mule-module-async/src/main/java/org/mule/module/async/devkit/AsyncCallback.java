package org.mule.module.async.devkit;

import org.mule.api.MuleException;

import java.util.Map;

/**
 * Callback for devkit Connectors
 */
public interface AsyncCallback
{

    void onSuccess(Object payload, Map<String, Object> flowVars);

    void onSuccess(Object payload);

    void onException(MuleException e);
}
