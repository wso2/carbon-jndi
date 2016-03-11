package org.wso2.carbon.jndi.internal.osgi;

import javax.naming.CompositeName;

/**
 * A composite name to represent Osgi url scheme.
 */
public class OSGiName extends CompositeName {

    public boolean hasInterface() {
        return size() > 1;
    }

    public boolean hasFilter() {
        return size() > 2;
    }
}
