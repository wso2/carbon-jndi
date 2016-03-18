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
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.naming.Binding;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * OSGiServiceBinding enumeration implementation..
 */
public class OSGiServiceBindingsEnumeration implements NamingEnumeration<Binding> {

    private BundleContext bundleContext;
    private ServiceReference[] serviceReferences;
    protected Iterator<Binding> iterator;
    protected List<Binding> nameClassPairList;

    public OSGiServiceBindingsEnumeration(BundleContext bundleContext, ServiceReference[] refs) {
        this.serviceReferences = refs;
        this.bundleContext = bundleContext;
        nameClassPairList = buildNameClassPair();
        iterator = nameClassPairList.iterator();
    }

    private List<Binding> buildNameClassPair() {
        nameClassPairList = new ArrayList<>();
        for (ServiceReference serviceReference : serviceReferences) {
            Object service = bundleContext.getService(serviceReference);
            String className = service.getClass().getName();
            String name = String.valueOf(serviceReference.getProperty(Constants.SERVICE_ID));
            Binding binding = new Binding(name, className, service);
            nameClassPairList.add(binding);
        }
        return nameClassPairList;
    }

    @Override
    public Binding next() throws NamingException {
        return nextElement();
    }

    @Override
    public boolean hasMore() throws NamingException {
        return iterator.hasNext();
    }

    @Override
    public void close() throws NamingException {

    }

    @Override
    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    @Override
    public Binding nextElement() {
        return iterator.next();
    }
}