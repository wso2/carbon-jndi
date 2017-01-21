/*
 *  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.jndi.sample.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.jndi.internal.osgi.builder.DefaultContextFactoryBuilder;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

/**
 * This client uses carbon-jndi In-Memory JNDI context service provider
 * {@link org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory} in non-OSGi environment.
 */
public class JNDIClient {

    private static Logger logger = LoggerFactory.getLogger(JNDIClient.class);

    public static void main(String[] args) {
        try {
            //The In-memory JNDI context can be created using InitialContext API via carbon-jndi using one of the
            // following 2 ways

            //1. The InitialContextFactoryBuilder implementation (org.wso2.carbon.jndi.internal.spi.builder
            // .DefaultContextFactoryBuilder), defined in carbon-jndi to be used in non-OSGi environment, has to be
            // set to NamingManager to use InitialContext API. It will by default use InMemoryInitialContextFactory
            // to create initial context as Context.INITIAL_CONTEXT_FACTORY is not defined.
            NamingManager.setInitialContextFactoryBuilder(new DefaultContextFactoryBuilder());
            Context context = new InitialContext();
            context.createSubcontext("java:comp");
            context.bind("java:comp/name", "test");
            logger.info("The name retrieved using \"java:comp/name\" jndi lookup: " + (String) context
                    .lookup("java:comp/name"));
            context.rebind("java:comp/name", "test2");
            logger.info("The name retrieved using \"java:comp/name\" jndi lookup after rebinding: " + (String) context
                    .lookup("java:comp/name"));
            context.rename("java:comp", "java:comp1");
            logger.info(
                    "The name retrieved using \"java:comp1/name\" jndi lookup after object renamed: " + (String) context
                            .lookup("java:comp1/name"));
            context.close();

            //2. Creating JNDI context using InitialContext API with Context.INITIAL_CONTEXT_FACTORY defined
            Hashtable<String, String> environment = new Hashtable<>();
            environment.put(Context.INITIAL_CONTEXT_FACTORY,
                    "org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory");
            context = new InitialContext(environment);
            context.createSubcontext("java:comp");
            context.bind("java:comp/id", "ID1");
            logger.info("The name retrieved using \"java:comp/id\" jndi lookup: " + (String) context
                    .lookup("java:comp/id"));
            context.close();

        } catch (NamingException e) {
            logger.error("Error occurred while using carbon-jndi In-Memory JNDI context service provider", e);
        }

    }
}
