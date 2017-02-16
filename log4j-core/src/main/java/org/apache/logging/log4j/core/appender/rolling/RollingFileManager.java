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
package org.apache.logging.log4j.core.appender.rolling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConfigurationFactoryData;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.Log4jThreadFactory;

/**
 * The Rolling File Manager.
 */
public class RollingFileManager extends FileManager {

    private static RollingFileManagerFactory factory = new RollingFileManagerFactory();
    private static final int MAX_TRIES = 3;
    private static final int MIN_DURATION = 100;

    protected long size;
    private long initialTime;
    private final PatternProcessor patternProcessor;
    private final Semaphore semaphore = new Semaphore(1);
    private final Log4jThreadFactory threadFactory = Log4jThreadFactory.createThreadFactory("RollingFileManager");
    private volatile TriggeringPolicy triggeringPolicy;
    private volatile RolloverStrategy rolloverStrategy;
    private volatile boolean renameEmptyFiles = false;
    private volatile boolean initialized = false;
    private volatile String fileName;
    private FileExtension fileExtension;
    /* This executor pool will create a new Thread for every work async action to be performed. Using it allows
       us to make sure all the Threads are completed when the Manager is stopped. */
    private ExecutorService asyncExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0, TimeUnit.MILLISECONDS,
            new EmptyQueue(), threadFactory);

    private static final AtomicReferenceFieldUpdater<RollingFileManager, TriggeringPolicy> triggeringPolicyUpdater =
            AtomicReferenceFieldUpdater.newUpdater(RollingFileManager.class, TriggeringPolicy.class, "triggeringPolicy");

    private static final AtomicReferenceFieldUpdater<RollingFileManager, RolloverStrategy> rolloverStrategyUpdater =
            AtomicReferenceFieldUpdater.newUpdater(RollingFileManager.class, RolloverStrategy.class, "rolloverStrategy");

    @Deprecated
    protected RollingFileManager(final String fileName, final String pattern, final OutputStream os,
            final boolean append, final long size, final long time, final TriggeringPolicy triggeringPolicy,
            final RolloverStrategy rolloverStrategy, final String advertiseURI,
            final Layout<? extends Serializable> layout, final int bufferSize, final boolean writeHeader) {
        this(fileName, pattern, os, append, size, time, triggeringPolicy, rolloverStrategy, advertiseURI, layout,
                writeHeader, ByteBuffer.wrap(new byte[Constants.ENCODER_BYTE_BUFFER_SIZE]));
    }

    @Deprecated
    protected RollingFileManager(final String fileName, final String pattern, final OutputStream os,
            final boolean append, final long size, final long time, final TriggeringPolicy triggeringPolicy,
            final RolloverStrategy rolloverStrategy, final String advertiseURI,
            final Layout<? extends Serializable> layout, final boolean writeHeader, final ByteBuffer buffer) {
        super(fileName, os, append, false, advertiseURI, layout, writeHeader, buffer);
        this.size = size;
        this.initialTime = time;
        this.triggeringPolicy = triggeringPolicy;
        this.rolloverStrategy = rolloverStrategy;
        this.patternProcessor = new PatternProcessor(pattern);
        this.patternProcessor.setPrevFileTime(time);
        this.fileName = fileName;
        this.fileExtension = FileExtension.lookupForFile(pattern);
    }

    /**
     * @since 2.7
     */
    protected RollingFileManager(final LoggerContext loggerContext, final String fileName, final String pattern, final OutputStream os,
            final boolean append, final boolean createOnDemand, final long size, final long time,
            final TriggeringPolicy triggeringPolicy, final RolloverStrategy rolloverStrategy,
            final String advertiseURI, final Layout<? extends Serializable> layout, final boolean writeHeader, final ByteBuffer buffer) {
        super(loggerContext, fileName, os, append, false, createOnDemand, advertiseURI, layout, writeHeader, buffer);
        this.size = size;
        this.initialTime = time;
        this.triggeringPolicy = triggeringPolicy;
        this.rolloverStrategy = rolloverStrategy;
        this.patternProcessor = new PatternProcessor(pattern);
        this.patternProcessor.setPrevFileTime(time);
        this.fileName = fileName;
        this.fileExtension = FileExtension.lookupForFile(pattern);
    }

    public void initialize() {

        if (!initialized) {
            LOGGER.debug("Initializing triggering policy {}", triggeringPolicy);
            initialized = true;
            triggeringPolicy.initialize(this);
            if (triggeringPolicy instanceof LifeCycle) {
                ((LifeCycle) triggeringPolicy).start();
            }
        }
    }

    /**
     * Returns a RollingFileManager.
     * @param fileName The file name.
     * @param pattern The pattern for rolling file.
     * @param append true if the file should be appended to.
     * @param bufferedIO true if data should be buffered.
     * @param policy The TriggeringPolicy.
     * @param strategy The RolloverStrategy.
     * @param advertiseURI the URI to use when advertising the file
     * @param layout The Layout.
     * @param bufferSize buffer size to use if bufferedIO is true
     * @param immediateFlush flush on every write or not
     * @param createOnDemand true if you want to lazy-create the file (a.k.a. on-demand.)
     * @param configuration The configuration.
     * @return A RollingFileManager.
     */
    public static RollingFileManager getFileManager(final String fileName, final String pattern, final boolean append,
            final boolean bufferedIO, final TriggeringPolicy policy, final RolloverStrategy strategy,
            final String advertiseURI, final Layout<? extends Serializable> layout, final int bufferSize,
            final boolean immediateFlush, final boolean createOnDemand, final Configuration configuration) {
        String name = fileName == null ? pattern : fileName;
        return (RollingFileManager) getManager(name, new FactoryData(fileName, pattern, append,
            bufferedIO, policy, strategy, advertiseURI, layout, bufferSize, immediateFlush, createOnDemand, configuration), factory);
    }

    /**
     * Returns the name of the File being managed.
     * @return The name of the File being managed.
     */
    @Override
    public String getFileName() {
        if (rolloverStrategy instanceof DirectFileRolloverStrategy) {
            fileName = ((DirectFileRolloverStrategy) rolloverStrategy).getCurrentFileName(this);
        }
        return fileName;
    }

    public FileExtension getFileExtension() {
        return fileExtension;
    }

    // override to make visible for unit tests
    @Override
    protected synchronized void write(final byte[] bytes, final int offset, final int length,
            final boolean immediateFlush) {
        super.write(bytes, offset, length, immediateFlush);
    }

    @Override
    protected synchronized void writeToDestination(final byte[] bytes, final int offset, final int length) {
        size += length;
        super.writeToDestination(bytes, offset, length);
    }

    public boolean isRenameEmptyFiles() {
        return renameEmptyFiles;
    }

    public void setRenameEmptyFiles(final boolean renameEmptyFiles) {
        this.renameEmptyFiles = renameEmptyFiles;
    }

    /**
     * Returns the current size of the file.
     * @return The size of the file in bytes.
     */
    public long getFileSize() {
        return size + byteBuffer.position();
    }

    /**
     * Returns the time the file was created.
     * @return The time the file was created.
     */
    public long getFileTime() {
        return initialTime;
    }

    /**
     * Determines if a rollover should occur.
     * @param event The LogEvent.
     */
    public synchronized void checkRollover(final LogEvent event) {
        if (triggeringPolicy.isTriggeringEvent(event)) {
            rollover();
        }
    }

    @Override
    public boolean releaseSub(final long timeout, final TimeUnit timeUnit) {
        LOGGER.debug("Shutting down RollingFileManager {}" + getName());
        boolean stopped = true;
        if (triggeringPolicy instanceof LifeCycle2) {
            stopped &= ((LifeCycle2) triggeringPolicy).stop(timeout, timeUnit);
        } else if (triggeringPolicy instanceof LifeCycle) {
            ((LifeCycle) triggeringPolicy).stop();
            stopped &= true;
        }
        boolean status = super.releaseSub(timeout, timeUnit) && stopped;
        asyncExecutor.shutdown();
        try {
            // Allow at least the minimum interval to pass so async actions can complete.
            long millis = timeUnit.toMillis(timeout);
            long waitInterval = MIN_DURATION < millis ? millis : MIN_DURATION;

            for (int count = 1; count <= MAX_TRIES && !asyncExecutor.isTerminated(); ++count) {
                asyncExecutor.awaitTermination(waitInterval * count, TimeUnit.MILLISECONDS);
            }
            if (asyncExecutor.isTerminated()) {
                LOGGER.debug("All asynchronous threads have terminated");
            } else {
                asyncExecutor.shutdownNow();
                try {
                    asyncExecutor.awaitTermination(timeout, timeUnit);
                    if (asyncExecutor.isTerminated()) {
                        LOGGER.debug("All asynchronous threads have terminated");
                    } else {
                        LOGGER.debug("RollingFileManager shutting down but some asynchronous services may not have completed");
                    }
                } catch (final InterruptedException inner) {
                    LOGGER.warn("RollingFileManager stopped but some asynchronous services may not have completed.");
                }
            }
        } catch (final InterruptedException ie) {
            asyncExecutor.shutdownNow();
            try {
                asyncExecutor.awaitTermination(timeout, timeUnit);
                if (asyncExecutor.isTerminated()) {
                    LOGGER.debug("All asynchronous threads have terminated");
                }
            } catch (final InterruptedException inner) {
                LOGGER.warn("RollingFileManager stopped but some asynchronous services may not have completed.");
            }
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        LOGGER.debug("RollingFileManager shutdown completed with status {}", status);
        return status;
    }

    public synchronized void rollover() {
        if (!hasOutputStream()) {
            return;
        }
        if (rollover(rolloverStrategy)) {
            try {
                size = 0;
                initialTime = System.currentTimeMillis();
                createFileAfterRollover();
            } catch (final IOException e) {
                logError("Failed to create file after rollover", e);
            }
        }
    }

    protected void createFileAfterRollover() throws IOException  {
        setOutputStream(createOutputStream());
    }

    /**
     * Returns the pattern processor.
     * @return The PatternProcessor.
     */
    public PatternProcessor getPatternProcessor() {
        return patternProcessor;
    }

    public void setTriggeringPolicy(final TriggeringPolicy triggeringPolicy) {
        triggeringPolicy.initialize(this);
        TriggeringPolicy policy = this.triggeringPolicy;
        int count = 0;
        boolean policyUpdated = false;
        do {
            ++count;
        } while (!(policyUpdated = triggeringPolicyUpdater.compareAndSet(this, this.triggeringPolicy, triggeringPolicy))
                && count < MAX_TRIES);
        if (policyUpdated) {
            if (triggeringPolicy instanceof LifeCycle) {
                ((LifeCycle) triggeringPolicy).start();
            }
            if (policy instanceof LifeCycle) {
                ((LifeCycle) policy).stop();
            }
        } else {
            if (triggeringPolicy instanceof LifeCycle) {
                ((LifeCycle) triggeringPolicy).stop();
            }
        }
    }

    public void setRolloverStrategy(final RolloverStrategy rolloverStrategy) {
        rolloverStrategyUpdater.compareAndSet(this, this.rolloverStrategy, rolloverStrategy);
    }

    /**
     * Returns the triggering policy.
     * @param <T> TriggeringPolicy type
     * @return The TriggeringPolicy
     */
    @SuppressWarnings("unchecked")
    public <T extends TriggeringPolicy> T getTriggeringPolicy() {
        // TODO We could parameterize this class with a TriggeringPolicy instead of type casting here.
        return (T) this.triggeringPolicy;
    }

    /**
     * Returns the rollover strategy.
     * @return The RolloverStrategy
     */
    public RolloverStrategy getRolloverStrategy() {
        return this.rolloverStrategy;
    }

    private boolean rollover(final RolloverStrategy strategy) {

        boolean releaseRequired = false;
        try {
            // Block until the asynchronous operation is completed.
            semaphore.acquire();
            releaseRequired = true;
        } catch (final InterruptedException e) {
            logError("Thread interrupted while attempting to check rollover", e);
            return false;
        }

        boolean success = true;

        try {
            final RolloverDescription descriptor = strategy.rollover(this);
            if (descriptor != null) {
                writeFooter();
                closeOutputStream();
                if (descriptor.getSynchronous() != null) {
                    LOGGER.debug("RollingFileManager executing synchronous {}", descriptor.getSynchronous());
                    try {
                        success = descriptor.getSynchronous().execute();
                    } catch (final Exception ex) {
                        success = false;
                        logError("Caught error in synchronous task", ex);
                    }
                }

                if (success && descriptor.getAsynchronous() != null) {
                    LOGGER.debug("RollingFileManager executing async {}", descriptor.getAsynchronous());
                    asyncExecutor.execute(new AsyncAction(descriptor.getAsynchronous(), this));
                    releaseRequired = false;
                }
                return true;
            }
            return false;
        } finally {
            if (releaseRequired) {
                semaphore.release();
            }
        }

    }

    /**
     * Performs actions asynchronously.
     */
    private static class AsyncAction extends AbstractAction {

        private final Action action;
        private final RollingFileManager manager;

        /**
         * Constructor.
         * @param act The action to perform.
         * @param manager The manager.
         */
        public AsyncAction(final Action act, final RollingFileManager manager) {
            this.action = act;
            this.manager = manager;
        }

        /**
         * Executes an action.
         *
         * @return true if action was successful.  A return value of false will cause
         *         the rollover to be aborted if possible.
         * @throws java.io.IOException if IO error, a thrown exception will cause the rollover
         *                             to be aborted if possible.
         */
        @Override
        public boolean execute() throws IOException {
            try {
                return action.execute();
            } finally {
                manager.semaphore.release();
            }
        }

        /**
         * Cancels the action if not already initialized or waits till completion.
         */
        @Override
        public void close() {
            action.close();
        }

        /**
         * Determines if action has been completed.
         *
         * @return true if action is complete.
         */
        @Override
        public boolean isComplete() {
            return action.isComplete();
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append("[action=");
            builder.append(action);
            builder.append(", manager=");
            builder.append(manager);
            builder.append(", isComplete()=");
            builder.append(isComplete());
            builder.append(", isInterrupted()=");
            builder.append(isInterrupted());
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Factory data.
     */
    private static class FactoryData extends ConfigurationFactoryData {
        private final String fileName;
        private final String pattern;
        private final boolean append;
        private final boolean bufferedIO;
        private final int bufferSize;
        private final boolean immediateFlush;
        private final boolean createOnDemand;
        private final TriggeringPolicy policy;
        private final RolloverStrategy strategy;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Creates the data for the factory.
         * @param pattern The pattern.
         * @param append The append flag.
         * @param bufferedIO The bufferedIO flag.
         * @param advertiseURI
         * @param layout The Layout.
         * @param bufferSize the buffer size
         * @param immediateFlush flush on every write or not
         * @param createOnDemand true if you want to lazy-create the file (a.k.a. on-demand.)
         * @param configuration The configuration
         */
        public FactoryData(final String fileName, final String pattern, final boolean append, final boolean bufferedIO,
                final TriggeringPolicy policy, final RolloverStrategy strategy, final String advertiseURI,
                final Layout<? extends Serializable> layout, final int bufferSize, final boolean immediateFlush,
                final boolean createOnDemand, final Configuration configuration) {
            super(configuration);
            this.fileName = fileName;
            this.pattern = pattern;
            this.append = append;
            this.bufferedIO = bufferedIO;
            this.bufferSize = bufferSize;
            this.policy = policy;
            this.strategy = strategy;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
            this.immediateFlush = immediateFlush;
            this.createOnDemand = createOnDemand;
        }

        public TriggeringPolicy getTriggeringPolicy()
        {
            return this.policy;
        }

        public RolloverStrategy getRolloverStrategy()
        {
            return this.strategy;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append("[pattern=");
            builder.append(pattern);
            builder.append(", append=");
            builder.append(append);
            builder.append(", bufferedIO=");
            builder.append(bufferedIO);
            builder.append(", bufferSize=");
            builder.append(bufferSize);
            builder.append(", policy=");
            builder.append(policy);
            builder.append(", strategy=");
            builder.append(strategy);
            builder.append(", advertiseURI=");
            builder.append(advertiseURI);
            builder.append(", layout=");
            builder.append(layout);
            builder.append("]");
            return builder.toString();
        }
    }

    @Override
    public void updateData(final Object data)
    {
        final FactoryData factoryData = (FactoryData) data;
        setRolloverStrategy(factoryData.getRolloverStrategy());
        setTriggeringPolicy(factoryData.getTriggeringPolicy());
    }

    /**
     * Factory to create a RollingFileManager.
     */
    private static class RollingFileManagerFactory implements ManagerFactory<RollingFileManager, FactoryData> {

        /**
         * Creates a RollingFileManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return a RollingFileManager.
         */
        @Override
        public RollingFileManager createManager(final String name, final FactoryData data) {
            long size = 0;
            boolean writeHeader = !data.append;
            File file = null;
            if (data.fileName != null) {
                file = new File(data.fileName);
                // LOG4J2-1140: check writeHeader before creating the file
                writeHeader = !data.append || !file.exists();

                try {
                    FileUtils.makeParentDirs(file);
                    final boolean created = data.createOnDemand ? false : file.createNewFile();
                    LOGGER.trace("New file '{}' created = {}", name, created);
                } catch (final IOException ioe) {
                    LOGGER.error("Unable to create file " + name, ioe);
                    return null;
                }
                size = data.append ? file.length() : 0;
            }

            try {
                final int actualSize = data.bufferedIO ? data.bufferSize : Constants.ENCODER_BYTE_BUFFER_SIZE;
                final ByteBuffer buffer = ByteBuffer.wrap(new byte[actualSize]);
                final OutputStream os = data.createOnDemand  || data.fileName == null ? null :
                        new FileOutputStream(data.fileName, data.append);
                final long time = data.createOnDemand || file == null ?
                        System.currentTimeMillis() : file.lastModified(); // LOG4J2-531 create file first so time has valid value

                return new RollingFileManager(data.getLoggerContext(), data.fileName, data.pattern, os,
                        data.append, data.createOnDemand, size, time, data.policy, data.strategy, data.advertiseURI,
                        data.layout, writeHeader, buffer);
            } catch (final IOException ex) {
                LOGGER.error("RollingFileManager (" + name + ") " + ex, ex);
            }
            return null;
        }
    }

    private static class EmptyQueue extends ArrayBlockingQueue<Runnable> {

        EmptyQueue() {
            super(1);
        }

        @Override
        public int remainingCapacity() {
            return 0;
        }

        @Override
        public boolean add(Runnable runnable) {
            throw new IllegalStateException("Queue is full");
        }

        @Override
        public void put(Runnable runnable) throws InterruptedException {
            /* No point in going into a permanent wait */
            throw new InterruptedException("Unable to insert into queue");
        }

        @Override
        public boolean offer(Runnable runnable, long timeout, TimeUnit timeUnit) throws InterruptedException {
            Thread.sleep(timeUnit.toMillis(timeout));
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends Runnable> collection) {
            if (collection.size() > 0) {
                throw new IllegalArgumentException("Too many items in collection");
            }
            return false;
        }

    }

}
