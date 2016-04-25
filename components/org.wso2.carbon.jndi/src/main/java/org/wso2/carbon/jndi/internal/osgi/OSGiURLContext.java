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
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.jndi.internal.Constants;
import org.wso2.carbon.jndi.internal.util.NameParserImpl;
import org.wso2.carbon.jndi.internal.util.StringManager;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

/**
 * JNDI context implementation for handling URL s with osgi: scheme.
 * Sample queries osgi:service/query , osgi:framework/bundleContext.
 */
public class OSGiURLContext implements Context {

    private static final Logger logger = LoggerFactory.getLogger(OSGiURLContext.class);

    /**
     * The environment for this context.
     */
    protected Hashtable<String, Object> env;

    /**
     * The string manager for this package.
     */
    protected static final StringManager SM = StringManager.getManager(Constants.PACKAGE);

    /**
     * Caller bundleContext.
     */
    protected BundleContext callerContext;

    /**
     * service path to retrieve a single Object from service registry.
     */
    protected static final String SERVICE_PATH = "service";

    /**
     * service path to retrieve callers bundleContext.
     */
    protected static final String FRAMEWORK_PATH = "framework";

    /**
     * subContext to get callers bundleContext.
     */
    protected static final String BUNDLE_CONTEXT = "bundleContext";

    /**
     * Name parser for this context.
     */
    protected NameParser parser;

    /**
     * Initializing name parser and class properties.
     *
     * @param callerContext caller bundle context.
     * @param environment   environment properties to set.
     * @throws NamingException If no context can be created for the given environment.
     */
    public OSGiURLContext(BundleContext callerContext, Hashtable<String, Object> environment) throws NamingException {
        this.callerContext = callerContext;
        parser = new NameParserImpl();
        env = new Hashtable<>();
        if (environment != null) {
            Enumeration<String> envEntries = environment.keys();
            while (envEntries.hasMoreElements()) {
                String entryName = envEntries.nextElement();
                addToEnvironment(entryName, environment.get(entryName));
            }
        }
    }

    /**
     * lookup services in the service registry.
     *
     * @param name lookup name for OSGi scheme.
     * @return service or bundleContext based on the query.
     * @throws NamingException if a naming exception is encountered.
     */
    @Override
    public Object lookup(Name name) throws NamingException {
        Object lookupResult;
        //Try to create an OSGi URL from the given name.
        //if the given name is not valid, this will throw an invalidNameException
        OSGiURL osgiURL = new OSGiURL(name);
        String protocol = osgiURL.getFirstComponent();
        String serviceName = osgiURL.getServiceName();

        if (FRAMEWORK_PATH.equals(getSubContext(protocol)) && BUNDLE_CONTEXT.equals(serviceName)) {
            //A JNDI client can also obtain the Bundle Context of the owning bundle by using the osgi: scheme.
            //namespace with the framework/bundleContext query.
            //eg: osgi:framework/bundleContext.
            return callerContext;
        } else if (SERVICE_PATH.equals(getSubContext(protocol))) {
            //The lookup for a URL with the osgi: scheme and service path returns the service object.
            //This scheme only allows a single service to be found.
            lookupResult = findService(callerContext, osgiURL);
        } else {
            lookupResult = null;
        }

        if (lookupResult == null) {
            throw new NameNotFoundException(name.toString());
        }

        return lookupResult;
    }

    /**
     * Retrieves the named object.
     *
     * @param name the name of the object to look up
     * @return the object bound to <tt>name</tt>
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public Object lookup(String name) throws NamingException {
        return lookup(parser.parse(name));
    }

    /**
     * Access service from service registry for given interface name and filter.
     *
     * @param ctx        caller bundle context
     * @param lookupName composite name to lookup.
     * @return service object from service registry.
     * @throws NamingException if No service found with the given URL.
     */
    protected Object findService(BundleContext ctx, OSGiURL lookupName) throws NamingException {
        String serviceName = lookupName.getServiceName();  //this can be an interface or a JNDI-service name.
        String filter = lookupName.getFilter();   //osgi:service/foo/bar/myservice
        //osgi:service/org.wso2.service.Deployer/(serviceId=56)
        //find the service with the given serviceName and filter.
        Object result;
        try {
            result = getService(ctx, serviceName, filter);
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully completed service registry lookup for name : " + lookupName);
            }
        } catch (NameNotFoundException e) {
            //if NameNotFoundException is occurred, the query might have used JNDI service name for the lookup.
            String jndiServiceName = lookupName.getJNDIServiceName();
            filter = "(" + JNDIConstants.JNDI_SERVICENAME + "=" + jndiServiceName + ")";
            if (logger.isDebugEnabled()) {
                logger.debug("No service found for name : " + lookupName +
                        ". Retrying service registry lookup with jndi-service-name : " + jndiServiceName);
            }
            result = getService(ctx, null, filter);  //parse as serviceName=null
        }

        return result;
    }

    private Object getService(BundleContext bundleContext, String serviceName, String filter) throws NamingException {

        try {
            ServiceReference[] serviceReferences = bundleContext.getServiceReferences(serviceName, filter);

            if (serviceReferences != null) {
                for (ServiceReference reference : serviceReferences) {
                    Object serviceObject = bundleContext.getService(reference);

                    if (serviceObject != null) {
                        return serviceObject;
                    }
                }
                throw new NameNotFoundException(SM.getString("osgiUrlContext.noService"));
            } else {
                throw new NameNotFoundException(SM.getString("osgiUrlContext.noServiceReference", serviceName));
            }

        } catch (InvalidSyntaxException e) {
            // If we get an invalid syntax exception we just ignore and return
            // a NameNotFoundException. eg: for queries :- osgi:service/foo/myService (where "foo/myService" is the
            // osgi.jndi.service.name and this may read filter=myService which causes an invalid syntax exception)
            throw new NameNotFoundException(SM.getString("osgiUrlContext.noServiceForFilter", filter));
        }
    }

    /**
     * @param scheme first component of the osgi query (eg: osgi:service)
     * @return scheme path
     */
    protected String getSubContext(String scheme) {
        //osgi:service or osgi:framework
        int index = scheme.indexOf(":");

        String result;

        if (index > 0) {
            result = scheme.substring(index + 1);
        } else {
            result = null;
        }

        return result;
    }

    /**
     * Binds a name to an object.
     * All intermediate contexts and the target context (that named by all
     * but terminal atomic component of the name) must already exist.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws javax.naming.NameAlreadyBoundException            if name is already bound
     * @throws javax.naming.directory.InvalidAttributesException if object did not supply all mandatory attributes
     * @throws NamingException                                   if a naming exception is encountered
     * @throws javax.naming.OperationNotSupportedException       if the context implementation
     *                                                           does not support the bind operation
     */
    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Binds a name to an object.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws javax.naming.NameAlreadyBoundException            if name is already bound
     * @throws javax.naming.directory.InvalidAttributesException if object  did not supply all mandatory attributes
     * @throws NamingException                                   if a jndi exception is encountered
     * @throws OperationNotSupportedException                    if the context implementation
     *                                                           does not support the bind operation
     */
    @Override
    public void bind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Binds a name to an object, overwriting any existing binding. All
     * intermediate contexts and the target context (that named by all but
     * terminal atomic component of the name) must already exist.
     * <p>
     * If the object is a DirContext, any existing attributes associated with
     * the name are replaced with those of the object. Otherwise, any
     * existing attributes associated with the name remain unchanged.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws javax.naming.directory.InvalidAttributesException if object
     *                                                           did not supply all mandatory attributes
     * @throws NamingException                                   if a jndi exception is encountered
     * @throws OperationNotSupportedException                    if the context implementation
     *                                                           does not support the rebind operation
     */
    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Binds a name to an object, overwriting any existing binding.
     *
     * @param name the name to bind; may not be empty
     * @param obj  the object to bind; possibly null
     * @throws javax.naming.directory.InvalidAttributesException if object
     *                                                           did not supply all mandatory attributes
     * @throws NamingException                                   if a jndi exception is encountered
     * @throws OperationNotSupportedException                    if the context implementation
     *                                                           does not support the rebind operation
     */
    @Override
    public void rebind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Unbinds the named object. Removes the terminal atomic name in name
     * from the target context--that named by all but the terminal atomic
     * part of name.
     * <p>
     * This method is idempotent. It succeeds even if the terminal atomic
     * name is not bound in the target context, but throws
     * NameNotFoundException if any of the intermediate contexts do not exist.
     *
     * @param name the name to bind; may not be empty
     * @throws javax.naming.OperationNotSupportedException if an intermediate context does not
     *                                                     exist
     * @throws NamingException                             if a jndi exception is encountered
     */
    @Override
    public void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Unbinds the named object.
     *
     * @param name the name to bind; may not be empty
     * @throws javax.naming.NameNotFoundException          if an intermediate context does not
     *                                                     exist
     * @throws NamingException                             if a jndi exception is encountered
     * @throws javax.naming.OperationNotSupportedException is the context implementation does
     *                                                     support the operation
     */
    @Override
    public void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds the
     * old name. Both names are relative to this context. Any attributes
     * associated with the old name become associated with the new name.
     * Intermediate contexts of the old name are not changed.
     *
     * @param oldName the name of the existing binding; may not be empty
     * @param newName the name of the new binding; may not be empty
     * @throws javax.naming.NameAlreadyBoundException      if newName is already bound
     * @throws NamingException                             if a jndi exception is encountered
     * @throws javax.naming.OperationNotSupportedException is the context implementation does
     *                                                     support the operation
     */
    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Binds a new name to the object bound to an old name, and unbinds the
     * old name.
     *
     * @param oldName the name of the existing binding; may not be empty
     * @param newName the name of the new binding; may not be empty
     * @throws javax.naming.NameAlreadyBoundException      if newName is already bound
     * @throws NamingException                             if a jndi exception is encountered
     * @throws javax.naming.OperationNotSupportedException is the context implementation does
     *                                                     support the operation
     */
    @Override
    public void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Destroys the named context and removes it from the namespace. Any
     * attributes associated with the name are also removed. Intermediate
     * contexts are not destroyed.
     * <p>
     * This method is idempotent. It succeeds even if the terminal atomic
     * name is not bound in the target context, but throws
     * NameNotFoundException if any of the intermediate contexts do not exist.
     * <p>
     * In a federated jndi system, a context from one jndi system may be
     * bound to a name in another. One can subsequently look up and perform
     * operations on the foreign context using a composite name. However, an
     * attempt destroy the context using this composite name will fail with
     * NotContextException, because the foreign context is not a "subcontext"
     * of the context in which it is bound. Instead, use unbind() to remove
     * the binding of the foreign context. Destroying the foreign context
     * requires that the destroySubcontext() be performed on a context from
     * the foreign context's "native" jndi system.
     *
     * @param name the name of the context to be destroyed; may not be empty
     * @throws javax.naming.NameNotFoundException          if an intermediate context does not
     *                                                     exist
     * @throws javax.naming.NotContextException            if the name is bound but does not name
     *                                                     a context, or does not name a context of the appropriate type
     * @throws javax.naming.OperationNotSupportedException is the context implementation does
     *                                                     support the operation
     */
    @Override
    public void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Destroys the named context and removes it from the namespace.
     *
     * @param name the name of the context to be destroyed; may not be empty
     * @throws javax.naming.NameNotFoundException          if an intermediate context does not
     *                                                     exist
     * @throws javax.naming.NotContextException            if the name is bound but does not name
     *                                                     a context, or does not name a context of the appropriate type
     * @throws javax.naming.OperationNotSupportedException is the context implementation does
     *                                                     support the operation
     */
    @Override
    public void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Creates and binds a new context. Creates a new context with the given
     * name and binds it in the target context (that named by all but
     * terminal atomic component of the name). All intermediate contexts and
     * the target context must already exist.
     *
     * @param name the name of the context to create; may not be empty
     * @return the newly created context
     * @throws javax.naming.NameAlreadyBoundException            if name is already bound
     * @throws javax.naming.directory.InvalidAttributesException if creation
     *                                                           of the sub-context requires specification of
     *                                                           mandatory attributes
     * @throws NamingException                                   if a jndi exception is encountered
     * @throws javax.naming.OperationNotSupportedException       is the context implementation does
     *                                                           support the operation
     */
    @Override
    public Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Creates and binds a new context.
     *
     * @param name the name of the context to create; may not be empty
     * @return the newly created context
     * @throws javax.naming.NameAlreadyBoundException            if name is already bound
     * @throws javax.naming.directory.InvalidAttributesException if creation
     *                                                           of the sub-context requires specification of
     *                                                           mandatory attributes
     * @throws NamingException                                   if a jndi exception is encountered
     * @throws javax.naming.OperationNotSupportedException       is the context implementation does
     *                                                           support the operation
     */
    @Override
    public Context createSubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Retrieves the named object, following links except for the terminal
     * atomic component of the name. If the object bound to name is not a
     * link, returns the object itself.
     *
     * @param name the name of the object to look up
     * @return the object bound to name, not following the terminal link
     * (if any).
     * @throws NamingException                             if a jndi exception is encountered
     * @throws javax.naming.OperationNotSupportedException is the context implementation does
     *                                                     support the operation
     */
    @Override
    public Object lookupLink(Name name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Retrieves the named object, following links except for the terminal
     * atomic component of the name.
     *
     * @param name the name of the object to look up
     * @return the object bound to name, not following the terminal link
     * (if any).
     * @throws NamingException                             if a jndi exception is encountered
     * @throws javax.naming.OperationNotSupportedException is the context implementation does
     *                                                     support the operation
     */
    @Override
    public Object lookupLink(String name) throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Retrieves the parser associated with the named context. In a
     * federation of namespaces, different jndi systems will parse names
     * differently. This method allows an application to get a parser for
     * parsing names into their atomic components using the jndi convention
     * of a particular jndi system. Within any single jndi system,
     * NameParser objects returned by this method must be equal (using the
     * equals() test).
     *
     * @param name the name of the context from which to get the parser
     * @return a name parser that can parse compound names into their atomic
     * components
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return parser;
    }

    /**
     * Retrieves the parser associated with the named context.
     *
     * @param name the name of the context from which to get the parser
     * @return a name parser that can parse compound names into their atomic
     * components
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return parser;
    }


    /**
     * Adds a new environment property to the environment of this context. If
     * the property already exists, its value is overwritten.
     *
     * @param propName the name of the environment property to add; may not
     *                 be null
     * @param propVal  the value of the property to add; may not be null
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return env.put(propName, propVal);
    }

    /**
     * Removes an environment property from the environment of this context.
     *
     * @param propName the name of the environment property to remove;
     *                 may not be null
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return env.remove(propName);
    }

    /**
     * Retrieves the environment in effect for this context. See class
     * description for more details on environment properties.
     * The caller should not make any changes to the object returned: their
     * effect on the context is undefined. The environment of this context
     * may be changed using addToEnvironment() and removeFromEnvironment().
     *
     * @return the environment of this context; never null
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        Hashtable<Object, Object> environment = new Hashtable<>();
        environment.putAll(env);
        return environment;
    }

    /**
     * Closes this context. This method releases this context's resources
     * immediately, instead of waiting for them to be released automatically
     * by the garbage collector.
     * This method is idempotent: invoking it on a context that has already
     * been closed has no effect. Invoking any other method on a closed
     * context is not allowed, and results in undefined behaviour.
     *
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public void close() throws NamingException {
        env.clear();
        parser = null;
    }

    /**
     * Retrieves the full name of this context within its own namespace.
     * <p>
     * Many jndi services have a notion of a "full name" for objects in
     * their respective namespaces. For example, an LDAP entry has a
     * distinguished name, and a DNS record has a fully qualified name. This
     * method allows the client application to retrieve this name. The string
     * returned by this method is not a JNDI composite name and should not be
     * passed directly to context methods. In jndi systems for which the
     * notion of full name does not make sense,
     * OperationNotSupportedException is thrown.
     *
     * @return this context's name in its own namespace; never null
     * @throws OperationNotSupportedException if the jndi system does
     *                                        not have the notion of a full name
     * @throws NamingException                if a jndi exception is encountered
     */
    @Override
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException(SM.getString("osgiUrlContext.operationNotSupported"));
    }

    /**
     * Composes the name of this context with a name relative to this context.
     * <p>
     * Given a name (name) relative to this context, and the name (prefix)
     * of this context relative to one of its ancestors, this method returns
     * the composition of the two names using the syntax appropriate for the
     * jndi system(s) involved. That is, if name names an object relative
     * to this context, the result is the name of the same object, but
     * relative to the ancestor context. None of the names may be null.
     *
     * @param name   a name relative to this context
     * @param prefix the name of this context relative to one of its ancestors
     * @return the composition of prefix and name
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return parser.parse(prefix + "/" + name);
    }

    /**
     * Composes the name of this context with a name relative to this context.
     *
     * @param name   a name relative to this context
     * @param prefix the name of this context relative to one of its ancestors
     * @return the composition of prefix and name
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return prefix + "/" + name;
    }

    /**
     * provides Naming Enumeration object which provides a NameClassPair object.
     * useful in cases where a client wishes to iterate over the available services without actually getting them.
     *
     * @param name name of the context to list
     * @return Naming Enumeration object which provides a NameClassPair
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    /**
     * provides Naming Enumeration object which provides a NameClassPair object.
     * useful in cases where a client wishes to iterate over the available services without actually getting them.
     *
     * @param name name of the context to list
     * @return Naming Enumeration object which provides a NameClassPair
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        OSGiURL osgiURL = new OSGiURL(name);
        List<ServiceReference> serviceReferences = getServiceReferences(osgiURL);
        return new OSGiServiceNamingEnumeration(callerContext, serviceReferences);
    }

    /**
     * produce a NamingEnumeration object that provides Binding objects.
     *
     * @param name Composite Name to create the OSGi Name
     * @return NamingEnumeration object that provides Binding objects
     * @throws NamingException if jndi exception encountered
     */
    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    /**
     * produce a NamingEnumeration object that provides Binding objects.
     *
     * @param name Composite Name to create the OSGi Name
     * @return NamingEnumeration object that provides Binding objects
     * @throws NamingException if jndi exception encountered
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        OSGiURL osgiURL = new OSGiURL(name);
        List<ServiceReference> serviceReferences = getServiceReferences(osgiURL);
        return new OSGiServiceBindingsEnumeration(callerContext, serviceReferences);
    }

    private List<ServiceReference> getServiceReferences(OSGiURL osgiURL) throws NamingException {
        ServiceReference[] refs;
        String serviceName = osgiURL.getServiceName();
        String filter = osgiURL.getFilter();

        try {
            refs = callerContext.getServiceReferences(serviceName, filter);
            if (refs == null) {
                refs = getServiceReferencesForJNDIServiceName(osgiURL);
            }
        } catch (InvalidSyntaxException e) {
            // If we get an invalid syntax exception this may due to queries of type :- osgi:service/foo/myService
            // (where "foo/myService" is the
            // osgi.jndi.service.name and this may read filter=myService which causes an invalid syntax exception)
            if (logger.isDebugEnabled()) {
                logger.debug("No service found for service : " + serviceName +
                        ". Retrying service registry lookup with jndi-service-name.");
            }
            refs = getServiceReferencesForJNDIServiceName(osgiURL);
        }

        return Arrays.asList(refs);
    }

    private ServiceReference[] getServiceReferencesForJNDIServiceName(OSGiURL osgiURL) throws NamingException {
        String jndiServiceName = osgiURL.getJNDIServiceName();
        ServiceReference[] refs;
        try {
            refs = callerContext.getServiceReferences((String) null, "(" + JNDIConstants.JNDI_SERVICENAME + "="
                    + jndiServiceName + ")");
            if (refs == null) {
                throw new NameNotFoundException(SM.getString("osgiUrlContext.noRegisteredService", osgiURL.toString()));
            }
        } catch (InvalidSyntaxException e) {
            throw new NamingException(SM.getString("osgiUrlContext.noServiceForFilter", e.getFilter()));
        }
        return refs;
    }

}
