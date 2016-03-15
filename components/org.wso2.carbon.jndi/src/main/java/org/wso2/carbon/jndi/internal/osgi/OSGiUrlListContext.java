package org.wso2.carbon.jndi.internal.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jndi.JNDIConstants;

import javax.naming.Binding;
import javax.naming.InvalidNameException;
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
    private OSGiName osgiLookupName;

    public OSGiUrlListContext(BundleContext callerContext, Map<String, Object> environment, Name name)
            throws InvalidNameException {
        super(callerContext, environment, name);
        osgiLookupName = new OSGiName(name);
    }

    @Override
    public Object lookup(Name name) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup(String name) throws NamingException {
        Object result = findService(callerContext, osgiLookupName, env);
        if (result == null) {
            throw new NameNotFoundException(name);
        }
        return result;
    }

    @Override
    public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
        return list(name.toString());
    }

    @Override
    public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
        String jndiServiceName = osgiLookupName.getJNDIServiceName();
        ServiceReference[] serviceReferences =
                getServiceReferences(callerContext, osgiLookupName.getInterface(), osgiLookupName.getFilter(), jndiServiceName);
        return new OSGiServiceNamingEnumeration(callerContext, serviceReferences);
    }

    @Override
    public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
        return listBindings(name.toString());
    }

    @Override
    public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
        String jndiServiceName = osgiLookupName.getJNDIServiceName();
        ServiceReference[] serviceReferences =
                getServiceReferences(callerContext, osgiLookupName.getInterface(), osgiLookupName.getFilter(), jndiServiceName);
        return new OSGiServiceBindingsEnumeration(callerContext, serviceReferences);
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

    private ServiceReference[] getServiceReferences(BundleContext ctx, String interfaceName,
                                                    String filter, String serviceName) throws NamingException {
        ServiceReference[] refs = null;

        try {
            refs = ctx.getServiceReferences(interfaceName, filter);

            if (refs == null || refs.length == 0) {
                refs = ctx.getServiceReferences((String) null, "(" + JNDIConstants.JNDI_SERVICENAME + "="
                        + serviceName + ')');
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();          //todo
        }

        return refs;
    }
}
