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

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.directory.Attributes;
import javax.naming.spi.DirObjectFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;

import static org.wso2.carbon.jndi.internal.util.JNDIUtils.getService;
import static org.wso2.carbon.jndi.internal.util.JNDIUtils.getServiceReferences;
import static org.wso2.carbon.jndi.internal.util.LambdaExceptionUtils.rethrowFunction;

/**
 * Implements JNDIProviderAdmin interface.
 */
public class JNDIProviderAdminImpl implements JNDIProviderAdmin {
    private static final Logger logger = LoggerFactory.getLogger(JNDIProviderAdminImpl.class);

    private BundleContext bundleContext;
    private static final String ADDRESS_TYPE = "URL";

    public JNDIProviderAdminImpl(BundleContext bundleContext, ServiceRegistration serviceRegistration) {
        this.bundleContext = bundleContext;
    }

    @Override
    public Object getObjectInstance(Object refInfo, Name name, Context context, Map<?, ?> environment)
            throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.putAll(environment);
        Object result;

        //1) If the description object is an instance of Referenceable , then get the corresponding Reference object
        Object referenceObject = getReferenceObject(refInfo);
        if (referenceObject instanceof Reference) {
            Reference reference = (Reference) referenceObject;

            String factoryClassName = reference.getFactoryClassName();
            // 3) If a factory class name is specified, use Bundle Context to search for a service registered under
            // the Reference's factory class name.
            if (factoryClassName != null && !"".equals(factoryClassName)) {
                result = getObjectInstanceUsingFactoryClassName(factoryClassName, name, context, env, reference, null);

            } else {
                //3) If no factory class name is specified,use Reference object's StringRefAddrs.
                result = getObjectInstanceUsingRefAddress(reference, name, context, env);
                if (result == null) {
                    //attempt to convert the object with each Object Factory service in ranking order
                    // until a non-null value is returned
                    result = getObjectInstanceUsingObjectFactories(reference, name, context, env);
                }
            }
            if (result == null) {
                result = reference;
            }
        } else {
            // 2.) If the description object is not a Reference object.
            // Iterate over the Object Factory Builder services in ranking order. Attempt to use each such service
            //to create an ObjectFactory or DirObjectFactory instance.
            result = getObjectInstanceUsingObjectFactoryBuilders(referenceObject, name, context, env, null);
            if (result == null) {
                //attempt to convert the object with each Object Factory service in ranking order
                // until a non-null value is returned
                result = getObjectInstanceUsingObjectFactories(referenceObject, name, context, env);
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
    public Object getObjectInstance(Object refInfo, Name name,
                                    Context context, Map<?, ?> environment, Attributes attributes) throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.putAll(environment);
        Object result;

        //1) If the description object is an instance of Referenceable , then get the corresponding Reference object
        Object referenceObject = getReferenceObject(refInfo);

        if (referenceObject instanceof Reference) {
            Reference reference = (Reference) referenceObject;
            String factoryClassName = reference.getFactoryClassName();
            // 3) If a factory class name is specified, use Bundle Context to search for a service registered under
            // the Reference's factory class name.
            if (factoryClassName != null && !"".equals(factoryClassName)) {
                result = getObjectInstanceUsingFactoryClassName(factoryClassName, name, context, env, reference, attributes);
            } else {
                //3) If no factory class name is specified,use Reference object's StringRefAddrs.
                result = getDirObjectInstanceUsingRefAddress(reference, name, context, env, attributes);
                if (result == null) {
                    //attempt to convert the object with each Object Factory service in ranking order
                    // until a non-null value is returned
                    result = getDirObjectInstanceUsingObjectFactories(reference, name, context, env, attributes);
                }
            }
            if (result == null) {
                result = reference;
            }

        } else {
            // 2.) If the description object is not a Reference object.
            // Iterate over the Object Factory Builder services in ranking order. Attempt to use each such service
            //to create an ObjectFactory or DirObjectFactory instance.
            result = getObjectInstanceUsingObjectFactoryBuilders(referenceObject, name, context, env, attributes);
            if (result == null) {
                //attempt to convert the object with each Object Factory service in ranking order
                // until a non-null value is returned
                result = getDirObjectInstanceUsingObjectFactories(referenceObject, name, context, env, attributes);
            }
        }

        if (result == null) {
            result = referenceObject;
        }

        return result;
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

    private Optional<ObjectFactory> getObjectFactoryBuilder(
            Object referenceObject,
            Collection<ServiceReference<ObjectFactoryBuilder>> serviceRefCollection,
            Hashtable<?, ?> environment) throws NamingException {

        return serviceRefCollection
                .stream()
                .map(serviceReference -> getService(bundleContext, serviceReference))
                .flatMap(builderOptional -> builderOptional.map(Stream::of).orElseGet(Stream::empty))
                .map(rethrowFunction(builder ->
                        builder.createObjectFactory(referenceObject, environment)))
                .filter(factory -> factory != null)
                .findFirst();

    }

    private Object getObjectInstanceUsingFactoryClassName(
            String factoryClassName,
            Name name,
            Context context,
            Hashtable<Object, Object> env,
            Reference reference, Attributes attributes) throws Exception {
        Object result = null;
        ObjectFactory objectFactory;
        DirObjectFactory dirObjectFactory;
        ServiceReference[] factorySRefCollection = bundleContext.getServiceReferences(factoryClassName, null);
        Iterator<ServiceReference> referenceIterator =
                (Iterator<ServiceReference>) Arrays.asList(factorySRefCollection);
        while (referenceIterator.hasNext()) {
            ServiceReference serviceReference = referenceIterator.next();
            Object service = bundleContext.getService(serviceReference);
            if (service != null) {
                if (service instanceof DirObjectFactory) {
                    dirObjectFactory = (DirObjectFactory) service;
                    result = dirObjectFactory.getObjectInstance(reference, name, context, env, attributes);
                } else {
                    objectFactory = (ObjectFactory) service;
                    result = objectFactory.getObjectInstance(reference, name, context, env);
                }

            }
        }
        return result;
    }

    private Object getObjectInstanceUsingObjectFactoryBuilders(
            Object referenceObject, Name name, Context context, Hashtable<Object, Object> env, Attributes attributes) throws Exception {
        Object result = null;
        Collection<ServiceReference<ObjectFactoryBuilder>> objectFactoryBuilderRef =
                getServiceReferences(bundleContext, ObjectFactoryBuilder.class, null);
        Optional<ObjectFactory> factory =
                getObjectFactoryBuilder(referenceObject, objectFactoryBuilderRef, env);
        if (factory.isPresent()) {
            // 3.)If this succeeds (non null) then use
            // this DirObjectFactory instance to recreate the object.
            if (factory.get() instanceof DirObjectFactory) {
                result = ((DirObjectFactory) factory.get()).getObjectInstance(referenceObject, name, context, env, attributes);
            } else {
                result = factory.get().getObjectInstance(referenceObject, name, context, env);
            }
        }
        return result;
    }


    //--------------------------- TODO refactor similar methods and avoid duplicate logic -------------------------------

    private Object getObjectInstanceUsingRefAddress(Reference reference,
                                                    Name name,
                                                    Context context,
                                                    Hashtable<Object, Object> env) throws Exception {
        //If no factory class name is specified, iterate over all the Reference object's StringRefAddrs
        //objects with the address type of URL. For each matching address type, use the value to find a matching
        //URL Context
        Object result = null;
        Enumeration<RefAddr> refAddrEnumeration = reference.getAll();
        while (refAddrEnumeration.hasMoreElements()) {
            RefAddr refAddr = refAddrEnumeration.nextElement();
            if (refAddr instanceof StringRefAddr && refAddr.getType().equalsIgnoreCase(ADDRESS_TYPE)) {
                String urlScheme = getUrlScheme((String) refAddr.getContent());
                Iterator<ServiceReference<ObjectFactory>> referenceIterator =
                        getServiceReferences(bundleContext, ObjectFactory.class, null).iterator();
                while (referenceIterator.hasNext()) {
                    ServiceReference serviceReference = referenceIterator.next();
                    if (serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME).equals(urlScheme)) {
                        ObjectFactory factory = (ObjectFactory) bundleContext.getService(serviceReference);
                        result = factory.getObjectInstance(reference, name, context, env);
                        //todo does the parameters be null? page 507
                    }
                }
            }
        }
        return result;
    }

    private Object getDirObjectInstanceUsingRefAddress(Reference reference,
                                                       Name name, Context context,
                                                       Hashtable<Object, Object> env,
                                                       Attributes attributes) throws Exception {
        //3) If no factory class name is specified, iterate over all the Reference object's StringRefAddrs
        //objects with the address type of URL. For each matching address type, use the value to find a matching
        //URL Context.
        Object result = null;
        Enumeration<RefAddr> refAddrEnumeration = reference.getAll();
        while (refAddrEnumeration.hasMoreElements()) {
            RefAddr refAddr = refAddrEnumeration.nextElement();
            if (refAddr instanceof StringRefAddr && refAddr.getType().equalsIgnoreCase(ADDRESS_TYPE)) {
                String urlScheme = getUrlScheme((String) refAddr.getContent());
                Iterator<ServiceReference<DirObjectFactory>> referenceIterator =
                        getServiceReferences(bundleContext, DirObjectFactory.class, null).iterator();
                while (referenceIterator.hasNext()) {
                    ServiceReference serviceReference = referenceIterator.next();
                    if (serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME).equals(urlScheme)) {
                        DirObjectFactory factory =
                                (DirObjectFactory) bundleContext.getService(serviceReference);
                        result = factory.getObjectInstance(reference, name, context, env, attributes);
                    }
                }
            }
        }
        return result;
    }

    private Object getObjectInstanceUsingObjectFactories(Object reference,
                                                         Name name, Context context,
                                                         Hashtable<Object, Object> env) throws Exception {
        Object result = null;
        Iterator<ServiceReference<ObjectFactory>> referenceIterator =
                getServiceReferences(bundleContext, ObjectFactory.class, null).iterator();
        ObjectFactory factory;
        while (referenceIterator.hasNext()) {
            ServiceReference serviceReference = referenceIterator.next();
            factory = (ObjectFactory) bundleContext.getService(serviceReference);
            if (factory != null) {

                result = factory.getObjectInstance(reference, name, context, env);
            }
        }
        return result;
    }

    private Object getDirObjectInstanceUsingObjectFactories(Object reference, Name name,
                                                            Context context, Hashtable<Object, Object> env,
                                                            Attributes attributes) throws Exception {
        Object result = null;
        Iterator<ServiceReference<DirObjectFactory>> referenceIterator =
                getServiceReferences(bundleContext, DirObjectFactory.class, null).iterator();
        DirObjectFactory factory;
        while (referenceIterator.hasNext()) {
            ServiceReference serviceReference = referenceIterator.next();
            factory = (DirObjectFactory) bundleContext.getService(serviceReference);
            if (factory != null) {

                result = factory.getObjectInstance(reference, name, context, env, attributes);
            }
        }
        return result;
    }
}
