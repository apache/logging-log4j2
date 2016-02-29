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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.rolling.action.AbstractAction;
import org.apache.logging.log4j.core.appender.rolling.action.Action;
import org.apache.logging.log4j.core.util.Log4jThread;

/**
 * The Rolling File Manager.
 */
public class RollingFileManager extends FileManager {
    private static final long serialVersionUID = 1L;

    private static RollingFileManagerFactory factory = new RollingFileManagerFactory();

    protected long size;
    private long initialTime;
    private final PatternProcessor patternProcessor;
    private final Semaphore semaphore = new Semaphore(1);
    private volatile TriggeringPolicy triggeringPolicy;
    private volatile RolloverStrategy rolloverStrategy;

    private static final AtomicReferenceFieldUpdater<RollingFileManager, TriggeringPolicy> triggeringPolicyUpdater =
            AtomicReferenceFieldUpdater.newUpdater(RollingFileManager.class, TriggeringPolicy.class, "triggeringPolicy");

    private static final AtomicReferenceFieldUpdater<RollingFileManager, RolloverStrategy> rolloverStrategyUpdater =
            AtomicReferenceFieldUpdater.newUpdater(RollingFileManager.class, RolloverStrategy.class, "rolloverStrategy");

    protected RollingFileManager(final String fileName, final String pattern, final OutputStream os,
            final boolean append, final long size, final long time, final TriggeringPolicy triggeringPolicy,
            final RolloverStrategy rolloverStrategy, final String advertiseURI,
            final Layout<? extends Serializable> layout, final int bufferSize, final boolean writeHeader) {
        super(fileName, os, append, false, advertiseURI, layout, bufferSize, writeHeader);
        this.size = size;
        this.initialTime = time;
        this.triggeringPolicy = triggeringPolicy;
        this.rolloverStrategy = rolloverStrategy;
        this.patternProcessor = new PatternProcessor(pattern);
        triggeringPolicy.initialize(this);
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
     * @return A RollingFileManager.
     */
    public static RollingFileManager getFileManager(final String fileName, final String pattern, final boolean append,
            final boolean bufferedIO, final TriggeringPolicy policy, final RolloverStrategy strategy,
            final String advertiseURI, final Layout<? extends Serializable> layout, final int bufferSize) {

        return (RollingFileManager) getManager(fileName, new FactoryData(pattern, append,
            bufferedIO, policy, strategy, advertiseURI, layout, bufferSize), factory);
    }

    @Override
    protected synchronized void write(final byte[] bytes, final int offset, final int length, final boolean immediateFlush) {
        size += length;
        super.write(bytes, offset, length, immediateFlush);
    }

    /**
     * Returns the current size of the file.
     * @return The size of the file in bytes.
     */
    public long getFileSize() {
        return size;
    }

    /**
     * Returns the time the file was created.
     * @return The time the file was created.
     */
    public long getFileTime() {
        return initialTime;
    }

    /**
     * Determine if a rollover should occur.
     * @param event The LogEvent.
     */
    public synchronized void checkRollover(final LogEvent event) {
        if (triggeringPolicy.isTriggeringEvent(event)) {
            rollover();
        }
    }

    public synchronized void rollover() {
        if (rollover(rolloverStrategy)) {
            try {
                size = 0;
                initialTime = System.currentTimeMillis();
                createFileAfterRollover();
            } catch (final IOException e) {
                logError("failed to create file after rollover", e);
            }
        }
    }

    protected void createFileAfterRollover() throws IOException  {
        final OutputStream os = new FileOutputStream(getFileName(), isAppend());
        if (getBufferSize() > 0) { // negative buffer size means no buffering
            setOutputStream(new BufferedOutputStream(os, getBufferSize()));
        } else {
            setOutputStream(os);
        }
    }

    /**
     * Returns the pattern processor.
     * @return The PatternProcessor.
     */
    public PatternProcessor getPatternProcessor() {
        return patternProcessor;
    }

    public void setTriggeringPolicy(final TriggeringPolicy triggeringPolicy)
    {
        triggeringPolicy.initialize(this);
        triggeringPolicyUpdater.compareAndSet(this, this.triggeringPolicy, triggeringPolicy);
    }

    public void setRolloverStrategy(final RolloverStrategy rolloverStrategy)
    {
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

        try {
            // Block until the asynchronous operation is completed.
            semaphore.acquire();
        } catch (final InterruptedException e) {
            logError("Thread interrupted while attempting to check rollover", e);
            return false;
        }

        boolean success = false;
        Thread thread = null;

        try {
            final RolloverDescription descriptor = strategy.rollover(this);
            if (descriptor != null) {
                writeFooter();
                close();
                if (descriptor.getSynchronous() != null) {
                    LOGGER.debug("RollingFileManager executing synchronous {}", descriptor.getSynchronous());
                    try {
                        success = descriptor.getSynchronous().execute();
                    } catch (final Exception ex) {
                        logError("caught error in synchronous task", ex);
                    }
                }

                if (success && descriptor.getAsynchronous() != null) {
                    LOGGER.debug("RollingFileManager executing async {}", descriptor.getAsynchronous());
                    thread = new Log4jThread(new AsyncAction(descriptor.getAsynchronous(), this));
                    thread.start();
                }
                return true;
            }
            return false;
        } finally {
            if (thread == null || !thread.isAlive()) {
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
         * Perform an action.
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
            StringBuilder builder = new StringBuilder();
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
    private static class FactoryData {
        private final String pattern;
        private final boolean append;
        private final boolean bufferedIO;
        private final int bufferSize;
        private final TriggeringPolicy policy;
        private final RolloverStrategy strategy;
        private final String advertiseURI;
        private final Layout<? extends Serializable> layout;

        /**
         * Create the data for the factory.
         * @param pattern The pattern.
         * @param append The append flag.
         * @param bufferedIO The bufferedIO flag.
         * @param advertiseURI
         * @param layout The Layout.
         * @param bufferSize the buffer size
         */
        public FactoryData(final String pattern, final boolean append, final boolean bufferedIO,
                final TriggeringPolicy policy, final RolloverStrategy strategy, final String advertiseURI,
                final Layout<? extends Serializable> layout, final int bufferSize) {
            this.pattern = pattern;
            this.append = append;
            this.bufferedIO = bufferedIO;
            this.bufferSize = bufferSize;
            this.policy = policy;
            this.strategy = strategy;
            this.advertiseURI = advertiseURI;
            this.layout = layout;
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
            StringBuilder builder = new StringBuilder();
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
         * Create the RollingFileManager.
         * @param name The name of the entity to manage.
         * @param data The data required to create the entity.
         * @return a RollingFileManager.
         */
        @Override
        public RollingFileManager createManager(final String name, final FactoryData data) {
            final File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }
            // LOG4J2-1140: check writeHeader before creating the file
            final boolean writeHeader = !data.append || !file.exists();
            try {
                file.createNewFile();
            } catch (final IOException ioe) {
                LOGGER.error("Unable to create file " + name, ioe);
                return null;
            }
            final long size = data.append ? file.length() : 0;

            OutputStream os;
            try {
                os = new FileOutputStream(name, data.append);
                int bufferSize = data.bufferSize;
                if (data.bufferedIO) {
                    os = new BufferedOutputStream(os, bufferSize);
                } else {
                    bufferSize = -1; // negative buffer size signals bufferedIO was configured false
                }
                final long time = file.lastModified(); // LOG4J2-531 create file first so time has valid value
                return new RollingFileManager(name, data.pattern, os, data.append, size, time, data.policy,
                    data.strategy, data.advertiseURI, data.layout, bufferSize, writeHeader);
            } catch (final FileNotFoundException ex) {
                LOGGER.error("FileManager (" + name + ") " + ex, ex);
            }
            return null;
        }
    }

}
