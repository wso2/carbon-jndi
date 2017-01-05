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
package org.wso2.carbon.jndi.internal.spi.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * This class represents the default context factory which is used by the
 * {@link org.wso2.carbon.jndi.internal.spi.builder.DefaultContextFactoryBuilder}
 * class.
 */
public class DefaultContextFactory implements InitialContextFactory {

    private static final Logger logger = LoggerFactory
            .getLogger(org.wso2.carbon.jndi.internal.spi.factory.DefaultContextFactory.class);

    /**
     * Creates an initial context from the user defined INITIAL_CONTEXT_FACTORY class and if it is not defined
     * creates it using InMemoryInitialContextFactory by default.
     *
     * @param environment The environment to be used in the creation of the initial context.
     * @return A non-null initial context object that implements the Context interface.
     * @throws NamingException If an initial context can't be created.
     */
    @Override public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
        Context initialContext = null;
        String userDefinedICFClassName = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (userDefinedICFClassName != null && !"".equals(userDefinedICFClassName)) {
            try {
                InitialContextFactory ctxFactory = (InitialContextFactory) Class.forName(userDefinedICFClassName)
                        .newInstance();
                initialContext = ctxFactory.getInitialContext(environment);
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                logger.error("Couldn't create initial context using the InitialContextFactory class "
                        + userDefinedICFClassName, e);
            }
        } else {
            //If INITIAL_CONTEXT_FACTORY class is not specified, use InMemoryInitialContextFactory as default
            initialContext = new InMemoryInitialContextFactory().getInitialContext(environment);
        }
        return initialContext;
    }

}
