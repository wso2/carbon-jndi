/*
*  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.carbon.jndi.internal.osgi.factory;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.jndi.internal.DataHolder;
import org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory;

import java.util.Hashtable;
import java.util.Optional;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;

import static org.wso2.carbon.jndi.internal.Constants.OSGI_SERVICE_JNDI_BC;
import static org.wso2.carbon.jndi.internal.util.LambdaExceptionUtils.rethrowFunction;

/**
 * This class represents the default context factory which is used by the {@code DefaultContextFactoryBuilder}
 */
public class DefaultContextFactory implements InitialContextFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultContextFactory.class);

    /**
     * Creates an initial context from the JNDIContextManager OSGi service retrieved from the caller bundle context.
     *
     * @param environment The possibly null environment
     *                    specifying information to be used in the creation
     *                    of the initial context.
     * @return A non-null initial context object that implements the Context interface.
     * @throws NamingException If cannot create an initial context.
     */
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {

        if (DataHolder.getInstance().getBundleContext() == null) {
            //Get initial context object for non-OSGi environment.
            return getInitialContextForSPI(environment);
        } else {

            //1) Find the BundleContext of the caller of this method. If a BundleContext cannot be found
            //   then throw NoInitialContext Exception.
            Optional<BundleContext> bundleContextOptional = getCallersBundleContext(environment);
            bundleContextOptional.orElseThrow(NoInitialContextException::new);

            //2) Retrieve the JNDIContextManager service from the BundleContext and invoke getInitialContext method.
            //   If no BundleContext is found then, throw NoInitialContext Exception.
            BundleContext callersBC = bundleContextOptional.get();
            Optional<ServiceReference<JNDIContextManager>> contextManagerSR = Optional
                    .ofNullable(callersBC.getServiceReference(JNDIContextManager.class));

            return contextManagerSR.map(callersBC::getService)
                    .map(rethrowFunction(jndiContextManager -> jndiContextManager.newInitialContext(environment)))
                    .orElseThrow(NoInitialContextException::new);
        }
    }

    /**
     * Creates an initial context for non-OSGi environment from the INITIAL_CONTEXT_FACTORY class and if it is not
     * defined creates it using InMemoryInitialContextFactory by default.
     *
     * @param environment The environment to be used in the creation of the initial context.
     * @return A non-null initial context object that implements the Context interface.
     * @throws NamingException If an initial context can't be created.
     */
    public Context getInitialContextForSPI(Hashtable<?, ?> environment) throws NamingException {
        String initialContextFactoryClassName = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (initialContextFactoryClassName == null || initialContextFactoryClassName.isEmpty()) {
            return new InMemoryInitialContextFactory().getInitialContext(environment);
        }

        try {
            InitialContextFactory ctxFactory = (InitialContextFactory) Class.forName(initialContextFactoryClassName)
                    .newInstance();
            return ctxFactory.getInitialContext(environment);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.error("Couldn't create initial context using the InitialContextFactory class "
                    + initialContextFactoryClassName, e);
            throw new NamingException(e.getMessage());
        }
    }

    /**
     * Returns caller's bundle context object.
     * <p>
     * 1) Get the bundle context from the given environment properties.
     * 2) If not, from the Thread Context Class Loader.
     * 3) If not, from the current class stack
     *
     * @param environment The possibly null environment
     *                    specifying information to be used in the creation
     *                    of the initial context.
     * @return an {@code Optional} describing the caller's bundle context
     */
    private Optional<BundleContext> getCallersBundleContext(Hashtable<?, ?> environment) {
        Optional<BundleContext> bundleContextOptional;

        // Get the bundle context from the given environment properties.
        bundleContextOptional = getBundleContextFromEnvironment(environment);
        if (bundleContextOptional.isPresent()) {
            return bundleContextOptional;
        }

        // If not, from the Thread Context Class Loader.
        bundleContextOptional = getBundleContextFromTCCL();
        if (bundleContextOptional.isPresent()) {
            return bundleContextOptional;
        }

        // If not, from the current class stack
        return getBundleContextFromCurrentClassStack();
    }

    /**
     * Returns the bundle context object extracted from the environment properties.
     * <p>
     * Look in the JNDI environment properties for a property called osgi.service.jndi.bundleContext.
     * If a value for this property exists then use it as the Bundle Context.
     * If the Bundle Context has been found then stop.
     *
     * @param environment The possibly null environment
     *                    specifying information to be used in the creation
     *                    of the initial context.
     * @return an {@code Optional} describing the caller's bundle context
     */
    private Optional<BundleContext> getBundleContextFromEnvironment(Hashtable<?, ?> environment) {
        Optional<BundleContext> bundleContextOptional;
        Object obj = environment.get(OSGI_SERVICE_JNDI_BC);
        bundleContextOptional = Optional.ofNullable(obj)
                .filter(o -> o instanceof BundleContext)
                .map(o -> (BundleContext) o);
        return bundleContextOptional;
    }

    /**
     * Returns the bundle context object obtained from the Thread Context Class Loader.
     * <p>
     * Obtain the Thread Context Class Loader; if it, or an ancestor class loader, implements the
     * BundleReference interface, call its getBundle method to get the client's Bundle; then call
     * getBundleContext on the Bundle object to get the client's Bundle Context.
     * If the Bundle Context has been found stop.
     *
     * @return an {@code Optional} describing the caller's bundle context
     */
    private Optional<BundleContext> getBundleContextFromTCCL() {
        Optional<BundleContext> bundleContextOptional;
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        bundleContextOptional = getCallersBundleContext(Optional.of(tccl));
        return bundleContextOptional;
    }

    /**
     * Returns the caller's bundle context retrieved from the current class context.
     * <p>
     * Current class context can be extracted from a sub class of the SecurityManager. You need to create a sub class
     * because the method which returns the current class stack is protected method in the SecurityManager.
     * <p>
     * Walk the call stack until the invoker is found. The invoker can be the caller of the InitialContext class
     * constructor or the NamingManager or DirectoryManager getObjectInstance methods.
     * Get the class loader of the caller and see if it, or an ancestor, implements the
     * BundleReference interface.
     * If a Class Loader implementing the BundleReference interface is found call the getBundle method to
     * get the clients Bundle; then call the getBundleContext method on the Bundle to get the
     * clients Bundle Context.
     * If the Bundle Context has been found stop, else continue with the next stack frame.
     *
     * @return an {@code Optional} describing the caller's bundle context retrieved from the current class context.
     */
    private Optional<BundleContext> getBundleContextFromCurrentClassStack() {
        Optional<BundleContext> bundleContextOptional = Optional.empty();

        Class[] currentClassStack = new DummySecurityManager().getClassContext();

        int index;
        boolean found = false;
        for (index = 0; index < currentClassStack.length; index++) {
            Class clazz = currentClassStack[index];

            if (found) {
                bundleContextOptional = getCallersBundleContext(Optional.ofNullable(clazz.getClassLoader()));
                if (bundleContextOptional.isPresent()) {
                    break;
                }
            }

            // Check whether NamingManager or InitialContext is a superclass or super interface of the current class.
            if (!found &&
                    (NamingManager.class.isAssignableFrom(clazz) || InitialContext.class.isAssignableFrom(clazz))) {
                found = true;
            }
        }
        return bundleContextOptional;
    }

    /**
     * Returns a BundleContext obtained from the given ClassLoader or from an ancestor ClassLoader.
     * <p>
     * First check whether this ClassLoader implements the BundleReference interface, if so try to get BundleContext.
     * If not get from the parent ClassLoader. Repeat these steps until a not-null BundleContext is found or the parent
     * ClassLoader becomes null.
     *
     * @param classLoaderOptional an {@code Optional} describing a ClassLoader
     * @return BundleContext extracted from the give ClassLoader or from its ancestors ClassLoaders.
     */
    private Optional<BundleContext> getCallersBundleContext(Optional<ClassLoader> classLoaderOptional) {

        if (!classLoaderOptional.isPresent()) {
            return Optional.empty();
        }

        Optional<BundleContext> bundleContextOptional = classLoaderOptional
                .filter(classLoader -> classLoader instanceof BundleReference)
                .map(classLoader -> (BundleReference) classLoader)
                .map(bundleReference -> bundleReference.getBundle().getBundleContext());

        if (bundleContextOptional.isPresent()) {
            return bundleContextOptional;
        } else {
            return getCallersBundleContext(Optional.ofNullable(classLoaderOptional.get().getParent()));
        }
    }

    // Creating a local class which extends SecurityManager to get the current execution
    // stack as an array of classes
    static class DummySecurityManager extends SecurityManager {
        public Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
}
