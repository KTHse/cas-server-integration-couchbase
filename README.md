cas-server-integration-couchbase
================================

Couchbase integration, ticket and service registries, for the Jasig CAS,
(http://www.jasig.org/cas) server. Couchbase, (http://www.couchbase.com), 
is a high availabiltiy, open source NoSQL database server based on 
Erlang/OTP (http://www.erlang.org) and its mnesia database.

The intention of the cas-server-integration-couchbase module is to leverage
the capability of Couchbase server to provide a high availability Jasig CAS server.


## Maven configuration to use this module ##

Add the repo and the dependency to their respective blocks.

```xml
  <repositories>
    <repository>
      <id>kth-infosys</id>
      <name>KTH Infosys Maven Repository</name>
      <url>https://github.com/KTHse/mvn-repo/raw/master/releases/</url>
    </repository>
  </repositories>

  <dependencies>
    <dependency>
      <groupId>se.kth.infosys</groupId>
      <artifactId>cas-server-integration-couchbase</artifactId>
      <version>2.0.0</version>
    </dependency>
  <dependencies>
```


## Configuration and usage of the module ##

### Couchbase server configuration ###

The Couchbase integration currently assumes that the ticket and server registries are stored
in their own vBuckets. Hence create one vBucket each for the CouchbaseServiceRegistryDaoImpl
and the CouchbaseTicketRegistry. Optionally set passwords for the buckets, optionally setup
redundancy and replication as per normal Couchbase configuration.


### Configuration of Service Registry, deployerConfig.xml ###

The service registry can be created with the following configuration in deployerConfig.xml,
replacing the existing serviceRegistryDao bean.

```xml
  <bean id="serviceRegistryClientFactory" class="se.kth.infosys.login.couchbase.CouchbaseClientFactory">
    <property name="uris">
      <list>
        <value>SOME COUCHBASE URI</value>
      </list>
    </property>
    <property name="bucket" value="SOME SERVICE REGISTRY BUCKET" />
  </bean>

  <bean id="serviceRegistryDao"
    class="se.kth.infosys.login.couchbase.CouchbaseServiceRegistryDaoImpl"
    init-method="initialize"
    destroy-method="destroy">

    <property name="couchbase" ref="serviceRegistryClientFactory" />
  </bean>
```


### Configuration of Ticket Registry, ticketRegistry.xml ###

Replace the ticketRegistry.xml file with the following.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:p="http://www.springframework.org/schema/p"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans-3.2.xsd">

  <bean id="ticketRegistryClientFactory" class="se.kth.infosys.login.couchbase.CouchbaseClientFactory">
    <property name="uris">
      <list>
        <value>SOME COUCHBASE URI</value>
      </list>
    </property>          
    <property name="bucket" value="SOME TICKET REGISTRY BUCKET" />
  </bean>
  
  <bean id="ticketRegistry"
    class="se.kth.infosys.login.couchbase.CouchbaseTicketRegistry"
    init-method="initialize"
    destroy-method="destroy">

    <property name="couchbase" ref="ticketRegistryClientFactory" />
    <property name="tgtTimeout" value="${tgt.maxTimeToLiveInSeconds:28800}" />
    <property name="stTimeout" value="${st.timeToKillInSeconds:10}" />
  </bean>
</beans>
```

### Configuration of the management webapp in CAS 4.0 ###

The CAS management webapp needs the same service registry configuration as the CAS server,
the configuration is found in managementConfigContext.xml. However, the management webapp 
does not reload the service information periodically, as the CAS server does, which
causes some issues with the asynchronous setup used in this module. Hence the management
webapp needs to be setup in the same way as the CAS server with the additional configuration

```xml
<bean id="serviceRegistryReloaderJobDetail"
    class="org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean"
    p:targetObject-ref="servicesManager"
    p:targetMethod="reload"/>

<bean id="periodicServiceRegistryReloaderTrigger" class="org.springframework.scheduling.quartz.SimpleTriggerBean"
      p:jobDetail-ref="serviceRegistryReloaderJobDetail"
      p:startDelay="${service.registry.quartz.reloader.startDelay:120000}"
      p:repeatInterval="${service.registry.quartz.reloader.repeatInterval:120000}"/>

<bean class="org.springframework.scheduling.quartz.SchedulerFactoryBean">
  <property name="triggers">
    <list>
      <ref bean="periodicServiceRegistryReloaderTrigger"/>
    </list>
  </property>
</bean>
```

### More about configuration of the CouchbaseClientFactory ###

The only truly mandatory setting of the CouchbaseClientFactory is the list of URIs.
The other settings are optional, but the registers are designed to be stored in buckets
of their own as mentioned above, so in reality the bucket property must also be set.


#### Properties ####

* `uris` _Required_. List of URIs to the Couchbase servers.
* `bucket` _Optional (well, sort of)_. Name of the bucket to use for this client.
* `password` _Optional_. The optional password set for the bucket.


### More about configuration of the CouchbaseTicketRegistry ###

The CouchbaseTicketRegistry can optionally take a parameter `registeredServices` 
containing a list of pre-registered services to create in the registry at system
startup.


## Status of the project ##

Currently a non-redundant server running Couchbase as a backend for the 
MemcacheTicketRegistry is in production at KTH.

The server based on Couchbase is currently in development and development testing at KTH.
It will soon be deployed for integration testing in a larger reference environment.
Later this spring (2013) it is expected to deploy the couchbase aware server to production 
at KTH. Around that time we will start to test redundant configurations using this server
in the reference environment for future deployment in production.  


## Code management ##

This project uses git and git flow style branching. Hence, the master branch 
is the stable release branch, and development is done on the development 
branch. For more information about the branch model see 
http://nvie.com/posts/a-successful-git-branching-model/.
For the `git flow` command line tool, see https://github.com/nvie/gitflow.

Branch version numbering follows the [Semantic versioning](http://semver.org) 
approach.


## License and acknowledgements ##


This module is released under the Apache Licens v2, see the file LICENCE.md
for details.
