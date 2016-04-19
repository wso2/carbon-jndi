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

import org.wso2.carbon.jndi.internal.Constants;
import org.wso2.carbon.jndi.internal.util.StringManager;

import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Predicate;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.OperationNotSupportedException;

/**
 * A composite name to represent Osgi url scheme.
 */
public class OSGiURL extends CompositeName {

    /**
     * The serial version UID
     */
    private static final long serialVersionUID = 7079733784681646165L;

    /**
     * The string manager for this package.
     */
    protected static final StringManager SM = StringManager.getManager(Constants.PACKAGE);

    public OSGiURL(String url) throws InvalidNameException, OperationNotSupportedException {
        super(url);
        if (!isValid()) {
            throw new InvalidNameException(SM.getString("osgiUrl.invalidURL", url));
        }
    }

    public OSGiURL(Name name) throws InvalidNameException, OperationNotSupportedException {
        this(name.toString());
    }

    /**
     * @return true if Composite name has a query.
     */
    public boolean containsQuery() {
        return size() > 1;
    }

    /**
     * @return true if Composite name has a filter.
     */
    public boolean containsFilter() {
        //following query will result size()>3 as size() method will count the components separated by "/"
        //osgi:service/org.wso2.carbon.jndi.osgi.services.FooService/(osgi.jndi.service.name=foo/myService)
        return size() == 3;
    }

    /**
     * construct a JNDI service name with the given composite name.
     *
     * @return JNDI service name
     */
    public String getJNDIServiceName() {
        //if the JNDI service name is foo, then the URL :osgi:service/foo selects the service
        String serviceName = this.toString(); //osgi:service
        return serviceName.substring(this.getFirstComponent().length() + 1);
    }

    /**
     * @return second component of the Composite name
     */
    public String getServiceName() {
        int secondComponentIndex = 1;
        return containsQuery() ? get(secondComponentIndex) : null;
    }

    /**
     * @return third component of the Composite name
     */
    public String getFilter() {
        int thirdComponentIndex = 2;
        return containsFilter() ? get(thirdComponentIndex) : null;
    }

    /**
     * @return first component of the Composite name
     */
    public String getFirstComponent() {
        //will return osgi:service or osgi:framework
        int firstComponentIndex = 0;
        return this.get(firstComponentIndex);
    }

    private boolean isValid() throws OperationNotSupportedException {
        boolean isValid = false;
        Enumeration<String> nameComponents = this.getAll();
        //valid URL samples are:
        // 1. osgi:service/<query>
        // 2. osgi:framework/bundleContext
        if (nameComponents.hasMoreElements()) {
            Predicate<String> osgiServiceFilter = servicePathSubContext ->
                    "osgi:service".equals(servicePathSubContext) && size() > 1;
            Predicate<String> osgiFrameworkFilter = frameworkPathSubContext ->
                    "osgi:framework".equals(frameworkPathSubContext) &&
                            nameComponents.hasMoreElements() &&
                            "bundleContext".equals(nameComponents.nextElement());
            Predicate<String> orFilter = osgiFrameworkFilter.or(osgiServiceFilter);
            Optional<String> subContextOptional = Optional.ofNullable(nameComponents.nextElement());
            isValid = subContextOptional.filter(orFilter).isPresent();

            if (!isValid && subContextOptional.filter("osgi:servicelist"::equals).isPresent()) {
                throw new OperationNotSupportedException(SM.getString("osgiUrl.unsupportedURL", this.toString()));
            }
        }
        return isValid;
    }
}
