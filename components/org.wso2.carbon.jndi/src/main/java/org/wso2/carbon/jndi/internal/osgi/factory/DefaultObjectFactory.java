package org.wso2.carbon.jndi.internal.osgi.factory;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

/**
 * Created by nipuni on 3/16/16.  //todo
 */
public class DefaultObjectFactory implements ObjectFactory{

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        return null; //todo  verify
    }
}
