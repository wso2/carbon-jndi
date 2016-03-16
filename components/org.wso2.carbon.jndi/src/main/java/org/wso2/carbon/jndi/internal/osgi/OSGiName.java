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

import java.util.Enumeration;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;

/**
 * A composite name to represent Osgi url scheme.
 */
public class OSGiName extends CompositeName {

    public OSGiName(String name) throws InvalidNameException {
        super(name);
    }

    public OSGiName(Name name) throws InvalidNameException {
        this(name.toString());
    }

    public boolean hasInterface() {
        return size() > 1;
    }

    public boolean hasFilter() {
        //following query will result size()>3 as size() will count the components separated by "/"
        //osgi:service/org.wso2.carbon.jndi.osgi.services.FooService/(osgi.jndi.service.name=foo/myService)
        return size() == 3;
    }

    public String getJNDIServiceName() {
        StringBuilder builder = new StringBuilder();
        Enumeration<String> parts = getAll();
        parts.nextElement();  //we need to skip the first component (eg:osgi:service)

        if (parts.hasMoreElements()) {
            while (parts.hasMoreElements()) {
                builder.append(parts.nextElement());
                builder.append('/');
            }

            builder.deleteCharAt(builder.length() - 1);  //remove last "/" character
        }

        return builder.toString();
    }

    public String getInterface() {
        return hasInterface() ? get(1) : null;
    }

    public String getFilter() {
        return hasFilter() ? get(2) : null;
    }
}
