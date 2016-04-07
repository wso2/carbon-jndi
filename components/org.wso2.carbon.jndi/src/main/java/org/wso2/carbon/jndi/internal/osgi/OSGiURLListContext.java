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

import java.util.Map;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * JNDI context implementation for handling osgi:servicelist queries.
 */
public class OSGiURLListContext extends OSGiURLContext {

    private Name lookupName;

    /**
     * Set the owning bundle context and environment variables.
     *
     * @param callerContext caller bundleContext.
     * @param environment   environment information to create the context.
     * @param name          name for the context.
     * @throws InvalidNameException if creating OSGIName fails.
     */
    public OSGiURLListContext(BundleContext callerContext, Map<String, Object> environment, Name name)
            throws InvalidNameException {
        super(callerContext, environment, name);
        this.lookupName = name;
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
        return lookup(name.toString());
    }

    /**
     * lookup services in the service registry.
     *
     * @param name lookup name for OSGi scheme.
     * @return service or serviceList context based on the query.
     * @throws NamingException if a naming exception is encountered.
     */
    @Override
    public Object lookup(String name) throws NamingException {
        OSGiName osGiName;
        try {
            osGiName = new OSGiName(name);
        } catch (InvalidNameException exception) {
            //may throw InvalidNameException if the lookup is for lookup(<query>)
            //pattern (in case if customer initially call lookup("servicelist/"))
            osGiName = (OSGiName) buildName(name);
        }
        Object result = findService(callerContext, osGiName, env);
        if (result == null) {
            throw new NameNotFoundException(name);
        }
        return result;
    }

    private Name buildName(String query) throws InvalidNameException {
        if (lookupName != null) {
            for (String component : query.split("/")) {
                lookupName.add(component);
            }
        }
        return lookupName;
    }
}
