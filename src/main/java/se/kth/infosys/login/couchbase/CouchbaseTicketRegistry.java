package se.kth.infosys.login.couchbase;

/*
 * Copyright (C) 2015 KTH, Kungliga tekniska hogskolan, http://www.kth.se
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.jasig.cas.monitor.TicketRegistryState;
import org.jasig.cas.ticket.ServiceTicket;
import org.jasig.cas.ticket.ServiceTicketImpl;
import org.jasig.cas.ticket.Ticket;
import org.jasig.cas.ticket.TicketGrantingTicket;
import org.jasig.cas.ticket.TicketGrantingTicketImpl;
import org.jasig.cas.ticket.registry.AbstractDistributedTicketRegistry;

import com.couchbase.client.java.document.SerializableDocument;
import com.couchbase.client.java.view.DefaultView;
import com.couchbase.client.java.view.View;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;


/**
 * A Ticket Registry storage backend which uses the memcached protocol.
 * CouchBase is a multi host NoSQL database with a memcached interface
 * to persistent storage which also is quite usable as a replicated
 * tickage storage engine for multiple front end CAS servers.
 * 
 * @author Fredrik Jönsson "fjo@kth.se"
 * @since 4.0
 */
public final class CouchbaseTicketRegistry extends AbstractDistributedTicketRegistry implements TicketRegistryState {
    private static final String END_TOKEN = "\u02ad";
    /*
     * Views, or indexes, in the database. 
     */
    private static final View ALL_TICKETS_VIEW = DefaultView.create(
            "all_tickets", 
            "function(d,m) {emit(m.id);}",
            "_count");
    private static final List<View> ALL_VIEWS = Arrays.asList(new View[] {
            ALL_TICKETS_VIEW
    });
    private static final String UTIL_DOCUMENT = "statistics";

    /* Couchbase client factory */
    @NotNull
    private CouchbaseClientFactory couchbase;

    @Min(0)
    private int tgtTimeout;

    @Min(0)
    private int stTimeout;

    /**
     * Default constructor.
     */
    public CouchbaseTicketRegistry() {}


    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateTicket(final Ticket ticket) {
        logger.debug("Updating ticket {}", ticket);
        try {
            final SerializableDocument document = 
                    SerializableDocument.create(ticket.getId(), getTimeout(ticket), ticket);
            couchbase.bucket().upsert(document);
        } catch (final Exception e) {
            logger.error("Failed updating {}: {}", ticket, e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addTicket(final Ticket ticket) {
        logger.debug("Adding ticket {}", ticket);
        try {
            final SerializableDocument document = 
                    SerializableDocument.create(ticket.getId(), getTimeout(ticket), ticket);
            couchbase.bucket().upsert(document);
        } catch (final Exception e) {
            logger.error("Failed adding {}: {}", ticket, e);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteTicket(final String ticketId) {
        logger.debug("Deleting ticket {}", ticketId);
        try {
            couchbase.bucket().remove(ticketId);
            return true;
        } catch (final Exception e) {
            logger.error("Failed deleting {}: {}", ticketId, e);
            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Ticket getTicket(final String ticketId) {
        try {
            final Ticket t = (Ticket) couchbase.bucket().get(ticketId, SerializableDocument.class).content();
            if (t != null) {
                return getProxiedTicketInstance(t);
            }
        } catch (final Exception e) {
            logger.error("Failed fetching {}: {}", ticketId, e);
        }
        return null;
    }


    /**
     * Starts the couchbase client.
     */
    public void initialize() {
        couchbase.ensureIndexes(UTIL_DOCUMENT, ALL_VIEWS);
        couchbase.initialize();
    }


    /**
     * Stops the couchbase client.
     * @throws Exception on errors.
     */
    public void destroy() throws Exception {
        couchbase.shutdown();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean needsCallback() {
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Ticket> getTickets() {
        throw new UnsupportedOperationException("GetTickets not supported.");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int sessionCount() {
        return runQuery(TicketGrantingTicketImpl.PREFIX + "-");
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int serviceTicketCount() {
        return runQuery(ServiceTicketImpl.PREFIX + "-");
    }


    /**
     * Run the statistics query.
     * @param prefix the ticket prefix to get statistics for.
     * @return the number of tickets.
     */
    private int runQuery(final String prefix) {
        final ViewResult allKeys = couchbase.bucket().query(
                ViewQuery.from(UTIL_DOCUMENT, "all_tickets")
                    .startKey(prefix)
                    .endKey(prefix + END_TOKEN)
                    .reduce());
        return getCountFromView(allKeys);
    }


    /**
     * Sets the time after which a ticket granting ticket will be
     * purged from the registry.
     * 
     * @param tgtTimeout Ticket granting ticket timeout in seconds.
     */
    public void setTgtTimeout(final int tgtTimeout) {
        this.tgtTimeout = tgtTimeout;
    }


    /**
     * Sets the time after which a session ticket will be purged
     * from the registry.
     * 
     * @param stTimeout Session ticket timeout in seconds.
     */
    public void setStTimeout(final int stTimeout) {
        this.stTimeout = stTimeout;
    }


    /**
     * @param t a CAS ticket.
     * @return the ticket timeout for the ticket in the registry.
     */
    private int getTimeout(final Ticket t) {
        if (t instanceof TicketGrantingTicket) {
            return tgtTimeout;
        } else if (t instanceof ServiceTicket) {
            return stTimeout;
        }
        throw new IllegalArgumentException("Invalid ticket type");
    }


    /**
     * @param couchbase the client factory to use.
     */
    public void setCouchbase(final CouchbaseClientFactory couchbase) {
        this.couchbase = couchbase;
    }


    /**
     * Returns the number of elements in view.
     * @param result a couchbase ViewResult.
     * @return number of items in view as reported by couchbase.
     */
    private int getCountFromView(final ViewResult result) {
        final Iterator<ViewRow> iterator = result.iterator();
        if (iterator.hasNext()) {
            final ViewRow res = iterator.next();
            return (Integer) res.value();
        } else {
            return 0;
        }
    }
}
