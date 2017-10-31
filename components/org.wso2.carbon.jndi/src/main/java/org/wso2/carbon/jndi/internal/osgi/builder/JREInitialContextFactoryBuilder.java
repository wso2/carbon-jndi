/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.jndi.internal.osgi.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;

/**
 * An implementation of {@code InitialContextFactoryBuilder} interface which, is capable of loading any
 * InitialContextFactory classes of the JRE providers.
 */
public class JREInitialContextFactoryBuilder implements InitialContextFactoryBuilder {
    private static final Logger logger = LoggerFactory.getLogger(JREInitialContextFactoryBuilder.class);

    @Override
    public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
        String contextFactoryClass = (String) environment.get(Context.INITIAL_CONTEXT_FACTORY);
        if (contextFactoryClass != null) {
            try {
                Class clazz = getClass().getClassLoader().loadClass(contextFactoryClass);
                return (InitialContextFactory) clazz.newInstance();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                return null;
            }
        }
        return null;
    }
}
