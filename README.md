cas-server-integration-couchbase
================================

Couchbase integration, ticket and service registries, for the CAS server.

## Couchbase server configuration ##

The Couchbase integration currently assumes that the ticket and server registries are stored
in their own vBuckets. Hence create one vBucket each for the CouchbaseServiceRegistryDaoImpl
and the CouchbaseTicketRegistry. Optionally set passwords for the buckets, optionally setup
redundancy and replication as per normal Couchbase configuration.

## Configuration of Service Registry, deployerConfig.xml ##

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

## Configuration of Ticket Registry, ticketRegistry.xml ##

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

## More about configuration of the CouchbaseClientFactory ##

The only truly mandatory setting of the CouchbaseClientFactory is the list of URIs.
The other settings are optional, but the registers are designed to be stored in buckets
of their own as mentioned above, so in reality the bucket property must also be set.

### Properties ###

* `uris` _Required_. List of URIs to the Couchbase servers.
* `bucket` _Optional (well, sort of)_. Name of the bucket to use for this client.
* `password` _Optional_. The optional password set for the bucket.

## More about configuration of the CouchbaseTicketRegistry ##

The CouchbaseTicketRegistry can optionally take a parameter `registeredServices` 
containing a list of pre-registered services to create in the registry at system
startup.
