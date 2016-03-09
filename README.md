## Carbon JNDI

Carbon JNDI project provides an In-memory JNDI service provider implementation as well as an implementation of the [OSGi](https://www.osgi.org/) JNDI Service specification.

## Features:

* In-memory JNDI service provider implementation.
* OSGi JNDI Service specification implementation.
* Mechanism to plug in custom InitialContextFactory and ObjectFactories in an OSGi environment.

## Getting Started

A client bundle which needs to use JNDI in OSGi should use the JNDI Context Manager service. Creating an InitialContext using new InitialContext() method is not recommended in OSGi environments due to class loading complexities.

### 1) Creating an InitialContext from the JNDIContextManager service

```java
ServiceReference<JNDIContextManager> contextManagerSRef = bundleContext.getServiceReference(
        JNDIContextManager.class);

JNDIContextManager jndiContextManager = Optional.ofNullable(contextManagerSRef)
                .map(bundleContext::getService)
                .orElseThrow(() -> new RuntimeException("JNDIContextManager service is not available."));

Context initialContext = jndiContextManager.newInitialContext();

DataSource dataSource = (DataSource) initialContext.lookup("java:comp/env/jdbc/wso2carbonDB");
```

### 2) Creating an InitialContext from the traditional client API

This way of creating the InitialContext is also supported by the OSGi JNDI Service specification.
  
```java
InitialContext initialContext = new InitialContext();  

Context envContext = initialContext.createSubcontext("java:comp/env");

DataSource dataSource = (DataSource) envContext.lookup("jdbc/wso2carbonDB");
```

For full source code, see [Carbon JNDI samples] (samples).

## Download 

Use Maven snippet:
````xml
<dependency>
    <groupId>org.wso2.carbon.jndi</groupId>
    <artifactId>org.wso2.carbon.jndi</artifactId>
    <version>${carbon.jndi.version}</version>
</dependency>
````

### Snapshot Releases
````xml
<repository>
    <id>wso2.snapshots</id>
    <name>WSO2 Snapshot Repository</name>
    <url>http://maven.wso2.org/nexus/content/repositories/snapshots/</url>
    <snapshots>
        <enabled>true</enabled>
        <updatePolicy>daily</updatePolicy>
    </snapshots>
    <releases>
        <enabled>false</enabled>
    </releases>
</repository>
````

### Released Versions
````xml
<repository>
    <id>wso2.releases</id>
    <name>WSO2 Releases Repository</name>
    <url>http://maven.wso2.org/nexus/content/repositories/releases/</url>
    <releases>
        <enabled>true</enabled>
        <updatePolicy>daily</updatePolicy>
        <checksumPolicy>ignore</checksumPolicy>
    </releases>
</repository>
````
