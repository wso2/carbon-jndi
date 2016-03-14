package org.wso2.carbon.jndi.sample.osgi.internal;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jndi.JNDIContextManager;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * This sample illustrate how to use the {@code JNDIContextManager} OSGi service to create the {@code InitialContext}
 * with osgi declarative services.
 *
 * @since 1.0.0
 */
public class ActivatorComponent {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Activator.class);

    @Reference(
            name = "org.osgi.service.jndi",
            service = JNDIContextManager.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindNDIContextManager"
    )
    protected void bindJNDIContextManager(JNDIContextManager jndiContextManager) throws NamingException {

        Context initialContext = jndiContextManager.newInitialContext();

        Context envContext = initialContext.createSubcontext("java:comp/env");
        envContext.bind("company", "wso2");

        String value = (String) initialContext.lookup("java:comp/env/company");
        logger.info("JNDI Lookup(java:comp/env/company) value: {}", value);
    }

    protected void unbindNDIContextManager(JNDIContextManager jndiContextManager) throws NamingException {
        jndiContextManager.newInitialContext().close();
    }
}
