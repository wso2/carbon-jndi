package org.wso2.carbon.jndi.internal.osgi;

import org.osgi.framework.BundleContext;

import javax.naming.Binding;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import java.util.Map;

/**
 * JNDI context implementation for handling osgi:servicelist lookup.
 */
public class OSGiUrlListContext extends AbstractOSGiUrlContext {

    public OSGiUrlListContext(BundleContext callerContext, Map<String, Object> environment, Name validName) {
        super(callerContext, environment, validName);
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException {
        OSGiName osGiName = new OSGiName(name);
        Object result = findService(callerContext, osGiName, name, env);
        if (result == null) {
            throw new NameNotFoundException(name.toString());
        }
        return result;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        return null;
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        return null;  //todo
    }

    @Override
    public Name composeName(Name name, Name prefix) throws NamingException {
        return null;
    }

    @Override
    public String composeName(String name, String prefix) throws NamingException {
        return null;
    }

    @Override
    public void close() throws NamingException {

    }

    @Override
    public String getNameInNamespace() throws NamingException {
        return null;
    }
}
