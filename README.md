# JNDI InitialContext Implementation for WSO2 Carbon

This module contains

In-memory JNDI service provider
OSGi JNDI Services Implementation.

## Usage

Example usage:

### How to create an InitialContext from the JNDIContextManager service.

```java
JNDIContextManager jndiContextManager;

        ServiceReference<JNDIContextManager> contextManagerSRef = bundleContext.getServiceReference(
                JNDIContextManager.class);

        jndiContextManager = Optional.ofNullable(contextManagerSRef)
                .map(bundleContext::getService)
                .orElseThrow(() -> new RuntimeException("JNDIContextManager service is not available."));

        Context initialContext = jndiContextManager.newInitialContext();

```