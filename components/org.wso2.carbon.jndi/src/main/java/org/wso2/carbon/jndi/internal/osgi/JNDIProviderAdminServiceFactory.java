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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jndi.JNDIContextManager;
import org.osgi.service.jndi.JNDIProviderAdmin;

/**
 * Created by nipuni on 3/16/16. //todo
 */
public class JNDIProviderAdminServiceFactory implements ServiceFactory<JNDIProviderAdmin> {
    @Override
    public JNDIProviderAdmin getService(Bundle bundle, ServiceRegistration<JNDIProviderAdmin> serviceRegistration) {
        return new JNDIProviderAdminImpl(bundle.getBundleContext(), serviceRegistration);
    }

    @Override
    public void ungetService(Bundle bundle, ServiceRegistration<JNDIProviderAdmin> serviceRegistration, JNDIProviderAdmin jndiProviderAdmin) {

    }
}
