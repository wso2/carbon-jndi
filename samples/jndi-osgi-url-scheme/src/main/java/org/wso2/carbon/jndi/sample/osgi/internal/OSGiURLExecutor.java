/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIContextManager;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.jndi.sample.osgi.services.FooService;
import org.wso2.carbon.jndi.sample.osgi.services.impl.FooServiceImpl;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Sample executor to test operations using osgi url scheme.
 */
public class OSGiURLExecutor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OSGiURLExecutor.class);

    public void executeOSGIURLScheme() throws NamingException {
        BundleContext bundleContext = DataHolder.getDataHolderInstance().getBundleContext();
        JNDIContextManager jndiContextManager;

        ServiceReference<JNDIContextManager> contextManagerSRef = bundleContext.getServiceReference(
                JNDIContextManager.class);

        jndiContextManager = Optional.ofNullable(contextManagerSRef)
                .map(bundleContext::getService)
                .orElseThrow(() -> new RuntimeException("JNDIContextManager service is not available."));

        Context initialContext = jndiContextManager.newInitialContext();

        accessServiceWithJNDIServiceName(bundleContext, initialContext);

        retrieveBundleContext(bundleContext, initialContext);

        listServices(bundleContext, initialContext);
    }

    private void accessServiceWithJNDIServiceName(BundleContext bundleContext, Context context) throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 10);
        propertyMap.put("osgi.jndi.service.name", "foo/myService");
        FooService fooService = new FooServiceImpl();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);

        FooService service = (FooService) context.lookup(
                "osgi:service/org.wso2.carbon.jndi.sample.osgi.services.FooService");

        FooService jndiService = (FooService) context.lookup("osgi:service/foo/myService");

        logger.info("JNDI lookup message with OSGi URL scheme is : " + service.getMessage());
        logger.info("JNDI lookup message with OSGi URL scheme and JNDI service name is : " + jndiService.getMessage());
        fooServiceRegistration.unregister();
    }

    private void retrieveBundleContext(BundleContext bundleContext, Context context) throws NamingException {
        FooService fooService = new FooServiceImpl();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, null);

        BundleContext owningBundleContext = (BundleContext) context.lookup("osgi:framework/bundleContext");

        ServiceReference serviceReference =
                owningBundleContext.getServiceReference("org.wso2.carbon.jndi.osgi.services.FooService");

        FooService service = (FooService) owningBundleContext.getService(serviceReference);

        logger.info("The service retrieved from the bundleContext has the message : " + service.getMessage());
        fooServiceRegistration.unregister();
    }

    private void listServices(BundleContext bundleContext, Context context) throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("osgi.jndi.service.name", "foo/myService");

        FooService fooService = new FooServiceImpl();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);

        NamingEnumeration<Binding> listBindings =
                context.listBindings("osgi:service/foo/myService");

        Binding binding = listBindings.nextElement();

        logger.info("First service retrieved from the listBinding has the class : " + binding.getClassName());
        listBindings.close();  //call the close() method so that it will unget all the gotten services.
        fooServiceRegistration.unregister();
    }
}
