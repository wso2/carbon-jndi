# Carbon JNDI SPI Sample

This is a sample java client application which uses WSO2 carbon-jndi In-Memory JNDI context service provider service to store objects using in-memory context in non-OSGi environment.

## Using carbon-jndi service in non-OSGi environment

The `InMemoryInitialContextFactory` JNDI service provider makes use of `NamingContext` to store objects to in-memory JNDI context.
The In-memory JNDI context can be created using InitialContext API via carbon-jndi using one of the following 2 ways:

1. The InitialContextFactoryBuilder implementation `org.wso2.carbon.jndi.internal.spi.builder.DefaultContextFactoryBuilder`, defined in carbon-jndi to be used in non-OSGi environment, has to be set to NamingManager to use InitialContext API. It will by default use the `InMemoryInitialContextFactory` to create initial context when Context.INITIAL_CONTEXT_FACTORY is not defined.
2. Creating JNDI context using InitialContext API with Context.INITIAL_CONTEXT_FACTORY set to `InMemoryInitialContextFactory`. 

## Instructions to build and execute the application
1. Build the application
    `mvn clean install`
    
2. Run the application
    `java -jar target/jndi-spi-sample-<version>.jar`
   Ex: `java -jar target/jndi-spi-sample-1.0.2-SNAPSHOT.jar`