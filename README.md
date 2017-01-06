## Carbon JNDI

Carbon JNDI project provides an In-memory JNDI service provider implementation as well as an implementation of the [OSGi](https://www.osgi.org/) JNDI Service specification. 

The Java Naming and Directory Interface (JNDI) is a registry technology in Java applications, both in the Java SE and Java EE space. JNDI provides a vendor-neutral set of APIs that allow clients to interact with a naming service from different vendors.

Usually JNDI usages in Java SE heavily depends on the single flat classpath model provided by JDK. e.g. JNDI providers are loaded using the Thread context class loader. This approach does not work or not suitable for OSGi environments because this creates a dependency between JNDI client and the JNDI provider implementation. This breaks modularity defined in OSGi. Therefore OSGi JNDI service specification define following models to resolve this issue.
  
* OSGi Service Model    - How clients interact with JNDI when running inside an OSGi Framework.
* JNDI Provider Model   - How JNDI providers can advertise their existence so they are available to OSGi and traditional clients.
* Traditional Model     - How traditional JNDI applications and providers can continue to work in an OSGi Framework without needing to be rewritten when certain precautions are taken.
* JNDI SPI Model        - How the JNDI application can be used in non-OSGi/traditional applications.

## Features:

* In-memory JNDI service provider implementation which can be used in both OSGi and non-OSGi environment.
* OSGi JNDI Service specification implementation.
* Mechanism to plug in custom InitialContextFactory and ObjectFactories in an OSGi environment.

## Getting Started

### Using Carbon JNDI in OSGi environment

A client bundle which needs to use JNDI in OSGi should use the JNDI Context Manager service. Creating an InitialContext using new InitialContext() method is not recommended in OSGi environments due to class loading complexities.

#### 1) Creating an InitialContext from the JNDIContextManager service

```java
ServiceReference<JNDIContextManager> contextManagerSRef = bundleContext.getServiceReference(
        JNDIContextManager.class);

JNDIContextManager jndiContextManager = Optional.ofNullable(contextManagerSRef)
                .map(bundleContext::getService)
                .orElseThrow(() -> new RuntimeException("JNDIContextManager service is not available."));

Context initialContext = jndiContextManager.newInitialContext();

DataSource dataSource = (DataSource) initialContext.lookup("java:comp/env/jdbc/wso2carbonDB");
```

#### 2) Creating an InitialContext from the traditional client API

This way of creating the InitialContext is also supported by the OSGi JNDI Service specification.
  
```java
InitialContext initialContext = new InitialContext();  

Context envContext = initialContext.createSubcontext("java:comp/env");

DataSource dataSource = (DataSource) envContext.lookup("jdbc/wso2carbonDB");
```

#### 3) Creating InitialContext with declarative services

Following service component retrieves the JNDIContextManager and create InitialContext.

```java
public class ActivatorComponent {
    @Reference(
            name = "org.osgi.service.jndi",
            service = JNDIContextManager.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unbindNDIContextManager"
    )
    protected void bindJNDIContextManager(JNDIContextManager jndiContextManager) throws NamingException {

        Context initialContext = jndiContextManager.newInitialContext();

        DataSource dataSource = (DataSource) initialContext.lookup("java:comp/env/jdbc/wso2carbonDB");
    }

    protected void unbindNDIContextManager(JNDIContextManager jndiContextManager) throws NamingException  {
        jndiContextManager.newInitialContext().close();
    }
}
```

#### OSGI URL Scheme

An OSGI URL scheme is available for users to access services in service registry. This URL scheme can have the format
    osgi:service/<query> or osgi:framework/bundleContext
No spaces are allowed between the terms. Thi OSGi URL scheme can be used to perform lookup of a OSGi service using the
interface and the filter. 
Following is an example lookup for an OSGi service using JNDI

```java

        FooService fooService = new FooServiceImpl();
        
        ServiceRegistration<FooService> fooServiceRegistration = bundleContext.registerService(
                FooService.class, fooService, propertyMap);
                
        Context context = jndiContextManager.newInitialContext();

        Object service = context.lookup("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");
```
Above lookup is equal to accessing the OSGi service from bundle context as below,

```java

        ServiceReference serviceReference = ctx.getServiceReference("org.wso2.carbon.jndi.osgi.services.FooService", filter);
        Object ctxService = ctx.getService(reference);
```
If Multiple services were registered with the same Service class, all services can be obtained calling listBindings method.
Calling the listBindings method will produce a NamingEnumeration object that provides Binding objects. 
A Binding object contains the name, class of the service, and the service object. 

```java
        NamingEnumeration<Binding> listBindings =
                context.listBindings("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");

        if(listBindings.hasMoreElements()) {

        Binding binding = listBindings.nextElement();
        
        }
```        

When the Context class list method is called, the Naming Enumeration object provides a NameClassPair object. 
This NameClassPair object will include the name and class of each service in the Context. The list method can be useful
 in cases where a client wishes to iterate over the available services without actually getting them. 
 If the service itself is required, then listBindings method should be used.
 
 ```java
        NamingEnumeration<NameClassPair> namingEnumeration =
                 context.list("osgi:service/org.wso2.carbon.jndi.osgi.services.FooService");
 
         if(namingEnumeration.hasMoreElements()) {
 
         NameClassPair nameClassPair = namingEnumeration.nextElement();
         
         }

```

### Using Carbon JNDI in non-OSGi environment

Carbon JNDI provides In-memory JNDI context service provider implementation which can be used to bind objects to In-memory JNDI context. The `InMemoryInitialContextFactory` JNDI service provider makes use of `NamingContext` to achieve this.
The In-memory JNDI context can be created using InitialContext API via carbon-jndi using one of the following 2 ways:

* The InitialContextFactoryBuilder implementation `org.wso2.carbon.jndi.internal.spi.builder.DefaultContextFactoryBuilder`, defined in carbon-jndi to be used in non-OSGi environment, has to be set to NamingManager to use InitialContext API. It will by default use the `InMemoryInitialContextFactory` to create initial context when Context.INITIAL_CONTEXT_FACTORY is not defined.

````
        try {
            NamingManager.setInitialContextFactoryBuilder(new DefaultContextFactoryBuilder());

            //Binding an object and retrieving it using jndi lookup
            Context context = new InitialContext();
            context.createSubcontext("java:comp");
            context.bind("java:comp/name", "test");
            logger.info("The name retrieved via jndi lookup: " + (String) context.lookup("java:comp/name"));

        } catch (NamingException e) {
            logger.error("Error occurred while using carbon In-memory JNDI service provider implementation.", e);
        }
````

* Creating JNDI context using InitialContext API with Context.INITIAL_CONTEXT_FACTORY set to `InMemoryInitialContextFactory`.
````
        try {
            Hashtable<String, String> environment = new Hashtable<>();
            environment.put(Context.INITIAL_CONTEXT_FACTORY,
                    "org.wso2.carbon.jndi.internal.InMemoryInitialContextFactory");
            Context context = new InitialContext(environment);
            context.createSubcontext("java:comp");
            context.bind("java:comp/id", "ID1");
            logger.info("The name retrieved using \"java:comp/id\" jndi lookup: " + (String) context
                    .lookup("java:comp/id"));
        } catch (NamingException e) {
            logger.error("Error occurred while using carbon-jndi In-Memory JNDI context service provider", e);
        }
````

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

Use following Maven repository for snapshot versions of Carbon JNDI.

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

Use following Maven repository for released stable versions of Carbon JNDI.

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

## Building From Source

Clone this repository first (`git clone https://github.com/wso2/carbon-jndi.git`) and use Maven install to build `mvn clean install`.

## Contributing to Carbon JNDI Project

Pull requests are highly encouraged and we recommend you to create a GitHub issue to discuss the issue or feature that you are contributing to.  

## License

Carbon JNDI is available under the Apache 2 License.

## Copyright

Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
