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
import org.wso2.carbon.jndi.internal.util.LambdaExceptionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
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
        Optional<Object> result;

        //1) If the description object is an instance of Referenceable , then get the corresponding Reference object
        Object referenceObject = getReferenceObject(refInfo);
        if (referenceObject instanceof Reference) {

            Reference reference = (Reference) referenceObject;

            String factoryClassName = reference.getFactoryClassName();
            if (factoryClassName != null && !"".equals(factoryClassName)) {
                // 3) If a factory class name is specified, use Bundle Context to search for a service registered under
                // the Reference's factory class name and create the object.
                result = createObjectUsingObjectFactoryClassName(factoryClassName, name, context, env, reference);

            } else {
                //3) If no factory class name is specified,use Reference object's StringRefAddrs.
                result = createObjectUsingStringRefAddress(reference, null, null, env);
                if (!result.isPresent()) {
                    //attempt to convert the object with each Object Factory service in ranking order
                    // until a non-null value is returned
                    result = createObjectUsingObjectFactories(reference, name, context, env);
                }
            }
            if (!result.isPresent()) {
                result = Optional.of(reference);
            }
        } else {
            // 2.) If the description object is not a Reference object.
            // Iterate over the Object Factory Builder services in ranking order. Attempt to use each such service
            //to create an ObjectFactory or DirObjectFactory instance.
            result = createObjectUsingObjectFactoryBuilders(referenceObject, name, context, env, null);
            if (!result.isPresent()) {
                //attempt to convert the object with each Object Factory service in ranking order
                // until a non-null value is returned
                result = createObjectUsingObjectFactories(referenceObject, name, context, env);
            }

        }

        if (!result.isPresent()) {
            result = Optional.of(referenceObject);
        }

        return result.get();
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
        Optional<Object> result;

        //1) If the description object is an instance of Referenceable , then get the corresponding Reference object
        Object referenceObject = getReferenceObject(refInfo);

        if (referenceObject instanceof Reference) {

            Reference reference = (Reference) referenceObject;

            String factoryClassName = reference.getFactoryClassName();
            if (factoryClassName != null && !"".equals(factoryClassName)) {
                // 3) If a factory class name is specified, use Bundle Context to search for a service registered under
                // the Reference's factory class name and create the object.
                result = createObjectUsingDirObjectFactoryClassName(factoryClassName,
                        name, context, env, reference, attributes);
                if (!result.isPresent()) {
                    //consult Object factory services if creating object with Dir Object factory services fails.
                    result = createObjectUsingObjectFactoryClassName(factoryClassName, name, context, env, reference);
                }
            } else {
                //3) If no factory class name is specified,use Reference object's StringRefAddrs.
                result = createDirObjectUsingStringRefAddress(reference, null, null, env, attributes);
                if (!result.isPresent()) {
                    //consult Object factory services if creating object with Dir Object factory services fails.
                    result = createObjectUsingStringRefAddress(reference, name, context, env);
                }
                if (!result.isPresent()) {
                    //attempt to convert the object with each Object Factory service in ranking order
                    // until a non-null value is returned
                    result = createObjectUsingDirObjectFactories(reference, name, context, env, attributes);
                }
            }
            if (!result.isPresent()) {
                result = Optional.of(reference);
            }

        } else {
            // 2.) If the description object is not a Reference object.
            // Iterate over the Object Factory Builder services in ranking order. Attempt to use each such service
            //to create an ObjectFactory or DirObjectFactory instance.
            result = createObjectUsingObjectFactoryBuilders(referenceObject, name, context, env, attributes);
            if (!result.isPresent()) {
                //attempt to convert the object with each Object Factory service in ranking order
                // until a non-null value is returned
                result = createObjectUsingDirObjectFactories(referenceObject, name, context, env, attributes);
            }
        }

        if (!result.isPresent()) {
            result = Optional.of(referenceObject);
        }

        return result.get();
    }

    private Object getReferenceObject(Object refInfo) throws NamingException {
        if (refInfo instanceof Referenceable) {
            //2) If the object is a reference object use this as the description object
            Referenceable referenceable = (Referenceable) refInfo;
            return referenceable.getReference();
        } else {
            // this object can be another type
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

    private Optional<Object> createObjectUsingObjectFactoryClassName(String factoryClassName,
                                                                     Name name,
                                                                     Context context,
                                                                     Hashtable<Object, Object> env,
                                                                     Reference reference) throws Exception {

        if (bundleContext.getServiceReferences(factoryClassName, null) == null) {
            return Optional.empty();
        }
        return Arrays.stream(bundleContext.getServiceReferences(factoryClassName, null))
                .map(bundleContext::getService)
                .filter(service -> service != null)
                .map(service -> convertToType(service, ObjectFactory.class))
                .map(LambdaExceptionUtils.rethrowFunction(
                        factory -> factory.getObjectInstance(reference, name, context, env)))
                .findFirst();
    }

    private <T, U> U convertToType(T t, Class<U> clazz) {
        return clazz.cast(t);
    }

    private Optional<Object> createObjectUsingDirObjectFactoryClassName(String factoryClassName,
                                                                        Name name,
                                                                        Context context,
                                                                        Hashtable<Object, Object> env,
                                                                        Reference reference,
                                                                        Attributes attributes) throws Exception {

        if (bundleContext.getServiceReferences(factoryClassName, null) == null) {
            return Optional.empty();
        }

        return Arrays.stream(bundleContext.getServiceReferences(factoryClassName, null))
                .map(bundleContext::getService)
                .filter(service -> service != null)
                .map(service -> convertToType(service, DirObjectFactory.class))
                .map(LambdaExceptionUtils.rethrowFunction(
                        factory -> factory.getObjectInstance(reference, name, context, env, attributes)))
                .findFirst();
    }

    private Optional<Object> createObjectUsingObjectFactoryBuilders(Object referenceObject,
                                                                    Name name,
                                                                    Context context,
                                                                    Hashtable<Object, Object> env,
                                                                    Attributes attributes) throws Exception {
        Optional<Object> result = Optional.empty();
        Collection<ServiceReference<ObjectFactoryBuilder>> objectFactoryBuilderRef =
                getServiceReferences(bundleContext, ObjectFactoryBuilder.class, null);
        Optional<ObjectFactory> factory =
                getObjectFactoryBuilder(referenceObject, objectFactoryBuilderRef, env);
        if (factory.isPresent()) {
            // 3.)If this succeeds (non null) then use
            // this DirObjectFactory instance to recreate the object.
            if (factory.get() instanceof DirObjectFactory) {
                result = Optional.ofNullable(((DirObjectFactory) factory.get()).
                        getObjectInstance(referenceObject, name, context, env, attributes));
            } else {
                result = Optional.ofNullable(factory.get().getObjectInstance(referenceObject, name, context, env));
            }
        }
        return result;
    }

    private Optional<Object> createObjectUsingStringRefAddress(Reference reference,
                                                               Name name,
                                                               Context context,
                                                               Hashtable<Object, Object> env) throws Exception {
        Enumeration<RefAddr> refAddrEnumeration = reference.getAll();
        while (refAddrEnumeration.hasMoreElements()) {
            RefAddr refAddr = refAddrEnumeration.nextElement();
            //Iterate over all the Reference object's StringRefAddrs
            //objects with the address type of URL.
            if (refAddr instanceof StringRefAddr && refAddr.getType().equalsIgnoreCase(ADDRESS_TYPE)) {
                // For each matching address type, use the value to find a matching URL Context
                String urlScheme = getUrlScheme((String) refAddr.getContent());

                return getServiceReferences(bundleContext, ObjectFactory.class, null).stream()
                        .filter(serviceReference -> serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME) != null
                                && serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME).equals(urlScheme))
                        .map(bundleContext::getService)
                        .filter(service -> service != null)
                        .map(service -> convertToType(service, ObjectFactory.class))
                        .map(LambdaExceptionUtils.rethrowFunction(
                                factory -> factory.getObjectInstance(reference, name, context, env)))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private Optional<Object> createDirObjectUsingStringRefAddress(Reference reference,
                                                                  Name name, Context context,
                                                                  Hashtable<Object, Object> env,
                                                                  Attributes attributes) throws Exception {
        Enumeration<RefAddr> refAddrEnumeration = reference.getAll();
        while (refAddrEnumeration.hasMoreElements()) {
            RefAddr refAddr = refAddrEnumeration.nextElement();
            //Iterate over all the Reference object's StringRefAddrs
            //objects with the address type of URL.
            if (refAddr instanceof StringRefAddr && refAddr.getType().equalsIgnoreCase(ADDRESS_TYPE)) {
                // For each matching address type, use the value to find a matching URL Context
                String urlScheme = getUrlScheme((String) refAddr.getContent());

                return getServiceReferences(bundleContext, DirObjectFactory.class, null).stream()
                        .filter(serviceReference -> serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME) != null
                                && serviceReference.getProperty(JNDIConstants.JNDI_URLSCHEME).equals(urlScheme))
                        .map(bundleContext::getService)
                        .filter(service -> service != null)
                        .map(service -> convertToType(service, DirObjectFactory.class))
                        .map(LambdaExceptionUtils.rethrowFunction(
                                factory -> factory.getObjectInstance(reference, name, context, env, attributes)))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private Optional<Object> createObjectUsingObjectFactories(Object reference,
                                                              Name name, Context context,
                                                              Hashtable<Object, Object> env) throws Exception {

        return getServiceReferences(bundleContext, ObjectFactory.class, null).stream()
                .map(bundleContext::getService)
                .filter(service -> service != null)
                .map(service -> convertToType(service, ObjectFactory.class))
                .map(LambdaExceptionUtils.rethrowFunction(
                        factory -> factory.getObjectInstance(reference, name, context, env)))
                .findFirst();
    }

    private Optional<Object> createObjectUsingDirObjectFactories(Object reference, Name name,
                                                                 Context context, Hashtable<Object, Object> env,
                                                                 Attributes attributes) throws Exception {

        return getServiceReferences(bundleContext, DirObjectFactory.class, null).stream()
                .map(bundleContext::getService)
                .filter(service -> service != null)
                .map(service -> convertToType(service, DirObjectFactory.class))
                .map(LambdaExceptionUtils.rethrowFunction(
                        factory -> factory.getObjectInstance(reference, name, context, env)))
                .findFirst();
    }
}
