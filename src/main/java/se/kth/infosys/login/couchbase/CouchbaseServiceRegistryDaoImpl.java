package se.kth.infosys.login.couchbase;

/*
 * Copyright (C) 2013 KTH, Kungliga tekniska hogskolan, http://www.kth.se
 *
 * This file is part of cas-server-integration-couchbase.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.jasig.cas.services.AbstractRegisteredService;
import org.jasig.cas.services.RegisteredService;
import org.jasig.cas.services.ServiceRegistryDao;
import org.jasig.cas.util.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.protocol.views.Query;
import com.couchbase.client.protocol.views.View;
import com.couchbase.client.protocol.views.ViewDesign;
import com.couchbase.client.protocol.views.ViewResponse;
import com.couchbase.client.protocol.views.ViewRow;

/**
 * A Service Registry storage backend which uses the memcached protocol.
 * This may seem like a weird idea until you realize that CouchBase is a
 * multi host NoSQL database with a memcached interface to persistent
 * storage which also is quite usable as a replicated tickage storage
 * engine for multiple front end CAS servers.
 * 
 * @author Fredrik Jönsson "fjo@kth.se"
 * @since 4.1
 */
public final class CouchbaseServiceRegistryDaoImpl extends TimerTask implements ServiceRegistryDao {
    private static final Timer TIMER = new Timer();
    private static final long RETRY_INTERVAL = 10;

    /*
     * Views, or indexes, in the database.
     */
    private static final ViewDesign ALL_SERVICES_VIEW = new ViewDesign(
            "all_services",
            "function(d,m) {if (!isNaN(m.id)) {emit(m.id);}}");
    private static final List<ViewDesign> ALL_VIEWS = Arrays.asList(new ViewDesign[] {
            ALL_SERVICES_VIEW
    });
    private static final String UTIL_DOCUMENT = "utils";

    private final Logger logger = LoggerFactory.getLogger(CouchbaseServiceRegistryDaoImpl.class);

    /* Couchbase client factory */
    @NotNull
    private CouchbaseClientFactory couchbase;

    /* List of statically configured services, to be used at bean instantiation. */
    private final List<RegisteredService> registeredServices = new LinkedList<RegisteredService>();

    /* Initial service id for added services. */
    private int initialId;

    private final JsonSerializer<RegisteredService> registeredServiceJsonSerializer;

    /**
     * Default constructor.
     * @param registeredServiceJsonSerializer the JSON serializer to use.
     */
    public CouchbaseServiceRegistryDaoImpl(final JsonSerializer<RegisteredService> registeredServiceJsonSerializer) {
        this.registeredServiceJsonSerializer = registeredServiceJsonSerializer;
    }


    /** 
     * {@inheritDoc}
     */
    @Override
    public RegisteredService save(final RegisteredService registeredService) {
        logger.debug("Saving service {}", registeredService);

        final StringWriter stringWriter = new StringWriter();
        registeredServiceJsonSerializer.toJson(stringWriter, registeredService);
        
        if (registeredService.getId() == RegisteredService.INITIAL_IDENTIFIER_VALUE) {
            final long id = couchbase.getClient().incr("LAST_ID", 1, initialId);
            ((AbstractRegisteredService) registeredService).setId(id);
        }

        couchbase.getClient().set(
                String.valueOf(registeredService.getId()), 
                0,
                stringWriter.toString());
        return registeredService;
    }


    /** 
     * {@inheritDoc}
     */
    @Override
    public boolean delete(final RegisteredService registeredService) {
        logger.debug("Deleting service {}", registeredService);
        couchbase.getClient().delete(String.valueOf(registeredService.getId()));
        return true;
    }


    /** 
     * {@inheritDoc}
     */
    @Override
    public List<RegisteredService> load() {
        try {
            logger.debug("Loading services");

            final CouchbaseClient client = couchbase.getClient();
            final View allKeys = client.getView(UTIL_DOCUMENT, ALL_SERVICES_VIEW.getName());
            final Query query = new Query();
            query.setIncludeDocs(true);
            final ViewResponse response = client.query(allKeys, query);
            final Iterator<ViewRow> iterator = response.iterator();

            final List<RegisteredService> services = new LinkedList<RegisteredService>();
            while (iterator.hasNext()) {
                final String json = (String) iterator.next().getDocument();
                logger.debug("Found service: {}", json);
                
                final StringReader stringReader = new StringReader(json);
                services.add(registeredServiceJsonSerializer.fromJson(stringReader));
            }
            return services;
        } catch (final RuntimeException e) {
            logger.warn("Unable to load services.", e.getMessage());
            return new LinkedList<RegisteredService>();
        }
    }


    /** 
     * {@inheritDoc}
     */
    @Override
    public RegisteredService findServiceById(final long id) {
        try {
            logger.debug("Lookup for service {}", id);
            final String json = (String) couchbase.getClient().get(String.valueOf(id));
            final StringReader stringReader = new StringReader(json);
            return registeredServiceJsonSerializer.fromJson(stringReader);
        } catch (final Exception e) {
            logger.error("Unable to get registered service", e);
            return null;
        }
    }


    /**
     * Used to initialize static services from configuration.
     * 
     * @param services List of RegisteredService objects to register.
     */
    public void setRegisteredServices(final List<RegisteredService> services) {
        this.registeredServices.addAll(services);
        this.initialId = services.size();
        TIMER.scheduleAtFixedRate(this, new Date(), TimeUnit.SECONDS.toMillis(RETRY_INTERVAL));
    }


    /**
     * Starts the couchbase client and initialization task.
     */
    public void initialize() {
        couchbase.ensureIndexes(UTIL_DOCUMENT, ALL_VIEWS);
        couchbase.initialize();
    }


    /**
     * Stops the couchbase client and cancels the initialization task if uncompleted.
     * @throws Exception on errors.
     */
    public void destroy() throws Exception {
        TIMER.cancel();
        TIMER.purge();
        couchbase.shutdown();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        try {
            for (final RegisteredService service : registeredServices) {
                save(service);
            }
            TIMER.cancel();
            logger.debug("Stored pre configured services from XML in registry.");
        } catch (final RuntimeException e) {
            logger.error("Unable to save pre configured services, retrying...", e);
        }
    }


    /**
     * @param couchbase client factory to use.
     */
    public void setCouchbase(final CouchbaseClientFactory couchbase) {
        this.couchbase = couchbase;
    }
}
