package org.wso2.carbon.jndi.sample.osgi.internal;

import org.osgi.framework.BundleContext;

/**
 * Data holder for osgi-url-scheme sample.
 */
public class DataHolder {

    private static DataHolder dataHolderInstance = new DataHolder();

    private BundleContext bundleContext;

    public static DataHolder getDataHolderInstance() {
        return dataHolderInstance;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
