/*
 *  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.jndi.sample.osgi.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIContextManager;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import javax.naming.Context;

/**
 * This sample illustrate how to use the {@code JNDIContextManager} OSGi service to create the {@code InitialContext}.
 *
 * @since 1.0.0
 */
public class Activator implements BundleActivator {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Activator.class);

    /**
     * This is called when the bundle is started.
     *
     * @param bundleContext BundleContext of this bundle
     * @throws Exception Could be thrown while bundle starting
     */
    public void start(BundleContext bundleContext) throws Exception {
        JNDIContextManager jndiContextManager;

        ServiceReference<JNDIContextManager> contextManagerSRef = bundleContext.getServiceReference(
                JNDIContextManager.class);

        jndiContextManager = Optional.ofNullable(contextManagerSRef)
                .map(bundleContext::getService)
                .orElseThrow(() -> new RuntimeException("JNDIContextManager service is not available."));

        Context initialContext = jndiContextManager.newInitialContext();

        Context envContext = initialContext.createSubcontext("java:comp/env");
        envContext.bind("company", "wso2");

        String value = (String) initialContext.lookup("java:comp/env/company");
        logger.info("JNDI Lookup(java:comp/env/company) value: {}", value);
    }

    /**
     * This is called when the bundle is stopped.
     *
     * @param bundleContext BundleContext of this bundle
     * @throws Exception Could be thrown while bundle stopping
     */
    public void stop(BundleContext bundleContext) throws Exception {
    }
}
