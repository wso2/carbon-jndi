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

package org.wso2.carbon.jndi.osgi;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.testng.listener.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.wso2.carbon.jndi.osgi.builders.ABCContextFactoryBuilder;
import org.wso2.carbon.jndi.osgi.builders.NullContextFactoryBuilder;
import org.wso2.carbon.jndi.osgi.builders.XYZContextFactoryBuilder;
import org.wso2.carbon.jndi.osgi.factories.BarInitialContextFactory;
import org.wso2.carbon.jndi.osgi.factories.BundleContextICFServiceFactory;
import org.wso2.carbon.jndi.osgi.factories.ExceptionInitialContextFactory;
import org.wso2.carbon.jndi.osgi.factories.FooInitialContextFactory;
import org.wso2.carbon.jndi.osgi.factories.NullInitialContextFactory;
import org.wso2.carbon.jndi.osgi.objectfactories.FooDirObjectFactory;
import org.wso2.carbon.jndi.osgi.objectfactories.FooObjectFactory;
import org.wso2.carbon.jndi.osgi.objectfactories.JavaxDirObjectFactory;
import org.wso2.carbon.jndi.osgi.objectfactories.JavaxObjectFactory;
import org.wso2.carbon.jndi.osgi.services.FooService;
import org.wso2.carbon.jndi.osgi.services.impl.FooServiceImpl1;
import org.wso2.carbon.jndi.osgi.services.impl.FooServiceImpl2;
import org.wso2.carbon.jndi.osgi.util.DummyBundleClassLoader;
import org.wso2.carbon.kernel.utils.CarbonServerInfo;
import org.wso2.carbon.osgi.test.util.CarbonSysPropConfiguration;
import org.wso2.carbon.osgi.test.util.OSGiTestConfigurationUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.ObjectFactory;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Listeners(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JNDITest {
    @Inject
    private BundleContext bundleContext;

    @Inject
    private CarbonServerInfo carbonServerInfo;

    @Inject
    private JNDIContextManager jndiContextManager;

    @Inject
    private JNDIProviderAdmin jndiProviderAdmin;

    @Configuration
    public Option[] createConfiguration() {
        List<Option> optionList = new ArrayList<>();
        optionList.add(mavenBundle().artifactId("org.wso2.carbon.jndi")
                .groupId("org.wso2.carbon.jndi").versionAsInProject());

        String currentDir = Paths.get("").toAbsolutePath().toString();
        Path carbonHome = Paths.get(currentDir, "target", "carbon-home");

        CarbonSysPropConfiguration sysPropConfiguration = new CarbonSysPropConfiguration();
        sysPropConfiguration.setCarbonHome(carbonHome.toString());
        sysPropConfiguration.setServerKey("carbon-jndi");
        sysPropConfiguration.setServerName("WSO2 Carbon JNDI Server");
        sysPropConfiguration.setServerVersion("1.0.0");

        optionList = OSGiTestConfigurationUtils.getConfiguration(optionList, sysPropConfiguration);

        return optionList.toArray(new Option[optionList.size()]);
    }

    @Test
    public void testJNDITraditionalClient() throws NamingException {
        InitialContext initialContext = new InitialContext();
        initialContext.createSubcontext("java:comp");
        initialContext.bind("java:comp/name", "sameera");

        InitialContext context = new InitialContext();
        String name = (String) context.lookup("java:comp/name");
        assertEquals(name, "sameera", "Value not found in JNDI");

        NamingEnumeration namingEnumeration = context.list("java:comp");
        namingEnumeration.hasMore();
        namingEnumeration.next();

        namingEnumeration = context.listBindings("java:comp");
        namingEnumeration.hasMore();
        namingEnumeration.next();

        context.rebind("java:comp/name", "jayasoma");
        name = (String) context.lookup("java:comp/name");
        assertEquals(name, "jayasoma", "Value not found in JNDI");

        context.rename("java:comp", "java:comp1");
        name = (String) context.lookup("java:comp1/name");
        assertEquals(name, "jayasoma", "Value not found in JNDI");

        context.rename("java:comp1", "java:comp");
    }

    @Test(dependsOnMethods = "testJNDITraditionalClient")
    public void testJNDIContextManagerService() throws NamingException {
        String name = null;

        Context initialContext = jndiContextManager.newInitialContext();

        initialContext.createSubcontext("java:comp/env2");
        initialContext.bind("java:comp/env2/name", "jayasoma");

        Context context = jndiContextManager.newInitialContext();

        name = (String) context.lookup("java:comp/env2/name");

        assertEquals(name, "jayasoma", "Value not found in JNDI");
    }

    /**
     * This method test the expected exception if a lookup() request is made for a name which is not bind to
     * any resource.
     */
    @Test(dependsOnMethods = "testJNDIContextManagerService", expectedExceptions = {NameNotFoundException.class})
    public void testJNDIContextManagerWithUnregisteredName() throws NamingException {

        Context context = jndiContextManager.newInitialContext();

        context.lookup("java:comp/env2/new-name");

    }

    /**
     * This method test the code which retrieve the caller's BundleContext instance from the
     * osgi.service.jndi.bundleContext environment variable defined in the OSGi JNDI Specification.
     */
    @Test(dependsOnMethods = "testJNDIContextManagerService")
    public void testJNDITraditionalClientWithEnvironmentBC() throws NamingException {

        // Getting the BundleContext of the org.wso2.carbon.jndi bundle.
        BundleContext carbonJNDIBundleContext = Arrays.asList(bundleContext.getBundles())
                .stream()
                .filter(bundle -> "org.wso2.carbon.jndi".equals(bundle.getSymbolicName()))
                .map(Bundle::getBundleContext)
                .findAny()
                .get();

        // This is used to get the caller's bundle context object.
        BundleContextICFServiceFactory bundleContextICFServiceFactory = new BundleContextICFServiceFactory();

        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 10);
        ServiceRegistration serviceRegistration = bundleContext.registerService(InitialContextFactory.class.getName(),
                bundleContextICFServiceFactory, propertyMap);

        //Setting carbonJNDIBundleContext as the value of osgi.service.jndi.bundleContext property.
        Hashtable<String, Object> environment = new Hashtable<>(1);
        environment.put("osgi.service.jndi.bundleContext", carbonJNDIBundleContext);

        InitialContext initialContext = new InitialContext(environment);
        initialContext.createSubcontext("java:comp/bundleContext");

        assertEquals(bundleContextICFServiceFactory.getFirstConsumersBundleContext().getBundle().getSymbolicName(),
                carbonJNDIBundleContext.getBundle().getSymbolicName(), "Value of the osgi.service.jndi.bundleContext " +
                        "environment variable has not been picked up");

        serviceRegistration.unregister();
    }

    /**
     * This method test the code which retrieve the caller's BundleContext instance from the
     * Thread Context ClassLoader.
     */
    @Test(dependsOnMethods = "testJNDITraditionalClientWithEnvironmentBC")
    public void testJNDITraditionalClientWithTCCL() throws NamingException {
        DummyBundleClassLoader dummyBundleClassLoader = new DummyBundleClassLoader(this.getClass().getClassLoader(),
                bundleContext.getBundle());

        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 10);
        ServiceRegistration serviceRegistration = bundleContext.registerService(InitialContextFactory.class.getName(),
                new FooInitialContextFactory(), propertyMap);

        ClassLoader currentTCCL = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(dummyBundleClassLoader);

            InitialContext initialContext = new InitialContext();
            initialContext.createSubcontext("java:comp/tccl");
        } finally {
            Thread.currentThread().setContextClassLoader(currentTCCL);
        }

        assertEquals(true, dummyBundleClassLoader.isGetBundleMethodInvoked(), "TCCL has not used to get the " +
                "caller's Bundle");

        serviceRegistration.unregister();
    }

    /**
     * In this method we are trying to create an InitialContext from a non-existent InitialContextFactory called
     * FooInitialContextFactory. This FooInitialContextFactory is specified as an environment variable.
     */
    @Test(dependsOnMethods = "testJNDITraditionalClientWithTCCL", expectedExceptions = {NamingException.class},
            expectedExceptionsMessageRegExp = "Cannot find the InitialContextFactory " +
                    "org.wso2.carbon.jndi.osgi.factories.FooInitialContextFactory.")
    public void testJNDIContextManagerWithEnvironmentContextFactoryException() throws NamingException {
        Map<String, String> environment = new HashMap<String, String>(1);
        environment.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                FooInitialContextFactory.class.getName());

        jndiContextManager.newInitialContext(environment);
    }

    /**
     * In this method we are trying to create an InitialContext from the FooInitialContextFactory by setting the
     * java.naming.factory.initial environment variable.
     */
    @Test(dependsOnMethods = "testJNDIContextManagerWithEnvironmentContextFactoryException")
    public void testJNDIContextManagerWithEnvironmentContextFactory() throws NamingException {

        ServiceRegistration serviceRegistration = bundleContext.registerService(
                new String[]{InitialContextFactory.class.getName(), FooInitialContextFactory.class.getName()},
                new FooInitialContextFactory(), null);

        Map<String, String> environment = new HashMap<String, String>(1);
        environment.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                FooInitialContextFactory.class.getName());

        Context initialContext = jndiContextManager.newInitialContext(environment);

        initialContext.bind("contextFactoryClass", "org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory");
        String contextFactoryClass = (String) initialContext.lookup("contextFactoryClass");
        assertEquals(contextFactoryClass, FooInitialContextFactory.class.getName(), "Specified InitialContextFactory " +
                "has not been picked up to create the requested initial context.");

        // Unregistering the FooInitialContextFactory service.
        serviceRegistration.unregister();

    }

    /**
     * In this method we are testing the functionality of the JNDIContextManager when there are multiple
     * InitialContextFactory services with different service.ranking property value.
     * <p>
     * We also test the situation with an InitialContextFactory which returns a null context.
     */
    @Test(dependsOnMethods = "testJNDIContextManagerWithEnvironmentContextFactory")
    public void testCustomInitialContextFactories() throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 10);
        ServiceRegistration<InitialContextFactory> fooICFServiceRef = bundleContext.registerService(
                InitialContextFactory.class, new FooInitialContextFactory(), propertyMap);
        Context initialContext = jndiContextManager.newInitialContext();

        // Here we expect returned Context to be an instance of the TestContext. Following bind operation is ignored
        // in the TestContext class. It is hard coded to return the created InitialContextFactory if you invoke the
        // TestContext.lookup("contextFactoryClass"). In this case it should be the FooInitialContextFactory
        initialContext.bind("contextFactoryClass", "org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory");
        String contextFactoryClass = (String) initialContext.lookup("contextFactoryClass");

        assertEquals(contextFactoryClass, FooInitialContextFactory.class.getName(), "Specified InitialContextFactory " +
                "has not been picked up to create the requested initial context.");

        // Now we are registering another InitialContextFactory with a higher service.ranking value.
        propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 20);
        ServiceRegistration<InitialContextFactory> barICFServiceRef = bundleContext.registerService(
                InitialContextFactory.class, new BarInitialContextFactory(), propertyMap);

        initialContext = jndiContextManager.newInitialContext();
        initialContext.bind("contextFactoryClass", "org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory");
        contextFactoryClass = (String) initialContext.lookup("contextFactoryClass");

        assertEquals(contextFactoryClass, BarInitialContextFactory.class.getName(), "Specified InitialContextFactory " +
                "has not been picked up to create the requested initial context.");


        // To test null from getInitialContext methods.
        propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 30);
        ServiceRegistration<InitialContextFactory> nullICFServiceRef = bundleContext.registerService(
                InitialContextFactory.class, new NullInitialContextFactory(), propertyMap);

        initialContext = jndiContextManager.newInitialContext();
        initialContext.bind("contextFactoryClass", "org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory");
        contextFactoryClass = (String) initialContext.lookup("contextFactoryClass");

        assertEquals(contextFactoryClass, BarInitialContextFactory.class.getName(), "Specified InitialContextFactory " +
                "has not been picked up to create the requested initial context.");

        // Unregistering all the registered ICF services.
        fooICFServiceRef.unregister();
        barICFServiceRef.unregister();
        nullICFServiceRef.unregister();
    }

    /**
     * In this method we are testing the functionality of the JNDIContextManager when there exists
     * an InitialContextFactory service which throws a NamingException.
     */
    @Test(dependsOnMethods = "testCustomInitialContextFactories", expectedExceptions = {NamingException.class},
            expectedExceptionsMessageRegExp = "InitialContext cannot be created due to a network failure.")
    public void testCustomInitialContextFactoryWithException() throws NamingException {
        // To test null from getInitialContext methods.
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 40);
        ServiceRegistration<InitialContextFactory> exceptionFactorySR = bundleContext.registerService(
                InitialContextFactory.class, new ExceptionInitialContextFactory(), propertyMap);

        try {
            Context initialContext = jndiContextManager.newInitialContext();
        } finally {
            // Unregistering the InitialContextFactory which throws an exception.
            exceptionFactorySR.unregister();
        }
    }

    /**
     * In this method we are trying to create an InitialContext from a non-existent InitialContextFactory called
     * FooInitialContextFactory. But we are registering an InitialContextFactoryBuilder service before that.
     * Therefore this InitialContextFactoryBuilder service should create an InitialContext according the OSGi JNDI
     * specification.
     */
    @Test(dependsOnMethods = "testCustomInitialContextFactoryWithException")
    public void testJNDIContextManagerWithEnvironmentContextFactoryBuilder() throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 10);
        ServiceRegistration<InitialContextFactoryBuilder> abcICFBServiceRef = bundleContext.registerService(
                InitialContextFactoryBuilder.class, new ABCContextFactoryBuilder(), propertyMap);

        Map<String, String> environment = new HashMap<String, String>(1);
        environment.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY,
                FooInitialContextFactory.class.getName());

        Context initialContext = jndiContextManager.newInitialContext(environment);

        initialContext.bind("contextFactoryBuilderClass", "EMPTY");
        String contextFactoryBuilderClass = (String) initialContext.lookup("contextFactoryBuilderClass");

        assertEquals(contextFactoryBuilderClass, ABCContextFactoryBuilder.class.getName(),
                "Specified InitialContextFactory has not been picked up to create the requested initial context.");

        abcICFBServiceRef.unregister();
    }

    /**
     * In this method we are testing the functionality of the JNDIContextManager when there are multiple
     * InitialContextFactoryBuilder services with different service.ranking property value.
     * <p>
     * We also test the situation with an InitialContextFactoryBuilder which returns a null context.
     */
    @Test(dependsOnMethods = "testJNDIContextManagerWithEnvironmentContextFactoryBuilder")
    public void testCustomInitialContextFactoryBuilders() throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 10);
        ServiceRegistration<InitialContextFactoryBuilder> abcICFBServiceRef = bundleContext.registerService(
                InitialContextFactoryBuilder.class, new ABCContextFactoryBuilder(), propertyMap);
        Context initialContext = jndiContextManager.newInitialContext();

        initialContext.bind("contextFactoryBuilderClass", "EMPTY");
        String contextFactoryBuilderClass = (String) initialContext.lookup("contextFactoryBuilderClass");

        assertEquals(contextFactoryBuilderClass, ABCContextFactoryBuilder.class.getName(),
                "Specified InitialContextFactory has not been picked up to create the requested initial context.");

        propertyMap.put("service.ranking", 30);
        ServiceRegistration<InitialContextFactoryBuilder> nullICFBServiceRef = bundleContext.registerService(
                InitialContextFactoryBuilder.class, new NullContextFactoryBuilder(), propertyMap);
        initialContext = jndiContextManager.newInitialContext();

        initialContext.bind("contextFactoryBuilderClass", "EMPTY");
        contextFactoryBuilderClass = (String) initialContext.lookup("contextFactoryBuilderClass");

        assertEquals(contextFactoryBuilderClass, ABCContextFactoryBuilder.class.getName(),
                "Specified InitialContextFactory has not been picked up to create the requested initial context.");

        propertyMap.put("service.ranking", 20);
        ServiceRegistration<InitialContextFactoryBuilder> xyzICFBServiceRef = bundleContext.registerService(
                InitialContextFactoryBuilder.class, new XYZContextFactoryBuilder(), propertyMap);
        initialContext = jndiContextManager.newInitialContext();

        initialContext.bind("contextFactoryBuilderClass", "EMPTY");
        contextFactoryBuilderClass = (String) initialContext.lookup("contextFactoryBuilderClass");

        assertEquals(contextFactoryBuilderClass, XYZContextFactoryBuilder.class.getName(),
                "Specified InitialContextFactory has not been picked up to create the requested initial context.");

        abcICFBServiceRef.unregister();
        nullICFBServiceRef.unregister();
        xyzICFBServiceRef.unregister();
    }

    /**
     * In this test we are trying do a osgi:service lookup.
     * We register a service in the bundle context and do a lookup.
     */
    @Test(dependsOnMethods = "testJNDIContextManagerWithEnvironmentContextFactoryBuilder")
    public void testOSGIUrlWithServicePath() throws NamingException {
        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration =
                bundleContext.registerService(FooService.class, fooService, null);
        Context context = jndiContextManager.newInitialContext();

        //url scheme: osgi:service/<interface>
        FooServiceImpl1 service =
                (FooServiceImpl1) context.lookup("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");

        assertNotNull(service, "Specified interface does not registered with bundle context");
        fooServiceRegistration.unregister();
    }

    /**
     * In this test we are trying do a osgi:service lookup.
     * We register two services in the bundle context with different service rankings and perform a lookup.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithServicePath")
    public void testOSGIUrlWithServiceRanking() throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("service.ranking", 10);
        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);
        Context context = jndiContextManager.newInitialContext();

        Object service = context.lookup("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");

        assertTrue((service instanceof FooServiceImpl1), "Specified interface does not registered with bundle context");

        propertyMap.put("service.ranking", 20);
        FooService fooService2 = new FooServiceImpl2();
        ServiceRegistration<FooService> fooService2Registration = bundleContext.registerService(
                FooService.class, fooService2, propertyMap);
        context = jndiContextManager.newInitialContext();

        service = context.lookup("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");

        assertTrue(service instanceof FooServiceImpl2, "Specified service does not returned as per ranking order" +
                " OR Specified interface does not registered with bundle context");
        fooServiceRegistration.unregister();
        fooService2Registration.unregister();
    }

    /**
     * In this test we are trying do a osgi:service lookup setting the
     * osgi.jndi.service.name.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithServiceRanking")
    public void testOSGIUrlWithJNDIServiceName() throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("osgi.jndi.service.name", "foo/myService");

        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);
        Context context = jndiContextManager.newInitialContext();

        Object service = context.lookup("osgi:service/foo/myService");

        assertTrue(service instanceof FooServiceImpl1, "Specified service does not returned" +
                " OR Specified interface does not registered with bundle context");

        service = context.lookup("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService/" +
                "(osgi.jndi.service.name=foo/myService)");

        assertTrue(service instanceof FooServiceImpl1, "Specified service does not returned" +
                " OR Specified interface does not registered with bundle context");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we are trying do a osgi:service lookup with an unregistered service.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithJNDIServiceName", expectedExceptions = {NameNotFoundException.class})
    public void testOSGIUrlWithoutJNDIServiceName() throws NamingException {
        Context context = jndiContextManager.newInitialContext();
        context.lookup("osgi:service/foo/myService");
    }

    /**
     * In this test we are trying do a osgi:service lookup setting the
     * osgi.jndi.service.name.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithoutJNDIServiceName")
    public void testOSGIUrlWithJNDIServiceName2() throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("osgi.jndi.service.name", "foo");

        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);
        Context context = jndiContextManager.newInitialContext();

        Object service = context.lookup("osgi:service/foo");

        assertTrue(service instanceof FooServiceImpl1, "Specified service does not returned" +
                " OR Specified interface does not registered with bundle context");

        service = context.lookup("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService/" +
                "(osgi.jndi.service.name=foo)");

        assertTrue(service instanceof FooServiceImpl1, "Specified service does not returned" +
                " OR Specified interface does not registered with bundle context");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we are trying do a osgi:service lookup setting the
     * osgi.jndi.service.name.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithJNDIServiceName2")
    public void testOSGIUrlWithJNDIServiceName3() throws NamingException {
        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("osgi.jndi.service.name", "foo/bar/myService");

        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);
        Context context = jndiContextManager.newInitialContext();

        Object service = context.lookup("osgi:service/foo/bar/myService");

        assertTrue(service instanceof FooServiceImpl1, "Specified service does not returned" +
                " OR Specified interface does not registered with bundle context");

        service = context.lookup("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService/" +
                "(osgi.jndi.service.name=foo/bar/myService)");

        assertTrue(service instanceof FooServiceImpl1, "Specified service does not returned" +
                " OR Specified interface does not registered with bundle context");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we are trying do a osgi:service lookup for a service that is not registered
     */
    @Test(dependsOnMethods = "testOSGIUrlWithJNDIServiceName3", expectedExceptions = {NameNotFoundException.class})
    public void testOSGIUrlWithUnregisteredService() throws NamingException {
        Context context = jndiContextManager.newInitialContext();
        context.lookup("osgi:service/foo");
    }

    /**
     * In this test we are trying do a osgi:service lookup for a service with an invalid filter
     */
    @Test(dependsOnMethods = "testOSGIUrlWithUnregisteredService", expectedExceptions = {NamingException.class})
    public void testOSGIUrlListWithUnregisteredService() throws NamingException {
        Context context = jndiContextManager.newInitialContext();
        context.list("osgi:service/(org.wso2.carbon.jndi.osgi.services.FooService");
    }

    /**
     * In this test we are trying do a osgi:service list() and test the NamingEnumeration object returned.
     */
    @Test(dependsOnMethods = "testOSGIUrlListWithUnregisteredService")
    public void testOSGIUrlWithServiceList() throws NamingException {

        FooService fooService1 = new FooServiceImpl1();
        FooService fooService2 = new FooServiceImpl2();
        ServiceRegistration<FooService> fooService1Registration = bundleContext.registerService(
                FooService.class, fooService1, null);
        ServiceRegistration<FooService> fooService2Registration = bundleContext.registerService(
                FooService.class, fooService2, null);

        Context context = jndiContextManager.newInitialContext();

        NamingEnumeration<NameClassPair> namingEnumeration =
                context.list("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");

        assertTrue(namingEnumeration.hasMoreElements());

        NameClassPair nameClassPair = namingEnumeration.nextElement();

        assertNotNull(nameClassPair, "No NameClassPair returned fo service :" + FooService.class);

        assertEquals(nameClassPair.getName(),
                String.valueOf(fooService1Registration.getReference().getProperty(Constants.SERVICE_ID)));

        assertTrue(namingEnumeration.hasMoreElements());
        fooService1Registration.unregister();
        fooService2Registration.unregister();
    }

    /**
     * In this test we are trying do a osgi:service list() and test the NamingEnumeration object returned.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithServiceList", expectedExceptions = {NameNotFoundException.class})
    public void testOSGIUrlListWithInvalidService() throws NamingException {

        Context context = jndiContextManager.newInitialContext();
        context.list("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");

    }

    /**
     * In this test we are trying do a osgi:service listBindings() and test the NamingEnumeration object returned.
     */
    @Test(dependsOnMethods = "testOSGIUrlListWithInvalidService")
    public void testOSGIUrlWithServiceListBindings() throws NamingException {

        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, null);

        FooService fooService2 = new FooServiceImpl2();
        ServiceRegistration<FooService> fooServiceRegistration2 = bundleContext.registerService(
                FooService.class, fooService2, null);

        Context context = jndiContextManager.newInitialContext();

        NamingEnumeration<Binding> listBindings =
                context.listBindings("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");

        assertTrue(listBindings.hasMoreElements());

        Binding binding = listBindings.nextElement();
        assertEquals(binding.getName(),
                String.valueOf(fooServiceRegistration.getReference().getProperty(Constants.SERVICE_ID)));

        assertNotNull(binding.getObject(),
                "No Binding object returned fo service :" + FooService.class);
        assertTrue(listBindings.hasMoreElements());   //we registered two services.

        listBindings.close();
        fooServiceRegistration.unregister();
        fooServiceRegistration2.unregister();
    }

    /**
     * In this test we are trying do a osgi:service listBindings() and test the NamingEnumeration object returned.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithServiceListBindings")
    public void testOSGIUrlWithJNDINameAndListBindings() throws NamingException {

        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put("osgi.jndi.service.name", "foo/myService");

        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);

        FooService fooService2 = new FooServiceImpl2();
        ServiceRegistration<FooService> fooServiceRegistration2 = bundleContext.registerService(
                FooService.class, fooService2, propertyMap);

        Context context = jndiContextManager.newInitialContext();

        NamingEnumeration<Binding> listBindings =
                context.listBindings("osgi:service/foo/myService");

        assertTrue(listBindings.hasMoreElements());

        Binding binding = listBindings.nextElement();
        assertEquals(binding.getName(),
                String.valueOf(fooServiceRegistration.getReference().getProperty(Constants.SERVICE_ID)));

        assertNotNull(binding.getObject(),
                "No Binding object returned fo service :" + FooService.class);
        assertTrue(listBindings.hasMoreElements());   //we registered two services.

        listBindings.close();
        fooServiceRegistration.unregister();
        fooServiceRegistration2.unregister();
    }

    /**
     * In this test we are trying to obtain the Bundle Context of the owning bundle with osgi:framework/bundleContext.
     */
    @Test(dependsOnMethods = "testOSGIUrlWithServiceListBindings")
    public void testOSGIUrlToGetOwningBundleContext() throws NamingException {

        FooService fooService = new FooServiceImpl1();
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, null);

        Context context = jndiContextManager.newInitialContext();

        BundleContext owningBundleContext = (BundleContext) context.lookup("osgi:framework/bundleContext");

        assertNotNull(owningBundleContext);

        ServiceReference serviceReference =
                owningBundleContext.getServiceReference("org.wso2.carbon.jndi.osgi.services.FooService");

        Object service = owningBundleContext.getService(serviceReference);
        assertTrue(service instanceof FooServiceImpl1, "BundleContext returned does not have the " +
                "expected service registered");
        fooServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object when
     * setting the factory class name.
     */
    @Test(dependsOnMethods = "testOSGIUrlToGetOwningBundleContext")
    public void testJNDIProviderWithFactoryClassName() throws Exception {

        FooObjectFactory fooObjectFactory = new FooObjectFactory();
        ServiceRegistration<?> fooServiceRegistration = bundleContext.registerService(
                new String[]{ObjectFactory.class.getName(), FooObjectFactory.class.getName()}
                , fooObjectFactory, null);

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference("class.name", FooObjectFactory.class.getName(), "");

        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap);

        assertEquals(convertedObject, "Test Object", "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object without
     * setting the factory class and adding a StringRefAddr
     */
    @Test(dependsOnMethods = "testOSGIUrlToGetOwningBundleContext")
    public void testJNDIProviderAdminWithStringRefAddr() throws Exception {

        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put(JNDIConstants.JNDI_URLSCHEME, "javax");

        FooObjectFactory fooObjectFactory = new FooObjectFactory();
        ServiceRegistration<ObjectFactory> fooServiceRegistration = bundleContext.registerService(
                ObjectFactory.class, fooObjectFactory, null);

        JavaxObjectFactory javaxObjectFactory = new JavaxObjectFactory();
        ServiceRegistration<ObjectFactory> javaxServiceRegistration = bundleContext.registerService(
                ObjectFactory.class, javaxObjectFactory, propertyMap);

        Reference ref = new Reference(null);
        ref.add(new StringRefAddr("URL", "javax"));

        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, new HashMap<>());

        assertEquals(convertedObject, "JAVAX Object", "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
        javaxServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object when
     * setting both the factory class name and StringRefAddress.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithStringRefAddr")
    public void testJNDIProviderAdminWithStringAddrAndFactoryClass() throws Exception {

        FooObjectFactory fooObjectFactory = new FooObjectFactory();
        ServiceRegistration<?> fooServiceRegistration = bundleContext.registerService(
                new String[]{ObjectFactory.class.getName(),
                        fooObjectFactory.getClass().getName()}
                , fooObjectFactory, null);

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference("class.name", FooObjectFactory.class.getName(), "");
        ref.add(new StringRefAddr("URL", "foo"));

        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap);

        assertEquals(convertedObject, "Test Object", "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object without
     * setting both the factory class name and StringRefAddress.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithStringAddrAndFactoryClass")
    public void testJNDIProviderAdminWithoutStringAddrAndFactoryClass() throws Exception {

        FooObjectFactory fooObjectFactory = new FooObjectFactory();
        ServiceRegistration<?> fooServiceRegistration = bundleContext.registerService(
                new String[]{ObjectFactory.class.getName(),
                        fooObjectFactory.getClass().getName()}
                , fooObjectFactory, null);

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference(FooObjectFactory.class.getName());

        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap);

        //JndiProviderAdmin will attempt to convert the object with each Object Factory service
        //(or Dir Object Factory service for directories) service in ranking order until a non - null value
        //is returned
        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a
     * unregistered type of reference
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithoutStringAddrAndFactoryClass")
    public void testJNDIProviderAdminWithUnregisteredFactoryClass() throws Exception {

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference(FooObjectFactory.class.getName());

        //if the reference ObjectFactory is unregistered, a result form a getObjectInstance() method of  a
        // registered ObjectFactory will be returned
        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap);

        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object when
     * setting both the factory class name and StringRefAddress.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithUnregisteredFactoryClass")
    public void testJNDIProviderAdminWithUnregisteredFactoryClassName() throws Exception {

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference("class.name", FooObjectFactory.class.getName(), "");
        ref.add(new StringRefAddr("URL", "foo"));
        //if the reference ObjectFactory is unregistered, a result form a getObjectInstance() method of  a
        //registered ObjectFactory will be returned

        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap);

        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a non referentiable object
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithUnregisteredFactoryClassName")
    public void testJNDIProviderAdminWithNonReferentialObject() throws Exception {

        Map<String, Object> propertyMap = new HashMap<>();
        Object object = new Object();
        //if the object is a non-Referenceable, iterate over the Object Factory Builder services in ranking order
        //and will call getObjectInstance() of such not null ObjectFactory

        Object convertedObject = jndiProviderAdmin.getObjectInstance(object, null, null, propertyMap);

        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object without
     * setting both the factory class name and StringRefAddress.
     * We have only registered a DirObjectFactory class and call getObjectInstance without passing attributes.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithNonReferentialObject")
    public void testJNDIProviderAdminDirObjectFactoryWithoutAttributes() throws Exception {

        FooDirObjectFactory fooDirObjectFactory = new FooDirObjectFactory();
        ServiceRegistration<?> fooServiceRegistration = bundleContext.registerService(
                new String[]{DirObjectFactory.class.getName(),
                        fooDirObjectFactory.getClass().getName()}
                , fooDirObjectFactory, null);

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference(FooDirObjectFactory.class.getName());

        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap);

        //JndiProviderAdmin will attempt to convert the object with each Object Factory service
        //(or Dir Object Factory service for directories) service in ranking order until a non - null value
        //is returned
        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object without
     * setting both the factory class name and StringRefAddress.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminDirObjectFactoryWithoutAttributes")
    public void testJNDIProviderAdminDirObjectFactoryWithoutStringAddrAndFactoryClass() throws Exception {

        FooDirObjectFactory fooDirObjectFactory = new FooDirObjectFactory();
        ServiceRegistration<?> fooServiceRegistration = bundleContext.registerService(
                new String[]{DirObjectFactory.class.getName(),
                        fooDirObjectFactory.getClass().getName()}
                , fooDirObjectFactory, null);

        Map<String, Object> propertyMap = new HashMap<>();
        Attributes attributes = new BasicAttributes();
        attributes.put(new BasicAttribute("test", "Basic-Attribute"));
        Reference ref = new Reference(FooDirObjectFactory.class.getName());

        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap, attributes);

        //JndiProviderAdmin will attempt to convert the object with each Object Factory service
        //(or Dir Object Factory service for directories) service in ranking order until a non - null value
        //is returned
        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a
     * unregistered type of reference with DirObjectFactory.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminDirObjectFactoryWithoutStringAddrAndFactoryClass")
    public void testJNDIProviderAdminWithUnregisteredDirFactoryClass() throws Exception {

        Map<String, Object> propertyMap = new HashMap<>();
        Attributes attributes = new BasicAttributes();
        Reference ref = new Reference(FooDirObjectFactory.class.getName());

        //if the reference ObjectFactory is unregistered, a result form a getObjectInstance() method of  a
        // registered ObjectFactory will be returned
        Object convertedObject = jndiProviderAdmin.getObjectInstance(ref, null, null, propertyMap, attributes);

        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object when
     * setting the factory class name to a DirObjectFactory.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithUnregisteredDirFactoryClass")
    public void testJNDIProviderWithDirFactoryClassName() throws Exception {

        FooDirObjectFactory fooDirObjectFactory = new FooDirObjectFactory();
        ServiceRegistration<?> fooServiceRegistration = bundleContext.registerService(
                new String[]{DirObjectFactory.class.getName(), FooDirObjectFactory.class.getName()}
                , fooDirObjectFactory, null);

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference("class.name", FooDirObjectFactory.class.getName(), "");

        Object convertedObject = jndiProviderAdmin.
                getObjectInstance(ref, null, null, propertyMap, new BasicAttributes());

        assertEquals(convertedObject, "Test Dir Object", "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object without
     * setting the factory class and adding a StringRefAddr.
     */
    @Test(dependsOnMethods = "testJNDIProviderWithDirFactoryClassName")
    public void testJNDIProviderAdminWithStringRefAddrAndDirObject() throws Exception {

        Dictionary<String, Object> propertyMap = new Hashtable<>();
        propertyMap.put(JNDIConstants.JNDI_URLSCHEME, "javax");

        FooDirObjectFactory fooObjectFactory = new FooDirObjectFactory();
        ServiceRegistration<DirObjectFactory> fooServiceRegistration = bundleContext.registerService(
                DirObjectFactory.class, fooObjectFactory, null);

        JavaxDirObjectFactory javaxObjectFactory = new JavaxDirObjectFactory();
        ServiceRegistration<DirObjectFactory> javaxServiceRegistration = bundleContext.registerService(
                DirObjectFactory.class, javaxObjectFactory, propertyMap);

        Reference ref = new Reference(null);
        ref.add(new StringRefAddr("URL", "javax"));

        Object convertedObject = jndiProviderAdmin.
                getObjectInstance(ref, null, null, new HashMap<>(), new BasicAttributes());

        assertEquals(convertedObject, "JAVAX Dir Object", "JNDIProviderAdmin could not convert the reference object");

        fooServiceRegistration.unregister();
        javaxServiceRegistration.unregister();
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a reference object when
     * setting both the DirObjectFactory class name and StringRefAddress.
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithStringRefAddrAndDirObject")
    public void testJNDIProviderAdminWithUnregisteredDirFactoryClassName() throws Exception {

        Map<String, Object> propertyMap = new HashMap<>();
        Reference ref = new Reference("class.name", FooDirObjectFactory.class.getName(), "");
        ref.add(new StringRefAddr("URL", "foo"));
        //if the reference ObjectFactory is unregistered, a result form a getObjectInstance() method of  a
        //registered ObjectFactory will be returned

        Object convertedObject = jndiProviderAdmin.
                getObjectInstance(ref, null, null, propertyMap, new BasicAttributes());

        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");
    }

    /**
     * In this test we check the object conversion service of JNDI provider admin passing a non referentiable object
     */
    @Test(dependsOnMethods = "testJNDIProviderAdminWithUnregisteredDirFactoryClassName")
    public void testJNDIProviderAdminWithNonReferentialObjectWithAttributes() throws Exception {

        Map<String, Object> propertyMap = new HashMap<>();
        Object object = new Object();
        //if the object is a non-Referenceable, iterate over the Object Factory Builder services in ranking order
        //and will call getObjectInstance() of such not null ObjectFactory

        Object convertedObject = jndiProviderAdmin.
                getObjectInstance(object, null, null, propertyMap, new BasicAttributes());

        assertNotNull(convertedObject, "JNDIProviderAdmin could not convert the reference object");
    }

    /*
     * In this test we are trying to do a lookup with an invalid url.
     */
    @Test(dependsOnMethods = "testOSGIUrlToGetOwningBundleContext", expectedExceptions = {InvalidNameException.class},
            expectedExceptionsMessageRegExp = "Invalid OSGi URL scheme : osgi:services/")
    public void testInvalidOSGIUrlListContextLookup() throws NamingException {

        Context context = jndiContextManager.newInitialContext();

        context.lookup("osgi:services/"); //services is an invalid sub-context
    }

    /**
     * In this test we are trying to do a lookup with an invalid url.
     */
    @Test(dependsOnMethods = "testInvalidOSGIUrlListContextLookup", expectedExceptions = {NamingException.class})
    public void testUnregisteredOSGIUrlWithListBindings() throws NamingException {

        Context context = jndiContextManager.newInitialContext();

        context.listBindings("osgi:service/foo"); //services is an invalid sub-context
    }

    /**
     * In this test we are trying to do a lookup with an unsupported url eg: osgi:servicelist.
     */
    @Test(dependsOnMethods = "testInvalidOSGIUrlListContextLookup",
            expectedExceptions = {OperationNotSupportedException.class},
            expectedExceptionsMessageRegExp = "Unsupported operation with URL : osgi:servicelist/")
    public void testUnsupportedOSGIUrlLookup() throws NamingException {

        Context context = jndiContextManager.newInitialContext();

        context.lookup("osgi:servicelist/"); //services is an invalid sub-context
    }
}
