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

import org.osgi.framework.AdminPermission;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;
import org.wso2.carbon.jndi.internal.util.NameParserImpl;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * JNDI context implementation for handling osgi:service lookup.
 * Sample query osgi:service/<interface>/<filter>
 */
public class OSGiUrlContext implements Context {
    /**
     * The environment for this context
     */
    protected Map<String, Object> env;
    private BundleContext callerContext;
    public static final String SERVICE_PATH = "service";
    public static final String SERVICE_LIST_PATH = "servicelist";
    public static final String FRAMEWORK_PATH = "framework";
    public static final String BUNDLE_CONTEXT = "bundlContext";
    NameParser parser;

    public OSGiUrlContext(BundleContext callerContext, Hashtable<?, ?> environment) {
        this.callerContext = callerContext;
        parser = new NameParserImpl();
        env = new HashMap<>();
        env.putAll((Map<? extends String, ?>) environment);
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        Object lookupResult;
        OSGiName osGiName = (OSGiName) name;
        String scheme = name.get(0);
        String interfaceName = null;
        String filter = null;
        //The owning bundle is the bundle that requested the initial Context from the JNDI Context Manager
        //service or received its Context through the InitialContext class
        if (osGiName.hasInterface()) {
            interfaceName = osGiName.get(1);
            if (FRAMEWORK_PATH.equals(getSchemePath(scheme)) && BUNDLE_CONTEXT.equals(interfaceName)) {
                //A JNDI client can also obtain the Bundle Context of the owning bundle by using the osgi: scheme
                //namespace with the framework/bundleContext name.
                //osgi:framework/bundleContext.
                return callerContext;
            } else if (getSchemePath(scheme).equals(SERVICE_PATH)) {
                //The lookup for a URL with the osgi: scheme and service path returns the service with highest
                //service.ranking and the lowest service.id. This scheme only allows a single service to be found
                lookupResult = getService(callerContext, osGiName, null, true, env);
            } else if (getSchemePath(scheme).equals(SERVICE_LIST_PATH)) {
                //If this osgi:servicelist scheme is used from a lookup method then a Context object is returned
                //instead of a service object
                lookupResult = new OSGiUrlListContext(callerContext, env, name);
            } else {
                lookupResult = null;
            }

        } else {
            lookupResult = new OSGiUrlListContext(callerContext, env, name);
        }

        if (lookupResult == null) {
            throw new NameNotFoundException(name.toString());
        }

        return lookupResult;
    }

    private Object getService(BundleContext ctx, OSGiName lookupName, String id,
                              boolean dynamicRebind, Map<String, Object> env) throws NamingException {
        String interfaceName = lookupName.get(1);
        String filter = lookupName.hasFilter()? lookupName.get(2) : null;
        String serviceName = lookupName.getServiceName();  //todo

        if (id != null) {
            if (filter == null) {
                filter = '(' + Constants.SERVICE_ID + '=' + id + ')';
            } else {
                filter = "(&(" + Constants.SERVICE_ID + '=' + id + ')' + filter + ')';
            }
        }

        ServicePair pair = null;

        if (!lookupName.isServiceNameBased()) {
            pair = findService(ctx, interfaceName, filter);
        }

        if (pair == null) {
            interfaceName = null;
            if (id == null) {
                filter = "(" + JNDIConstants.JNDI_SERVICENAME + "=" + serviceName + ')';
            } else {
                filter = "(&(" + Constants.SERVICE_ID + '=' + id + ")(" + JNDIConstants.JNDI_SERVICENAME
                        + "=" + serviceName + "))";
            }
            pair = findService(ctx, interfaceName, filter);
        }

        Object result = null;

        if (pair != null) {
            if (requireProxy) {
                Object obj = env.get(org.apache.aries.jndi.api.JNDIConstants.REBIND_TIMEOUT);
                int timeout = 0;
                if (obj instanceof String) {
                    timeout = Integer.parseInt((String) obj);
                } else if (obj instanceof Integer) {
                    timeout = (Integer) obj;
                }

                result = proxy(interfaceName, filter, dynamicRebind, ctx, pair, timeout);
            }
        }

        return result;
    }

    private Object findService(BundleContext ctx, String interface1, String filter)
            throws NamingException {
        Object p = null;

        try {
            ServiceReference[] refs = ctx.getServiceReferences(interface1, filter);

            if (refs != null) {
                // natural order is the exact opposite of the order we desire.
                Arrays.sort(refs, new Comparator<ServiceReference>() {
                    public int compare(ServiceReference o1, ServiceReference o2) {
                        return o2.compareTo(o1);
                    }
                });

                for (ServiceReference ref : refs) {
                    Object service = ctx.getService(ref);

                    if (service != null) {
                        p = service;
                        break;
                    }
                }
            }

        } catch (InvalidSyntaxException e) {
            // ignore
        }

        return p;
    }

    public String getSchemePath(String scheme) {
        String part0 = scheme;  //osgi:service or osgi:servicelist
        int index = part0.indexOf(':');

        String result;

        if (index > 0) {
            result = part0.substring(index + 1);
        } else {
            result = null;
        }

        return result;
    }

    @Override
    public Object lookup(String name) throws NamingException {
        return lookup(parser.parse(name));
    }

    @Override
    public void bind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void bind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rebind(Name name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rebind(String name, Object obj) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void unbind(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void unbind(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rename(Name oldName, Name newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void rename(String oldName, String newName) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return new OSGiUrlListContext(callerContext, env, name).list("");
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return list(parser.parse(name));
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return new OSGiUrlListContext(callerContext, env, name).listBindings("");
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return listBindings(parser.parse(name));
    }

    @Override
    public void destroySubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public void destroySubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Context createSubcontext(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Context createSubcontext(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Object lookupLink(Name name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public Object lookupLink(String name) throws NamingException {
        throw new OperationNotSupportedException();
    }

    @Override
    public NameParser getNameParser(Name name) throws NamingException {
        return parser;
    }

    @Override
    public NameParser getNameParser(String name) throws NamingException {
        return parser;
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return null;  //todo
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return null; //todo
    }

    @Override
    public Object addToEnvironment(String propName, Object propVal) throws NamingException {
        return env.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment(String propName) throws NamingException {
        return env.remove(propName);
    }

    @Override
    public Hashtable<?, ?> getEnvironment() throws NamingException {
        Hashtable<Object, Object> environment = new Hashtable<>();
        environment.putAll(env);
        return environment;
    }

    @Override
    public void close() throws NamingException {

    }

    @Override
    public String getNameInNamespace() throws NamingException {
        throw new OperationNotSupportedException();
    }
}
