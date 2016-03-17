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
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.ObjectFactoryBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.wso2.carbon.jndi.internal.util.JNDIUtils.getServiceReferences;

/**
 * Implements JNDIProviderAdmin interface.
 */
public class JNDIProviderAdminImpl implements JNDIProviderAdmin {
    private static final Logger logger = LoggerFactory.getLogger(JNDIProviderAdminImpl.class);

    private BundleContext bundleContext;
    private static final String OBJECT_CLASS = "objectClass";
    private ServiceRegistration serviceRegistration;
    public final static String ADDRESS_TYPE = "URL";

    public JNDIProviderAdminImpl(BundleContext bundleContext, ServiceRegistration serviceRegistration) {
        this.bundleContext = bundleContext;
        this.serviceRegistration = serviceRegistration;
    }

    @Override
    public Object getObjectInstance(Object refInfo, Name name, Context context, Map<?, ?> environment) throws Exception {
        Hashtable<Object, Object> env = new Hashtable<>();
        env.putAll(environment);

        //1) If the description object is an instance of Referenceable , then get the corresponding Reference object
        Object referenceObject = getReferenceObject(refInfo);
        if (referenceObject instanceof Reference) {
            Reference reference = (Reference) referenceObject;

            String factoryClassName = reference.getFactoryClassName();
            // 3) If a factory class name is specified, use Bundle Context to search for a service registered under
            // the Reference's factory class name.
            if (factoryClassName != null && !"".equals(factoryClassName)) {
                Collection<ServiceReference<ObjectFactory>> factorySRefCollection =
                        getServiceReferences(bundleContext, ObjectFactory.class, null);
                Iterator<ServiceReference<ObjectFactory>> referenceIterator = factorySRefCollection.iterator();
                ObjectFactory factory;
                while (referenceIterator.hasNext()) {
                    ServiceReference serviceReference = referenceIterator.next();
                    if (serviceReference != null) {
                        factory = (ObjectFactory) bundleContext.getService(serviceReference);
                        return factory.getObjectInstance(reference, name, context, env);
                    }
                }
                return reference;

            } else {
                // 3) If no factory class name is specified, iterate over all the Reference object's StringRefAddrs objects
                // with the address type of URL. For each matching address type, use the value to find a matching
                //URL Context, see URL Context Provider on page 507, and use it to recreate the object. See the
                //Naming Manager for details. If an object is created then it is returned and the algorithm stops
                //here.
                Enumeration<RefAddr> refAddrEnumeration = reference.getAll();
                while (refAddrEnumeration.hasMoreElements()) {
                    RefAddr refAddr = refAddrEnumeration.nextElement();
                    List<String> refUrls = new ArrayList<>();
                    if (refAddr instanceof StringRefAddr && refAddr.getType().equalsIgnoreCase(ADDRESS_TYPE)) {
                        String urlScheme = getUrlScheme((String) refAddr.getContent());
                        //todo get registered urlcontext factory and create object  , there is already a javaURLContextFactory mapped
                        //TODO register a URL context factory with property osgi.jndi.url.scheme in activator
                    }
                }
                return referenceObject;
            }
        } else {
            // 2.) If the description object is not a Reference object.
            // Iterate over the Object Factory Builder services in ranking order. Attempt to use each such service
            //to create an ObjectFactory or DirObjectFactory instance.
            Collection<ServiceReference<ObjectFactoryBuilder>> objectFactoryBuilderRef =
                    getServiceReferences(bundleContext, ObjectFactoryBuilder.class, getServiceFilter(ObjectFactoryBuilder.class.getName())); //todo cannot apply getServiceFilter
            Optional<ObjectFactoryBuilder> objectFactoryBuilder = getObjectFactoryBuilder(referenceObject, objectFactoryBuilderRef, env);
            if (objectFactoryBuilder.isPresent()) {
                // 3.)If this succeeds (non null) then use
                // this ObjectFactory or DirObjectFactory instance to recreate the object.
                ObjectFactory objectFactory = objectFactoryBuilder.get().createObjectFactory(referenceObject, env);
                if (objectFactory != null) {
                    return objectFactory.getObjectInstance(referenceObject, name, context, env);
                }
            }
            return referenceObject;
        }
    }

    private String getUrlScheme(String name) {
        String scheme = name;
        int index = name.indexOf(':');
        if (index != -1) {
            scheme = name.substring(0, index);
        }
        return scheme;
    }

    @Override
    public Object getObjectInstance(Object refInfo, Name name, Context context, Map<?, ?> environment, Attributes attributes) throws Exception {
        Object referenceObject = getReferenceObject(refInfo);

        if (referenceObject instanceof Referenceable) {
            Reference reference = (Reference) referenceObject;
            String userDefinedICFClassName = (String) environment.get(javax.naming.Context.INITIAL_CONTEXT_FACTORY);
            if (userDefinedICFClassName != null && !"".equals(userDefinedICFClassName)) {

            } else {

            }

        } else {
            //go to step 5
        }
        return null;
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
    private Optional<ObjectFactoryBuilder> getObjectFactoryBuilder(
            Object referenceObject,
            Collection<ServiceReference<ObjectFactoryBuilder>> serviceRefCollection,
            Hashtable<?, ?> environment) throws NamingException {
        return null; //todo

//        return serviceRefCollection
//                .stream()
//                .sorted(new ServiceRankComparator())
//                .map(this::getService)
//                .flatMap(factoryOptional -> factoryOptional.map(Stream::of).orElseGet(Stream::empty))
//                .map(rethrowFunction(objectFactoryBuilder -> objectFactoryBuilder.createObjectFactory(referenceObject, environment)))
//                .findFirst();
    }

    /**
     * @param serviceReference
     * @param <S>
     * @return
     */
    private <S> Optional<S> getService(ServiceReference<S> serviceReference) {
        return Optional.ofNullable(bundleContext.getService(serviceReference));
    }

    /**
     *
     */
    private static class ServiceRankComparator implements Comparator<ServiceReference<?>> {

        @Override
        public int compare(ServiceReference<?> ref1, ServiceReference<?> ref2) {
            int rank1 = (Integer) ref1.getProperty("service.ranking");
            int rank2 = (Integer) ref2.getProperty("service.ranking");
            int diff = rank1 - rank2;
            if (diff == 0) {
                int serviceId1 = (Integer) ref1.getProperty("service.id");
                int serviceId2 = (Integer) ref2.getProperty("service.id");
                return -(serviceId1 - serviceId2);
            } else {
                return diff;
            }
        }
    }

}
