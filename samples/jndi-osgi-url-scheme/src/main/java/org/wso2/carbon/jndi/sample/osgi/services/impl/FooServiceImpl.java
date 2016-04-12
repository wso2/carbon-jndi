package org.wso2.carbon.jndi.sample.osgi.services.impl;

import org.wso2.carbon.jndi.sample.osgi.services.FooService;

/**
 * Sample implementation for FooService.
 */
public class FooServiceImpl implements FooService {

    @Override
    public String getMessage() {
        return "Service Implementation for Foo";
    }
}
