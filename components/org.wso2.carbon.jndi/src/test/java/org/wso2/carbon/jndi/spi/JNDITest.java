/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.jndi.spi;

import org.testng.annotations.Test;
import org.wso2.carbon.jndi.internal.spi.builder.DefaultContextFactoryBuilder;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;

import static org.testng.Assert.assertEquals;

/**
 * This class is used to test In-memory JNDI context provider in non-OSGi environment.
 */
public class JNDITest {

    @Test public void testInMemoryJNDIContextProvider() throws NamingException {

        //InitialContextFactoryBuilder has to be set to NamingManager to use InitialContext API
        NamingManager.setInitialContextFactoryBuilder(new DefaultContextFactoryBuilder());

        //Binding an object and retrieving it using jndi lookup
        Context context = new InitialContext();
        context.createSubcontext("java:comp");
        context.bind("java:comp/name", "test1");
        String name = (String) context.lookup("java:comp/name");
        assertEquals(name, "test1", "Value not found in JNDI");

        NamingEnumeration namingEnumeration = context.list("java:comp");
        namingEnumeration.hasMore();
        namingEnumeration.next();

        namingEnumeration = context.listBindings("java:comp");
        namingEnumeration.hasMore();
        namingEnumeration.next();

        context.rebind("java:comp/name", "test2");
        name = (String) context.lookup("java:comp/name");
        assertEquals(name, "test2", "Value not found in JNDI");

        context.rename("java:comp", "java:comp1");
        name = (String) context.lookup("java:comp1/name");
        assertEquals(name, "test2", "Value not found in JNDI");

    }

}
