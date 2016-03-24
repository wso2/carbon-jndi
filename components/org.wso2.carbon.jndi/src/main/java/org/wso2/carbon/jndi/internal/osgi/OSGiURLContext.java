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

import java.util.Hashtable;
import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * JNDI context implementation for handling osgi:service queries.
 * Sample query osgi:service/interface/filter
 */
public class OSGiURLContext extends AbstractOSGiURLContext {

    /**
     * Set the owning bundle context and environment variables.
     *
     * @param callerContext caller bundleContext
     * @param environment   environment information to create the context
     */
    public OSGiURLContext(BundleContext callerContext, Hashtable<?, ?> environment) {
        super(callerContext, environment);
    }

    /**
     * lookup services in the service registry.
     *
     * @param name lookup name for OSGi scheme.
     * @return service or serviceList context based on the query.
     * @throws NamingException if a naming exception is encountered.
     */
    @Override
    public Object lookup(Name name) throws NamingException {

        //osgi:service/<interface>/<filter>
        Object lookupResult;
        OSGiName osgiName = new OSGiName(name);
        String protocol = osgiName.getProtocol();
        String interfaceName;
        //The owning bundle is the bundle that requested the initial Context from the JNDI Context Manager
        //service or received its Context through the InitialContext class
        if (osgiName.containsQuery()) {
            interfaceName = osgiName.getInterface();
            if (FRAMEWORK_PATH.equals(getSubContext(protocol)) && BUNDLE_CONTEXT.equals(interfaceName)) {
                //A JNDI client can also obtain the Bundle Context of the owning bundle by using the osgi: scheme
                //namespace with the framework/bundleContext name.
                //osgi:framework/bundleContext.
                return callerContext;
            } else if (SERVICE_PATH.equals(getSubContext(protocol))) {
                //The lookup for a URL with the osgi: scheme and service path returns the service.
                //This scheme only allows a single service to be found
                lookupResult = findService(callerContext, osgiName, env);
            } else if (SERVICE_LIST_PATH.equals(getSubContext(protocol))) {
                //If this osgi:servicelist scheme is used from a lookup method then a Context object is returned
                //instead of a service object
                lookupResult = new OSGiURLListContext(callerContext, env, name);
            } else {
                lookupResult = null;
            }

        } else {
            lookupResult = new OSGiURLListContext(callerContext, env, name);
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
     * provides Naming Enumeration object which provides a NameClassPair object.
     * useful in cases where a client wishes to iterate over the available services without actually getting them.
     *
     * @param name name of the context to list
     * @return Naming Enumeration object which provides a NameClassPair
     * @throws NamingException if a jndi exception is encountered
     */
    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return new OSGiURLListContext(callerContext, env, name).list(name);
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
        return list(parser.parse(name));
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
        return new OSGiURLListContext(callerContext, env, name).listBindings(name);
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
        return listBindings(parser.parse(name));
    }

}
