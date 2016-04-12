package org.wso2.carbon.jndi.sample.osgi.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * This sample illustrate how to use the OSGi URL scheme to access services from service registry.
 *
 * @since 1.0.0
 */
public class Activator implements BundleActivator {

    /**
     * This is called when the bundle is started.
     *
     * @param bundleContext BundleContext of this bundle
     * @throws Exception Could be thrown while bundle starting
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        DataHolder.getDataHolderInstance().setBundleContext(bundleContext);

        new OSGiURLExecutor().executeOSGIURLScheme();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {

    }
}
