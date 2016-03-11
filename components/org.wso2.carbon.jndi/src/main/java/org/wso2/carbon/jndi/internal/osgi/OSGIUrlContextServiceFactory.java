package org.wso2.carbon.jndi.internal.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * A {@code ServiceFactory} which supplies instance of the OSGiURLContextFactory.
 */
public class OSGIUrlContextServiceFactory implements ServiceFactory<OSGiURLContextFactory> {

    @Override
    public OSGiURLContextFactory getService(Bundle bundle, ServiceRegistration<OSGiURLContextFactory> serviceRegistration) {
        return new OSGiURLContextFactory(bundle.getBundleContext());
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<OSGiURLContextFactory> serviceRegistration, OSGiURLContextFactory osGiURLContextFactory) {

    }
}
