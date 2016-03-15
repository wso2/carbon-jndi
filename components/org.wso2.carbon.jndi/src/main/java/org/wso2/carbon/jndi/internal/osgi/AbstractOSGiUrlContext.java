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

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Abstract class for common method implementations for
 * OSGiUrlContext.
 *
 */
public abstract class AbstractOSGiUrlContext implements Context{

    /**
     * The environment for this context
     */
    protected Map<String, Object> env;
    protected BundleContext callerContext;
    public static final String SERVICE_PATH = "service";
    public static final String SERVICE_LIST_PATH = "servicelist";
    public static final String FRAMEWORK_PATH = "framework";
    public static final String BUNDLE_CONTEXT = "bundleContext";
    NameParser parser;

    public AbstractOSGiUrlContext(BundleContext callerContext, Hashtable<?, ?> environment) {
        this.callerContext = callerContext;
        parser = new NameParserImpl();
        env = new HashMap<>();
        env.putAll((Map<? extends String, ?>) environment);
    }

    public AbstractOSGiUrlContext(BundleContext callerContext, Map<String, Object> environment, Name validName) {
        this.callerContext = callerContext;
        parser = new NameParserImpl();
        env = environment;
    }

    //todo move to a util class?
    protected Object findService(BundleContext ctx, OSGiName lookupName,
                                 Map<String, Object> env) throws NamingException {
        String interfaceName = lookupName.getInterface();
        String filter = lookupName.getFilter();
        String serviceName = lookupName.getJNDIServiceName();

        Object result;
        result = getService(ctx, interfaceName, filter);

        if (result == null) {
            interfaceName = null;
            filter = "(" + JNDIConstants.JNDI_SERVICENAME + "=" + serviceName + ')';
            result = getService(ctx, interfaceName, filter);
        }

        return result;
    }

    private Object getService(BundleContext ctx, String interfaceName, String filter)
            throws NamingException {

        try {
            ServiceReference[] serviceReferences = ctx.getServiceReferences(interfaceName, filter);

            if (serviceReferences != null) {
                // natural order is the exact opposite of the order we desire.
                Arrays.sort(serviceReferences, new Comparator<ServiceReference>() {
                    public int compare(ServiceReference reference1, ServiceReference reference2) {
                        return reference2.compareTo(reference1);
                    }
                });

                for (ServiceReference reference : serviceReferences) {
                    Object ctxService = ctx.getService(reference);

                    if (ctxService != null) {
                        return ctxService;
                    }
                }
            }

        } catch (InvalidSyntaxException e) {
            // If we get an invalid syntax exception we just ignore and return
            // a null. eg: for queries :- osgi:service/foo/myService (where "foo/myService" is the
            // osgi.jndi.service.name and this may read filter=myService which causes and invalid syntax exception)
        }
        return null;
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

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return parser.parse(prefix + "/" + name);
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return prefix + "/" + name;
    }
}
