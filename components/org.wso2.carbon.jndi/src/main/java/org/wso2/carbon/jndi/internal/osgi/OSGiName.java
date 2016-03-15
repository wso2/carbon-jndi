package org.wso2.carbon.jndi.internal.osgi;

import javax.naming.CompositeName;
import javax.naming.InvalidNameException;
import javax.naming.Name;
import java.util.Enumeration;

/**
 * A composite name to represent Osgi url scheme.
 */
public class OSGiName extends CompositeName {

    public OSGiName(String name) throws InvalidNameException {
        super(name);
    }

    public OSGiName(Name name) throws InvalidNameException {
        this(name.toString());
    }

    public boolean hasInterface() {
        return size() > 1;
    }

    public boolean hasFilter() {
        //following query will result size()>3 as size() will count the components separated by "/"
        //osgi:service/org.wso2.carbon.jndi.osgi.osgiServices.FooService/(osgi.jndi.service.name=foo/myService)
        return size() == 3;
    }

    public String getJNDIServiceName() {
        StringBuilder builder = new StringBuilder();
        Enumeration<String> parts = getAll();
        parts.nextElement();  //we need to skip the first component (eg:osgi:service)

        if (parts.hasMoreElements()) {
            while (parts.hasMoreElements()) {
                builder.append(parts.nextElement());
                builder.append('/');
            }

            builder.deleteCharAt(builder.length() - 1);  //remove last "/" character
        }

        return builder.toString();
    }

    public String getInterface() {
        return hasInterface() ? get(1) : null;
    }

    public String getFilter() {
        return hasFilter() ? get(2) : null;
    }
}
