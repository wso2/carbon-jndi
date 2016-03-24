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
import org.wso2.carbon.jndi.internal.util.NameParserImpl;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

/**
 * Abstract class for common method implementations for
 * OSGiUrlContext and OSGIUrlListContext.
 */
public abstract class AbstractOSGiUrlContext implements Context {

    /**
     * The environment for this context
     */
    protected Map<String, Object> env;
    protected BundleContext callerContext;
    protected static final String SERVICE_PATH = "service";
    protected static final String SERVICE_LIST_PATH = "servicelist";
    protected static final String FRAMEWORK_PATH = "framework";
    protected static final String BUNDLE_CONTEXT = "bundleContext";
    protected NameParser parser;

    /**
     * Initializing name parser and class properties.
     *
     * @param callerContext caller bundle context.
     * @param environment   environment properties to set.
     */
    public AbstractOSGiUrlContext(BundleContext callerContext, Hashtable<?, ?> environment) {
        this.callerContext = callerContext;
        parser = new NameParserImpl();
        env = new HashMap<>();
        env.putAll((Map<? extends String, ?>) environment);
    }

    /**
     * Initializing name parser and class properties.
     *
     * @param callerContext caller bundle context.
     * @param environment   environment properties to set.
     * @param validName     lookup name
     */
    public AbstractOSGiUrlContext(BundleContext callerContext, Map<String, Object> environment, Name validName) {
        this.callerContext = callerContext;
        parser = new NameParserImpl();
        env = environment;
    }

    /**
     * Access service from service registry for given interface name and filter.
     *
     * @param ctx        caller bundle context
     * @param lookupName composite name to lookup.
     * @param env        environment for this context
     * @return service object from service registry.
     * @throws NamingException
     */
    protected Object findService(BundleContext ctx, OSGiName lookupName,
                                 Map<String, Object> env) throws NamingException {
        String interfaceName = lookupName.getInterface();
        String filter = lookupName.getFilter();
        String serviceName = lookupName.getJNDIServiceName(lookupName.getProtocol());

        //find the service with the given interface and filter.
        Object result;
        try {
            result = getService(ctx, interfaceName, filter);
        } catch (NameNotFoundException e) {
            //if NameNotFoundException is occurred, the query might have used JNDI service name for the lookup.
            filter = "(" + JNDIConstants.JNDI_SERVICENAME + "=" + serviceName + ")";
            result = getService(ctx, null, filter);  //parse the interface=null
        }

        return result;
    }

    private Object getService(BundleContext ctx, String interfaceName, String filter) throws NamingException {

        try {
            ServiceReference[] serviceReferences = ctx.getServiceReferences(interfaceName, filter);

            if (serviceReferences != null) {
                for (ServiceReference reference : serviceReferences) {
                    Object ctxService = ctx.getService(reference);

                    if (ctxService != null) {
                        return ctxService;
                    }
                }
                throw new NameNotFoundException("No service found for service references");
            } else {
                throw new NameNotFoundException("Service reference not found with service : " + interfaceName);
            }

        } catch (InvalidSyntaxException e) {
            // If we get an invalid syntax exception we just ignore and return
            // a NameNotFoundException. eg: for queries :- osgi:service/foo/myService (where "foo/myService" is the
            // osgi.jndi.service.name and this may read filter=myService which causes an invalid syntax exception)
            throw new NameNotFoundException("Could not find matching service from registry with filter : " + filter);
        }
    }

    /**
     * @param scheme first component of the osgi query (eg: osgi:service)
     * @return scheme path
     */
    protected String getSchemePath(String scheme) {
        //osgi:service or osgi:servicelist
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
     * @throws OperationNotSupportedException                    if the context implementation
     *                                                           does not support the bind operation
     */
    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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
        throw new OperationNotSupportedException();
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

}
