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
package org.wso2.carbon.jndi.internal.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIConstants;
import org.osgi.service.jndi.JNDIProviderAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;
import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.wso2.carbon.jndi.internal.util.JNDIUtils.getServiceReferences;
import static org.wso2.carbon.jndi.internal.util.JNDIUtils.getService;
import static org.wso2.carbon.jndi.internal.util.LambdaExceptionUtils.rethrowFunction;

/**
 * Implements JNDIProviderAdmin interface.
 */
public class JNDIProviderAdminImpl implements JNDIProviderAdmin {
    private static final Logger logger = LoggerFactory.getLogger(JNDIProviderAdminImpl.class);

    private BundleContext bundleContext;
    private static final String OBJECT_CLASS = "objectClass";
    public final static String ADDRESS_TYPE = "URL";

    public JNDIProviderAdminImpl(BundleContext bundleContext, ServiceRegistration serviceRegistration) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Object getObjectInstance(Object refInfo, Name name, Context context, Map<?, ?> environment) throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.putAll(environment);
        Object result = null;

        //1) If the description object is an instance of Referenceable , then get the corresponding Reference object
        Object referenceObject = getReferenceObject(refInfo);
        if (referenceObject instanceof Reference) {
            Reference reference = (Reference) referenceObject;

            String factoryClassName = reference.getFactoryClassName();
            // 3) If a factory class name is specified, use Bundle Context to search for a service registered under
            // the Reference's factory class name.
            if (factoryClassName != null && !"".equals(factoryClassName)) {
                ServiceReference[] factorySRefCollection = bundleContext.getServiceReferences(factoryClassName, null);
                Iterator<ServiceReference> referenceIterator = (Iterator<ServiceReference>) Arrays.asList(factorySRefCollection);
                ObjectFactory factory;
                while (referenceIterator.hasNext()) {
                    ServiceReference serviceReference = referenceIterator.next();
                    if (serviceReference != null) {
                        factory = (ObjectFactory) bundleContext.getService(serviceReference);
                        result = factory.getObjectInstance(reference, name, context, env);
                    }
                }

            } else {
                // 3) If no factory class name is specified, iterate over all the Reference object's StringRefAddrs objects
                // with the address type of URL. For each matching address type, use the value to find a matching
                //URL Context, see URL Context Provider on page 507, and use it to recreate the object. See the
                //Naming Manager for details. If an object is created then it is returned and the algorithm stops
                //here.
                Enumeration<RefAddr> refAddrEnumeration = reference.getAll();
                while (refAddrEnumeration.hasMoreElements()) {
                    RefAddr refAddr = refAddrEnumeration.nextElement();
                    if (refAddr instanceof StringRefAddr && refAddr.getType().equalsIgnoreCase(ADDRESS_TYPE)) {
                        String urlScheme = getUrlScheme((String) refAddr.getContent());
                        Collection<ServiceReference<ObjectFactory>> factorySRefCollection =
                                getServiceReferences(bundleContext, ObjectFactory.class, null);
                        Iterator<ServiceReference<ObjectFactory>> referenceIterator = factorySRefCollection.iterator();
                        while (referenceIterator.hasNext()) {
                            ServiceReference serviceReference = referenceIterator.next();
                            if (serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME).equals(urlScheme)) {
                                ObjectFactory factory = (ObjectFactory) bundleContext.getService(serviceReference);
                                result = factory.getObjectInstance(reference, name, context, env);    //todo does the parameters be null? page 507
                            }
                        }
                    }
                }
                if (result == null) {
                    Collection<ServiceReference<ObjectFactory>> factorySRefCollection =
                            getServiceReferences(bundleContext, ObjectFactory.class, null);
                    Iterator<ServiceReference<ObjectFactory>> referenceIterator = factorySRefCollection.iterator();
                    ObjectFactory factory;
                    while (referenceIterator.hasNext()) {
                        ServiceReference serviceReference = referenceIterator.next();
                        if (serviceReference != null) {
                            factory = (ObjectFactory) bundleContext.getService(serviceReference);
                            result = factory.getObjectInstance(reference, name, context, env);
                        }
                    }
                }
            }
            if (result == null) {
                result = reference;
            }
        } else {
            // 2.) If the description object is not a Reference object.
            // Iterate over the Object Factory Builder services in ranking order. Attempt to use each such service
            //to create an ObjectFactory or DirObjectFactory instance.
            Collection<ServiceReference<ObjectFactoryBuilder>> objectFactoryBuilderRef =
                    getServiceReferences(bundleContext, ObjectFactoryBuilder.class, getServiceFilter(ObjectFactoryBuilder.class.getName())); //todo cannot apply getServiceFilter
            Optional<ObjectFactory> factory = getObjectFactoryBuilder(referenceObject, objectFactoryBuilderRef, env);
            if (factory.isPresent()) {
                // 3.)If this succeeds (non null) then use
                // this ObjectFactory or DirObjectFactory instance to recreate the object.
                result = factory.get().getObjectInstance(referenceObject, name, context, env);
            }
            if (result == null) {
                Collection<ServiceReference<ObjectFactory>> factorySRefCollection =
                        getServiceReferences(bundleContext, ObjectFactory.class, null);
                Iterator<ServiceReference<ObjectFactory>> referenceIterator = factorySRefCollection.iterator();
                ObjectFactory objectFactory;
                while (referenceIterator.hasNext()) {
                    ServiceReference serviceReference = referenceIterator.next();
                    if (serviceReference != null) {
                        objectFactory = (ObjectFactory) bundleContext.getService(serviceReference);
                        result = objectFactory.getObjectInstance(referenceObject, name, context, env);
                    }
                }
            }

        }

        if (result == null) {
            result = referenceObject;
        }

        return result;
    }

    private String getUrlScheme(String name) {
        String scheme = name;
        int index = name.indexOf(':');
        if (index != -1) {
            scheme = name.substring(0, index);  //scheme must not contain the colon
        }
        return scheme;
    }

    @Override
    public Object getObjectInstance(Object refInfo, Name name, Context context, Map<?, ?> environment, Attributes attributes) throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.putAll(environment);
        Object result = null;

        Object referenceObject = getReferenceObject(refInfo);

        if (referenceObject instanceof Referenceable) {
            Reference reference = (Reference) referenceObject;
            String factoryClassName = reference.getFactoryClassName();
            if (factoryClassName != null && !"".equals(factoryClassName)) {
                ServiceReference[] factorySRefCollection = bundleContext.getServiceReferences(factoryClassName, null);
                Iterator<ServiceReference> referenceIterator = (Iterator<ServiceReference>) Arrays.asList(factorySRefCollection);
                DirObjectFactory factory;
                while (referenceIterator.hasNext()) {
                    ServiceReference serviceReference = referenceIterator.next();
                    if (serviceReference != null) {
                        factory = (DirObjectFactory) bundleContext.getService(serviceReference);
                        result = factory.getObjectInstance(reference, name, context, env, attributes);
                    }
                }
            } else {
                // 3) If no factory class name is specified, iterate over all the Reference object's StringRefAddrs objects
                // with the address type of URL. For each matching address type, use the value to find a matching
                //URL Context, see URL Context Provider on page 507, and use it to recreate the object. See the
                //Naming Manager for details. If an object is created then it is returned and the algorithm stops
                //here.
                Enumeration<RefAddr> refAddrEnumeration = reference.getAll();
                while (refAddrEnumeration.hasMoreElements()) {
                    RefAddr refAddr = refAddrEnumeration.nextElement();
                    if (refAddr instanceof StringRefAddr && refAddr.getType().equalsIgnoreCase(ADDRESS_TYPE)) {
                        String urlScheme = getUrlScheme((String) refAddr.getContent());
                        Collection<ServiceReference<DirObjectFactory>> factorySRefCollection =
                                getServiceReferences(bundleContext, DirObjectFactory.class, null);
                        Iterator<ServiceReference<DirObjectFactory>> referenceIterator = factorySRefCollection.iterator();
                        while (referenceIterator.hasNext()) {
                            ServiceReference serviceReference = referenceIterator.next();
                            if (serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME).equals(urlScheme)) {
                                DirObjectFactory factory = (DirObjectFactory) bundleContext.getService(serviceReference);
                                result = factory.getObjectInstance(reference, name, context, env);
                            }
                        }
                    }
                }
                if (result == null) {
                    Collection<ServiceReference<DirObjectFactory>> factorySRefCollection =
                            getServiceReferences(bundleContext, DirObjectFactory.class, null);
                    Iterator<ServiceReference<DirObjectFactory>> referenceIterator = factorySRefCollection.iterator();
                    DirObjectFactory factory;
                    while (referenceIterator.hasNext()) {
                        ServiceReference serviceReference = referenceIterator.next();
                        if (serviceReference != null) {
                            factory = (DirObjectFactory) bundleContext.getService(serviceReference);
                            result = factory.getObjectInstance(reference, name, context, env);
                        }
                    }
                }
            }
            if (result == null) {
                result = reference;
            }

        } else {
            // 2.) If the description object is not a Reference object.
            // Iterate over the Object Factory Builder services in ranking order. Attempt to use each such service
            //to create an ObjectFactory or DirObjectFactory instance.
            Collection<ServiceReference<ObjectFactoryBuilder>> objectFactoryBuilderRef =
                    getServiceReferences(bundleContext, ObjectFactoryBuilder.class, getServiceFilter(ObjectFactoryBuilder.class.getName())); //todo cannot apply getServiceFilter
            Optional<DirObjectFactory> factory = getDirObjectFactoryBuilder(referenceObject, objectFactoryBuilderRef, env);
            if (factory.isPresent()) {
                // 3.)If this succeeds (non null) then use
                // this ObjectFactory or DirObjectFactory instance to recreate the object.
                result = factory.get().getObjectInstance(referenceObject, name, context, env);
            }
            if (result == null) {
                Collection<ServiceReference<DirObjectFactory>> factorySRefCollection =
                        getServiceReferences(bundleContext, DirObjectFactory.class, null);
                Iterator<ServiceReference<DirObjectFactory>> referenceIterator = factorySRefCollection.iterator();
                ObjectFactory objectFactory;
                while (referenceIterator.hasNext()) {
                    ServiceReference serviceReference = referenceIterator.next();
                    if (serviceReference != null) {
                        objectFactory = (DirObjectFactory) bundleContext.getService(serviceReference);
                        result = objectFactory.getObjectInstance(referenceObject, name, context, env);
                    }
                }
            }

        }

        if (result == null) {
            result = referenceObject;
        }

        return result;
    }

    //todo move to jndiUtils, both contextManager and this class uses this
    private String getServiceFilter(String userDefinedICFClassName) {
        return "(&" +
                "(" + OBJECT_CLASS + "=" + userDefinedICFClassName + ")" +
                "(" + OBJECT_CLASS + "=" + InitialContextFactory.class.getName() + ")" +
                ")";
    }

    private Object getReferenceObject(Object refInfo) throws NamingException {
        if (refInfo instanceof Referenceable) {
            //2) If the object is a reference object use this as the description object
            Referenceable referenceable = (Referenceable) refInfo;
            return referenceable.getReference();
        } else {
            // this object is either a Reference or another type
            return refInfo;
        }
    }

    /**
     * @param serviceRefCollection
     * @param environment
     * @return
     * @throws NamingException
     */
    private Optional<ObjectFactory> getObjectFactoryBuilder(
            Object referenceObject,
            Collection<ServiceReference<ObjectFactoryBuilder>> serviceRefCollection,
            Hashtable<?, ?> environment) throws NamingException {

        return serviceRefCollection
                .stream()
//                .sorted(new ServiceRankComparator())
                .map(serviceReference -> getService(bundleContext, serviceReference))
                .flatMap(builderOptional -> builderOptional.map(Stream::of).orElseGet(Stream::empty))
                .map(rethrowFunction(builder -> builder.createObjectFactory(referenceObject, environment)))
                .filter(factory -> factory != null)
                .findFirst();

    }


    /**
     * @param serviceRefCollection
     * @param environment
     * @return
     * @throws NamingException
     */
    private Optional<DirObjectFactory> getDirObjectFactoryBuilder(
            Object referenceObject,
            Collection<ServiceReference<ObjectFactoryBuilder>> serviceRefCollection,
            Hashtable<?, ?> environment) throws NamingException {

        return serviceRefCollection
                .stream()
//                .sorted(new ServiceRankComparator())
                .map(serviceReference -> getService(bundleContext, serviceReference))
                .flatMap(builderOptional -> builderOptional.map(Stream::of).orElseGet(Stream::empty))
                .map(rethrowFunction(builder -> (DirObjectFactory) builder.createObjectFactory(referenceObject, environment)))
                .filter(factory -> factory != null)
                .findFirst();

    }

}
