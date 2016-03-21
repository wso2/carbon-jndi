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
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;

/**
 * URLContextFactory for osgi: namespace.
 */
public class OSGiUrlContextFactory implements ObjectFactory {
    private BundleContext callerContext;

    public OSGiUrlContextFactory(BundleContext callerContext) {
        this.callerContext = callerContext;
    }

    /**
     * Create a new Context instance.
     *
     * @param obj         The possibly null object containing location or reference
     *                    information that can be used in creating an object.
     * @param name        The name of this object relative to <code>nameCtx</code>,
     *                    or null if no name is specified.
     * @param nameCtx     The context relative to which the <code>name</code>
     *                    parameter is specified, or null if <code>name</code> is
     *                    relative to the default initial context.
     * @param environment The possibly null environment that is used in
     *                    creating the object.
     * @return The object created; null if an object cannot be created.
     * @throws Exception if this object factory encountered an exception
     *                   while attempting to create an object, and no other object factories are
     *                   to be tried.
     */
    @Override
    public Object getObjectInstance(Object obj,
                                    Name name,
                                    Context nameCtx,
                                    Hashtable<?, ?> environment) throws Exception {
        return new OSGiUrlContext(callerContext, environment);
    }
}
