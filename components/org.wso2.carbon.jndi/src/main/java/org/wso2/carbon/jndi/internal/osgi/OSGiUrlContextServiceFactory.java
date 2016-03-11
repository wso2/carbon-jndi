package org.wso2.carbon.jndi.internal.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * A {@code ServiceFactory} which supplies instance of the OSGiURLContextFactory.
 */
public class OSGiUrlContextServiceFactory implements ServiceFactory<OSGiUrlContextFactory> {

    @Override
    public OSGiUrlContextFactory getService(Bundle bundle, ServiceRegistration<OSGiUrlContextFactory> serviceRegistration) {
        return new OSGiUrlContextFactory(bundle.getBundleContext());
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<OSGiUrlContextFactory> serviceRegistration, OSGiUrlContextFactory osGiURLContextFactory) {

    }
}
