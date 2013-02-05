package se.kth.infosys.login.couchbase;

/*
    Copyright (C) 2013 KTH, Kungliga tekniska hogskolan, http://www.kth.se
    
    Derived from work with the following copyright:

    Copyright 2010, JA-SIG, Inc., http://www.jasig.org/

    This file is part of cas-server-integration-couchbase.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
        http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.jasig.cas.monitor.AbstractCacheMonitor;
import org.jasig.cas.monitor.CacheStatistics;
import org.jasig.cas.monitor.CacheStatus;
import org.jasig.cas.monitor.SimpleCacheStatistics;
import org.jasig.cas.monitor.StatusCode;

/**
 * Monitors the couchbase hosts known to an instance of {@link CouchbaseClientFactory}.
 * TODO: This is a naive port of MemcachedMonitor. Should be amended.
 */
public class CouchbaseMonitor extends AbstractCacheMonitor {

    @NotNull
    private final CouchbaseClientFactory couchbase;


    /**
     * Creates a new monitor that observes the given couchbase client.
     *
     * @param client Couchbase client factory.
     */
    public CouchbaseMonitor(final CouchbaseClientFactory couchbase) {
        this.couchbase = couchbase;
    }


    /**
     * Supersede the default cache status algorithm by considering unavailable couchbase nodes above cache statistics.
     * If all nodes are unavailable, raise an error; if one or more nodes are unavailable, raise a warning; otherwise
     * delegate to examination of cache statistics.
     *
     * @return Cache status descriptor.
     */
    public CacheStatus observe() {
        if (couchbase.getClient().getAvailableServers().size() == 0) {
            return new CacheStatus(StatusCode.ERROR, "No couchbase servers available.");
        }
        final Collection<SocketAddress> unavailableList = couchbase.getClient().getUnavailableServers();
        final CacheStatus status;
        if (unavailableList.size() > 0) {
            final String description = "One or more couchbase servers is unavailable: " + unavailableList;
            status = new CacheStatus(StatusCode.WARN, description, getStatistics());
        } else {
            status = super.observe();
        }
        return status;
    }


    /**
     * Get cache statistics for all couchbase hosts known to {@link CouchbaseClientFactory}.
     *
     * @return Statistics for all available hosts.
     */
    protected CacheStatistics[] getStatistics() {
        long evictions;
        long size;
        long capacity;
        String name;
        Map<String, String> statsMap;
        final Map<SocketAddress, Map<String, String>> allStats = couchbase.getClient().getStats();
        final List<CacheStatistics> statsList = new ArrayList<CacheStatistics>();
        for (final SocketAddress address : allStats.keySet()) {
            statsMap = allStats.get(address);
            if (statsMap.size() > 0) {
                size = Long.parseLong(statsMap.get("bytes"));
                capacity = Long.parseLong(statsMap.get("limit_maxbytes"));
                evictions = Long.parseLong(statsMap.get("evictions"));
                if (address instanceof InetSocketAddress) {
                    name = ((InetSocketAddress) address).getHostName();
                } else {
                    name = address.toString();
                }
                statsList.add(new SimpleCacheStatistics(size, capacity, evictions, name));
            }
        }
        return statsList.toArray(new CacheStatistics[statsList.size()]);
    }
}