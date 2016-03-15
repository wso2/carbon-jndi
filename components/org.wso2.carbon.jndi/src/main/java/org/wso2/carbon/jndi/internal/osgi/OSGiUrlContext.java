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

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * JNDI context implementation for handling osgi:service lookup.
 * Sample query osgi:service/<interface>/<filter>
 */
public class OSGiUrlContext extends AbstractOSGiUrlContext {

    public OSGiUrlContext(BundleContext callerContext, Hashtable<?, ?> environment) {
        super(callerContext, environment);
    }

    @Override
    public Object lookup(Name name) throws NamingException {

        //osgi:service/<interface>/<filter>
        Object lookupResult;
        OSGiName osGiName = new OSGiName(name);
        String scheme = name.get(0);
        String interfaceName;
        String filter = null;
        //The owning bundle is the bundle that requested the initial Context from the JNDI Context Manager
        //service or received its Context through the InitialContext class
        if (osGiName.hasInterface()) {
            interfaceName = osGiName.getInterface();
            if (FRAMEWORK_PATH.equals(getSchemePath(scheme)) && BUNDLE_CONTEXT.equals(interfaceName)) {
                //A JNDI client can also obtain the Bundle Context of the owning bundle by using the osgi: scheme
                //namespace with the framework/bundleContext name.
                //osgi:framework/bundleContext.
                return callerContext;
            } else if (getSchemePath(scheme).equals(SERVICE_PATH)) {
                //The lookup for a URL with the osgi: scheme and service path returns the service with highest
                //service.ranking and the lowest service.id. This scheme only allows a single service to be found
                lookupResult = findService(callerContext, osGiName, env);
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

    private String getSchemePath(String scheme) {
        //osgi:service or osgi:servicelist
        int index = scheme.indexOf(':');

        String result;

        if (index > 0) {
            result = scheme.substring(index + 1);
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
    public Name composeName(Name name, Name prefix) throws NamingException {
        return null;  //todo
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return null; //todo
    }

    @Override
    public void close() throws NamingException {

    }
}
