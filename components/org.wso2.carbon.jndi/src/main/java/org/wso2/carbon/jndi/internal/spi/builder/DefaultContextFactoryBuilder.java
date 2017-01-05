/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.jndi.internal.spi.builder;

import org.wso2.carbon.jndi.internal.spi.factory.DefaultContextFactory;

import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

/**
 * An implementation of {@link InitialContextFactoryBuilder} interface, which acts as the default initial context
 * factory builder, has to be set in the NamingManager. This is required in non-OSGi environment which uses the
 * InitialContext API.
 */
public class DefaultContextFactoryBuilder implements InitialContextFactoryBuilder {

    @Override public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment)
            throws NamingException {
        return new DefaultContextFactory();
    }
}
