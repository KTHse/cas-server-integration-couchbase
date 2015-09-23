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

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.view.DesignDocument;
import com.couchbase.client.java.view.View;

/**
 * A factory class which produces a client for a particular Couchbase bucket.
 * A design consideration was that we want the server to start even if Couchbase
 * is unavailable, picking up the connection when Couchbase comes online. Hence
 * the creation of the client is made using a scheduled task which is repeated
 * until successful connection is made.
 * 
 * @author Fredrik Jönsson "fjo@kth.se"
 * @since 4.0
 */
public class CouchbaseClientFactory extends TimerTask {
    private static final int RETRY_INTERVAL = 10; // seconds.

    private final Logger logger = LoggerFactory.getLogger(CouchbaseClientFactory.class);
    private final Timer timer = new Timer();

    private Cluster cluster;

    @NotNull
    private List<String> nodes;

    /* The name of the bucket, will use the default bucket unless otherwise specified. */
    private String bucketName = "default";
    private Bucket bucket;

    /* Password for the bucket if any. */
    private String password = "";

    /* Design document and views to create in the bucket, if any. */
    private String designDocument;
    private List<View> views;


    /**
     * Default constructor. 
     */
    public CouchbaseClientFactory() {}


    /**
     * Start initializing the client. This will schedule a task that retries
     * connection until successful.
     */
    public void initialize() {
        timer.scheduleAtFixedRate(this, new Date(), TimeUnit.SECONDS.toMillis(RETRY_INTERVAL));
    }


    /**
     * Inverse of initialize, shuts down the client, cancelling connection
     * task if not completed.
     * 
     * @throws Exception on errors.
     */
    public void shutdown() throws Exception {
        timer.cancel();
        timer.purge();
        if (cluster != null) {
            cluster.disconnect();
        }
    }


    /**
     * Retrieve the Couchbase bucket.
     * 
     * @return the bucket.
     */
    public Bucket bucket() {
        if (bucket != null) {
            return bucket;
        } else {
            throw new RuntimeException("Conncetion to bucket " + bucketName + " not initialized yet.");
        }
    }


    /**
     * Register indexes to ensure in the bucket when the client is initialized.
     * 
     * @param documentName name of the Couchbase design document.
     * @param views the list of Couchbase views (i.e. indexes) to create in the document.
     */
    public void ensureIndexes(final String documentName, final List<View> views) {
        this.designDocument = documentName;
        this.views = views;
    }


    /**
     * Ensures that all views exists in the database.
     * 
     * @param documentName the name of the design document.
     * @param views the views to ensure exists in the database.
     */
    private void doEnsureIndexes(final String documentName, final List<View> views) {
        logger.debug("Ensure that indexes exist in bucket {}.", bucket.name());
        final DesignDocument newDocument = DesignDocument.create(documentName, views);
        final DesignDocument document = bucket.bucketManager().getDesignDocument(documentName);
        if (!newDocument.equals(document)) {
            logger.warn("Missing indexes in bucket {} for document {}, creating new.", bucket.name(), documentName);
            bucket.bucketManager().upsertDesignDocument(newDocument);
        }
    }


    /**
     * Task to initialize the Couchbase client.
     */
    public void run() {
        try {
            logger.info("Trying to connect to couchbase bucket {}", bucketName);
            cluster = CouchbaseCluster.create(nodes);
            bucket = cluster.openBucket(bucketName, password);

            logger.info("Connected to Couchbase bucket {}.", bucketName);

            if (views != null) {
                doEnsureIndexes(designDocument, views);
            }
            timer.cancel();
        } catch (final Exception e) {
            logger.error("Failed to connect to Couchbase bucket {}: {}, retrying...", bucketName, e);
        }
    }

    public void setNodes(final List<String> nodes) {
        this.nodes = nodes;
    }

    public void setBucket(final String bucket) {
        this.bucketName = bucket;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
