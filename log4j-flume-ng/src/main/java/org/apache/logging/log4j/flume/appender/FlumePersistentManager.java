/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.flume.appender;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockConflictException;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import org.apache.flume.Event;
import org.apache.flume.event.SimpleEvent;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.util.ExecutorServices;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.Log4jThread;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;
import org.apache.logging.log4j.core.util.SecretKeyProvider;
import org.apache.logging.log4j.util.Strings;

/**
 * Manager that persists data to Berkeley DB before passing it on to Flume.
 */
public class FlumePersistentManager extends FlumeAvroManager {

    /** Attribute name for the key provider. */
    public static final String KEY_PROVIDER = "keyProvider";

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final String DEFAULT_DATA_DIR = ".log4j/flumeData";

    private static final long SHUTDOWN_WAIT_MILLIS = 60000;

    private static final long LOCK_TIMEOUT_SLEEP_MILLIS = 500;

    private static BDBManagerFactory factory = new BDBManagerFactory();

    private final Database database;

    private final Environment environment;

    private final WriterThread worker;

    private final Gate gate = new Gate();

    private final SecretKey secretKey;

    private final int lockTimeoutRetryCount;

    private final ExecutorService threadPool;

    private final AtomicLong dbCount = new AtomicLong();

    /**
     * Constructor
     * @param name The unique name of this manager.
     * @param shortName Original name for the Manager.
     * @param agents An array of Agents.
     * @param batchSize The number of events to include in a batch.
     * @param retries The number of times to retry connecting before giving up.
     * @param connectionTimeout The amount of time to wait for a connection to be established.
     * @param requestTimeout The amount of time to wair for a response to a request.
     * @param delay The amount of time to wait between retries.
     * @param database The database to write to.
     * @param environment The database environment.
     * @param secretKey The SecretKey to use for encryption.
     * @param lockTimeoutRetryCount The number of times to retry a lock timeout.
     */
    protected FlumePersistentManager(final String name, final String shortName, final Agent[] agents,
                                     final int batchSize, final int retries, final int connectionTimeout,
                                     final int requestTimeout, final int delay, final Database database,
                                     final Environment environment, final SecretKey secretKey,
                                     final int lockTimeoutRetryCount) {
        super(name, shortName, agents, batchSize, delay, retries, connectionTimeout, requestTimeout);
        this.database = database;
        this.environment = environment;
        dbCount.set(database.count());
        this.worker = new WriterThread(database, environment, this, gate, batchSize, secretKey, dbCount,
            lockTimeoutRetryCount);
        this.worker.start();
        this.secretKey = secretKey;
        this.threadPool = Executors.newCachedThreadPool(Log4jThreadFactory.createDaemonThreadFactory("Flume"));
        this.lockTimeoutRetryCount = lockTimeoutRetryCount;
    }


    /**
     * Returns a FlumeAvroManager.
     * @param name The name of the manager.
     * @param agents The agents to use.
     * @param properties Properties to pass to the Manager.
     * @param batchSize The number of events to include in a batch.
     * @param retries The number of times to retry connecting before giving up.
     * @param connectionTimeout The amount of time to wait to establish a connection.
     * @param requestTimeout The amount of time to wait for a response to a request.
     * @param delayMillis Amount of time to delay before delivering a batch.
     * @param lockTimeoutRetryCount The number of times to retry after a lock timeout.
     * @param dataDir The location of the Berkeley database.
     * @return A FlumeAvroManager.
     */
    public static FlumePersistentManager getManager(final String name, final Agent[] agents,
                                                    final Property[] properties, int batchSize, final int retries,
                                                    final int connectionTimeout, final int requestTimeout,
                                                    final int delayMillis, final int lockTimeoutRetryCount,
                                                    final String dataDir) {
        if (agents == null || agents.length == 0) {
            throw new IllegalArgumentException("At least one agent is required");
        }

        if (batchSize <= 0) {
            batchSize = 1;
        }
        final String dataDirectory = Strings.isEmpty(dataDir) ? DEFAULT_DATA_DIR : dataDir;

        final StringBuilder sb = new StringBuilder("FlumePersistent[");
        boolean first = true;
        for (final Agent agent : agents) {
            if (!first) {
                sb.append(',');
            }
            sb.append(agent.getHost()).append(':').append(agent.getPort());
            first = false;
        }
        sb.append(']');
        sb.append(' ').append(dataDirectory);
        return getManager(sb.toString(), factory, new FactoryData(name, agents, batchSize, retries,
            connectionTimeout, requestTimeout, delayMillis, lockTimeoutRetryCount, dataDir, properties));
    }

    @Override
    public void send(final Event event)  {
        if (worker.isShutdown()) {
            throw new LoggingException("Unable to record event");
        }

        final Map<String, String> headers = event.getHeaders();
        final byte[] keyData = headers.get(FlumeEvent.GUID).getBytes(UTF8);
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final DataOutputStream daos = new DataOutputStream(baos);
            daos.writeInt(event.getBody().length);
            daos.write(event.getBody(), 0, event.getBody().length);
            daos.writeInt(event.getHeaders().size());
            for (final Map.Entry<String, String> entry : headers.entrySet()) {
                daos.writeUTF(entry.getKey());
                daos.writeUTF(entry.getValue());
            }
            byte[] eventData = baos.toByteArray();
            if (secretKey != null) {
                final Cipher cipher = Cipher.getInstance("AES");
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
                eventData = cipher.doFinal(eventData);
            }
            final Future<Integer> future = threadPool.submit(new BDBWriter(keyData, eventData, environment, database,
                gate, dbCount, getBatchSize(), lockTimeoutRetryCount));
            boolean interrupted = false;
            int ieCount = 0;
            do {
                try {
                    future.get();
                } catch (final InterruptedException ie) {
                    interrupted = true;
                    ++ieCount;
                }
            } while (interrupted && ieCount <= 1);

        } catch (final Exception ex) {
            throw new LoggingException("Exception occurred writing log event", ex);
        }
    }

    @Override
    protected boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
    	boolean closed = true;
        LOGGER.debug("Shutting down FlumePersistentManager");
        worker.shutdown();
        final long requestedTimeoutMillis = timeUnit.toMillis(timeout);
        final long shutdownWaitMillis = requestedTimeoutMillis > 0 ? requestedTimeoutMillis : SHUTDOWN_WAIT_MILLIS;
		try {
            worker.join(shutdownWaitMillis);
        } catch (final InterruptedException ie) {
            // Ignore the exception and shutdown.
        }
        ExecutorServices.shutdown(threadPool, shutdownWaitMillis, TimeUnit.MILLISECONDS, toString());
        try {
            worker.join();
        } catch (final InterruptedException ex) {
            logDebug("interrupted while waiting for worker to complete", ex);
        }
        try {
            LOGGER.debug("FlumePersistenceManager dataset status: {}", database.getStats(new StatsConfig()));
            database.close();
        } catch (final Exception ex) {
            logWarn("Failed to close database", ex);
            closed = false;
        }
        try {
            environment.cleanLog();
            environment.close();
        } catch (final Exception ex) {
            logWarn("Failed to close environment", ex);
            closed = false;
        }
        return closed && super.releaseSub(timeout, timeUnit);
    }

    private void doSend(final SimpleEvent event) {
        LOGGER.debug("Sending event to Flume");
        super.send(event);
    }

    /**
     * Thread for writing to Berkeley DB to avoid having interrupts close the database.
     */
    private static class BDBWriter implements Callable<Integer> {
        private final byte[] eventData;
        private final byte[] keyData;
        private final Environment environment;
        private final Database database;
        private final Gate gate;
        private final AtomicLong dbCount;
        private final long batchSize;
        private final int lockTimeoutRetryCount;

        public BDBWriter(final byte[] keyData, final byte[] eventData, final Environment environment,
                         final Database database, final Gate gate, final AtomicLong dbCount, final long batchSize,
                         final int lockTimeoutRetryCount) {
            this.keyData = keyData;
            this.eventData = eventData;
            this.environment = environment;
            this.database = database;
            this.gate = gate;
            this.dbCount = dbCount;
            this.batchSize = batchSize;
            this.lockTimeoutRetryCount = lockTimeoutRetryCount;
        }

        @Override
        public Integer call() throws Exception {
            final DatabaseEntry key = new DatabaseEntry(keyData);
            final DatabaseEntry data = new DatabaseEntry(eventData);
            Exception exception = null;
            for (int retryIndex = 0; retryIndex < lockTimeoutRetryCount; ++retryIndex) {
                Transaction txn = null;
                try {
                    txn = environment.beginTransaction(null, null);
                    try {
                        database.put(txn, key, data);
                        txn.commit();
                        txn = null;
                        if (dbCount.incrementAndGet() >= batchSize) {
                            gate.open();
                        }
                        exception = null;
                        break;
                    } catch (final LockConflictException lce) {
                        exception = lce;
                        // Fall through and retry.
                    } catch (final Exception ex) {
                        if (txn != null) {
                            txn.abort();
                        }
                        throw ex;
                    } finally {
                        if (txn != null) {
                            txn.abort();
                            txn = null;
                        }
                    }
                } catch (final LockConflictException lce) {
                    exception = lce;
                    if (txn != null) {
                        try {
                            txn.abort();
                            txn = null;
                        } catch (final Exception ex) {
                            LOGGER.trace("Ignoring exception while aborting transaction during lock conflict.");
                        }
                    }

                }
                try {
                    Thread.sleep(LOCK_TIMEOUT_SLEEP_MILLIS);
                } catch (final InterruptedException ie) {
                    // Ignore the error
                }
            }
            if (exception != null) {
                throw exception;
            }
            return eventData.length;
        }
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        private final String name;
        private final Agent[] agents;
        private final int batchSize;
        private final String dataDir;
        private final int retries;
        private final int connectionTimeout;
        private final int requestTimeout;
        private final int delayMillis;
        private final int lockTimeoutRetryCount;
        private final Property[] properties;

        /**
         * Constructor.
         * @param name The name of the Appender.
         * @param agents The agents.
         * @param batchSize The number of events to include in a batch.
         * @param dataDir The directory for data.
         */
        public FactoryData(final String name, final Agent[] agents, final int batchSize, final int retries,
                           final int connectionTimeout, final int requestTimeout, final int delayMillis,
                           final int lockTimeoutRetryCount, final String dataDir, final Property[] properties) {
            this.name = name;
            this.agents = agents;
            this.batchSize = batchSize;
            this.dataDir = dataDir;
            this.retries = retries;
            this.connectionTimeout = connectionTimeout;
            this.requestTimeout = requestTimeout;
            this.delayMillis = delayMillis;
            this.lockTimeoutRetryCount = lockTimeoutRetryCount;
            this.properties = properties;
        }
    }

    /**
     * Avro Manager Factory.
     */
    private static class BDBManagerFactory implements ManagerFactory<FlumePersistentManager, FactoryData> {

        /**
         * Create the FlumeKratiManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return The FlumeKratiManager.
         */
        @Override
        public FlumePersistentManager createManager(final String name, final FactoryData data) {
            SecretKey secretKey = null;
            Database database = null;
            Environment environment = null;

            final Map<String, String> properties = new HashMap<>();
            if (data.properties != null) {
                for (final Property property : data.properties) {
                    properties.put(property.getName(), property.getValue());
                }
            }

            try {
                final File dir = new File(data.dataDir);
                FileUtils.mkdir(dir, true);
                final EnvironmentConfig dbEnvConfig = new EnvironmentConfig();
                dbEnvConfig.setTransactional(true);
                dbEnvConfig.setAllowCreate(true);
                dbEnvConfig.setLockTimeout(5, TimeUnit.SECONDS);
                environment = new Environment(dir, dbEnvConfig);
                final DatabaseConfig dbConfig = new DatabaseConfig();
                dbConfig.setTransactional(true);
                dbConfig.setAllowCreate(true);
                database = environment.openDatabase(null, name, dbConfig);
            } catch (final Exception ex) {
                LOGGER.error("Could not create FlumePersistentManager", ex);
                // For consistency, close database as well as environment even though it should never happen since the
                // database is that last thing in the block above, but this does guard against a future line being
                // inserted at the end that would bomb (like some debug logging).
                if (database != null) {
                    database.close();
                    database = null;
                }
                if (environment != null) {
                    environment.close();
                    environment = null;
                }
                return null;
            }

            try {
                String key = null;
                for (final Map.Entry<String, String> entry : properties.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(KEY_PROVIDER)) {
                        key = entry.getValue();
                        break;
                    }
                }
                if (key != null) {
                    final PluginManager manager = new PluginManager("KeyProvider");
                    manager.collectPlugins();
                    final Map<String, PluginType<?>> plugins = manager.getPlugins();
                    if (plugins != null) {
                        boolean found = false;
                        for (final Map.Entry<String, PluginType<?>> entry : plugins.entrySet()) {
                            if (entry.getKey().equalsIgnoreCase(key)) {
                                found = true;
                                final Class<?> cl = entry.getValue().getPluginClass();
                                try {
                                    final SecretKeyProvider provider = (SecretKeyProvider) cl.newInstance();
                                    secretKey = provider.getSecretKey();
                                    LOGGER.debug("Persisting events using SecretKeyProvider {}", cl.getName());
                                } catch (final Exception ex) {
                                    LOGGER.error("Unable to create SecretKeyProvider {}, encryption will be disabled",
                                        cl.getName());
                                }
                                break;
                            }
                        }
                        if (!found) {
                            LOGGER.error("Unable to locate SecretKey provider {}, encryption will be disabled", key);
                        }
                    } else {
                        LOGGER.error("Unable to locate SecretKey provider {}, encryption will be disabled", key);
                    }
                }
            } catch (final Exception ex) {
                LOGGER.warn("Error setting up encryption - encryption will be disabled", ex);
            }
            return new FlumePersistentManager(name, data.name, data.agents, data.batchSize, data.retries,
                data.connectionTimeout, data.requestTimeout, data.delayMillis, database, environment, secretKey,
                data.lockTimeoutRetryCount);
        }
    }

    /**
     * Thread that sends data to Flume and pulls it from Berkeley DB.
     */
    private static class WriterThread extends Log4jThread  {
        private volatile boolean shutdown = false;
        private final Database database;
        private final Environment environment;
        private final FlumePersistentManager manager;
        private final Gate gate;
        private final SecretKey secretKey;
        private final int batchSize;
        private final AtomicLong dbCounter;
        private final int lockTimeoutRetryCount;

        public WriterThread(final Database database, final Environment environment,
                            final FlumePersistentManager manager, final Gate gate, final int batchsize,
                            final SecretKey secretKey, final AtomicLong dbCount, final int lockTimeoutRetryCount) {
            super("FlumePersistentManager-Writer");
            this.database = database;
            this.environment = environment;
            this.manager = manager;
            this.gate = gate;
            this.batchSize = batchsize;
            this.secretKey = secretKey;
            this.setDaemon(true);
            this.dbCounter = dbCount;
            this.lockTimeoutRetryCount = lockTimeoutRetryCount;
        }

        public void shutdown() {
            LOGGER.debug("Writer thread shutting down");
            this.shutdown = true;
            gate.open();
        }

        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public void run() {
            LOGGER.trace("WriterThread started - batch size = " + batchSize + ", delayMillis = " + manager.getDelayMillis());
            long nextBatchMillis = System.currentTimeMillis() + manager.getDelayMillis();
            while (!shutdown) {
                final long nowMillis = System.currentTimeMillis();
                final long dbCount = database.count();
                dbCounter.set(dbCount);
                if (dbCount >= batchSize || dbCount > 0 && nextBatchMillis <= nowMillis) {
                    nextBatchMillis = nowMillis + manager.getDelayMillis();
                    try {
                        boolean errors = false;
                        final DatabaseEntry key = new DatabaseEntry();
                        final DatabaseEntry data = new DatabaseEntry();

                        gate.close();
                        OperationStatus status;
                        if (batchSize > 1) {
                            try {
                                errors = sendBatch(key, data);
                            } catch (final Exception ex) {
                                break;
                            }
                        } else {
                            Exception exception = null;
                            for (int retryIndex = 0; retryIndex < lockTimeoutRetryCount; ++retryIndex) {
                                exception = null;
                                Transaction txn = null;
                                Cursor cursor = null;
                                try {
                                    txn = environment.beginTransaction(null, null);
                                    cursor = database.openCursor(txn, null);
                                    try {
                                        status = cursor.getFirst(key, data, LockMode.RMW);
                                        while (status == OperationStatus.SUCCESS) {
                                            final SimpleEvent event = createEvent(data);
                                            if (event != null) {
                                                try {
                                                    manager.doSend(event);
                                                } catch (final Exception ioe) {
                                                    errors = true;
                                                    LOGGER.error("Error sending event", ioe);
                                                    break;
                                                }
                                                try {
                                                    cursor.delete();
                                                } catch (final Exception ex) {
                                                    LOGGER.error("Unable to delete event", ex);
                                                }
                                            }
                                            status = cursor.getNext(key, data, LockMode.RMW);
                                        }
                                        if (cursor != null) {
                                            cursor.close();
                                            cursor = null;
                                        }
                                        txn.commit();
                                        txn = null;
                                        dbCounter.decrementAndGet();
                                        exception = null;
                                        break;
                                    } catch (final LockConflictException lce) {
                                        exception = lce;
                                        // Fall through and retry.
                                    } catch (final Exception ex) {
                                        LOGGER.error("Error reading or writing to database", ex);
                                        shutdown = true;
                                        break;
                                    } finally {
                                        if (cursor != null) {
                                            cursor.close();
                                            cursor = null;
                                        }
                                        if (txn != null) {
                                            txn.abort();
                                            txn = null;
                                        }
                                    }
                                } catch (final LockConflictException lce) {
                                    exception = lce;
                                    if (cursor != null) {
                                        try {
                                            cursor.close();
                                            cursor = null;
                                        } catch (final Exception ex) {
                                            LOGGER.trace("Ignored exception closing cursor during lock conflict.");
                                        }
                                    }
                                    if (txn != null) {
                                        try {
                                            txn.abort();
                                            txn = null;
                                        } catch (final Exception ex) {
                                            LOGGER.trace("Ignored exception aborting tx during lock conflict.");
                                        }
                                    }
                                }
                                try {
                                    Thread.sleep(LOCK_TIMEOUT_SLEEP_MILLIS);
                                } catch (final InterruptedException ie) {
                                    // Ignore the error
                                }
                            }
                            if (exception != null) {
                                LOGGER.error("Unable to read or update data base", exception);
                            }
                        }
                        if (errors) {
                            Thread.sleep(manager.getDelayMillis());
                            continue;
                        }
                    } catch (final Exception ex) {
                        LOGGER.warn("WriterThread encountered an exception. Continuing.", ex);
                    }
                } else {
                    if (nextBatchMillis <= nowMillis) {
                        nextBatchMillis = nowMillis + manager.getDelayMillis();
                    }
                    try {
                        final long interval = nextBatchMillis - nowMillis;
                        gate.waitForOpen(interval);
                    } catch (final InterruptedException ie) {
                        LOGGER.warn("WriterThread interrupted, continuing");
                    } catch (final Exception ex) {
                        LOGGER.error("WriterThread encountered an exception waiting for work", ex);
                        break;
                    }
                }
            }

            if (batchSize > 1 && database.count() > 0) {
                final DatabaseEntry key = new DatabaseEntry();
                final DatabaseEntry data = new DatabaseEntry();
                try {
                    sendBatch(key, data);
                } catch (final Exception ex) {
                    LOGGER.warn("Unable to write final batch");
                }
            }
            LOGGER.trace("WriterThread exiting");
        }

        private boolean sendBatch(DatabaseEntry key, final DatabaseEntry data) throws Exception {
            boolean errors = false;
            OperationStatus status;
            Cursor cursor = null;
            try {
            	final BatchEvent batch = new BatchEvent();
            	for (int retryIndex = 0; retryIndex < lockTimeoutRetryCount; ++retryIndex) {
            		try {
            			cursor = database.openCursor(null, CursorConfig.DEFAULT);
            			status = cursor.getFirst(key, data, null);

            			for (int i = 0; status == OperationStatus.SUCCESS && i < batchSize; ++i) {
            				final SimpleEvent event = createEvent(data);
            				if (event != null) {
            					batch.addEvent(event);
            				}
            				status = cursor.getNext(key, data, null);
            			}
            			break;
            		} catch (final LockConflictException lce) {
            			if (cursor != null) {
            				try {
                                cursor.close();
                                cursor = null;
                            } catch (final Exception ex) {
                                LOGGER.trace("Ignored exception closing cursor during lock conflict.");
                            }
                        }
                    }
            	}

                try {
                    manager.send(batch);
                } catch (final Exception ioe) {
                    LOGGER.error("Error sending events", ioe);
                    errors = true;
                }
                if (!errors) {
                	if (cursor != null) {
	                    cursor.close();
	                    cursor = null;
                	}
                    Transaction txn = null;
                    Exception exception = null;
                    for (int retryIndex = 0; retryIndex < lockTimeoutRetryCount; ++retryIndex) {
                        try {
                            txn = environment.beginTransaction(null, null);
                            try {
                                for (final Event event : batch.getEvents()) {
                                    try {
                                        final Map<String, String> headers = event.getHeaders();
                                        key = new DatabaseEntry(headers.get(FlumeEvent.GUID).getBytes(UTF8));
                                        database.delete(txn, key);
                                    } catch (final Exception ex) {
                                        LOGGER.error("Error deleting key from database", ex);
                                    }
                                }
                                txn.commit();
                                long count = dbCounter.get();
                                while (!dbCounter.compareAndSet(count, count - batch.getEvents().size())) {
                                    count = dbCounter.get();
                                }
                                exception = null;
                                break;
                            } catch (final LockConflictException lce) {
                                exception = lce;
                                if (cursor != null) {
                                    try {
                                        cursor.close();
                                        cursor = null;
                                    } catch (final Exception ex) {
                                        LOGGER.trace("Ignored exception closing cursor during lock conflict.");
                                    }
                                }
                                if (txn != null) {
                                    try {
                                        txn.abort();
                                        txn = null;
                                    } catch (final Exception ex) {
                                        LOGGER.trace("Ignored exception aborting transaction during lock conflict.");
                                    }
                                }
                            } catch (final Exception ex) {
                                LOGGER.error("Unable to commit transaction", ex);
                                if (txn != null) {
                                    txn.abort();
                                }
                            }
                        } catch (final LockConflictException lce) {
                            exception = lce;
                            if (cursor != null) {
                                try {
                                    cursor.close();
                                    cursor = null;
                                } catch (final Exception ex) {
                                    LOGGER.trace("Ignored exception closing cursor during lock conflict.");
                                }
                            }
                            if (txn != null) {
                                try {
                                    txn.abort();
                                    txn = null;
                                } catch (final Exception ex) {
                                    LOGGER.trace("Ignored exception aborting transaction during lock conflict.");
                                }
                            }
                        } finally {
                            if (cursor != null) {
                                cursor.close();
                                cursor = null;
                            }
                            if (txn != null) {
                                txn.abort();
                                txn = null;
                            }
                        }
                        try {
                            Thread.sleep(LOCK_TIMEOUT_SLEEP_MILLIS);
                        } catch (final InterruptedException ie) {
                            // Ignore the error
                        }
                    }
                    if (exception != null) {
                        LOGGER.error("Unable to delete events from data base", exception);
                    }
                }
            } catch (final Exception ex) {
                LOGGER.error("Error reading database", ex);
                shutdown = true;
                throw ex;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            return errors;
        }

        private SimpleEvent createEvent(final DatabaseEntry data) {
            final SimpleEvent event = new SimpleEvent();
            try {
                byte[] eventData = data.getData();
                if (secretKey != null) {
                    final Cipher cipher = Cipher.getInstance("AES");
                    cipher.init(Cipher.DECRYPT_MODE, secretKey);
                    eventData = cipher.doFinal(eventData);
                }
                final ByteArrayInputStream bais = new ByteArrayInputStream(eventData);
                final DataInputStream dais = new DataInputStream(bais);
                int length = dais.readInt();
                final byte[] bytes = new byte[length];
                dais.read(bytes, 0, length);
                event.setBody(bytes);
                length = dais.readInt();
                final Map<String, String> map = new HashMap<>(length);
                for (int i = 0; i < length; ++i) {
                    final String headerKey = dais.readUTF();
                    final String value = dais.readUTF();
                    map.put(headerKey, value);
                }
                event.setHeaders(map);
                return event;
            } catch (final Exception ex) {
                LOGGER.error("Error retrieving event", ex);
                return null;
            }
        }

    }

    /**
     * An internal class.
     */
    private static class Gate {

        private boolean isOpen = false;

        public boolean isOpen() {
            return isOpen;
        }

        public synchronized void open() {
            isOpen = true;
            notifyAll();
        }

        public synchronized void close() {
            isOpen = false;
        }

        public synchronized void waitForOpen(final long timeout) throws InterruptedException {
            wait(timeout);
        }
    }
}
