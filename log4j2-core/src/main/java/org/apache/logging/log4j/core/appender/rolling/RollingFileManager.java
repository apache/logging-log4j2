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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.rolling.helper.Action;
import org.apache.logging.log4j.core.appender.rolling.helper.ActionBase;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;

/**
 * The Rolling File Manager.
 */
public class RollingFileManager extends FileManager {

    private static ManagerFactory factory = new RollingFileManagerFactory();

    private long size;
    private long initialTime;
    private PatternProcessor processor;
    private final Semaphore semaphore = new Semaphore(1);

    protected RollingFileManager(String fileName, String pattern, OutputStream os, boolean append, long size,
                                 long time) {
        super(fileName, os, append, false);
        this.size = size;
        this.initialTime = time;
        processor = new PatternProcessor(pattern);
    }

    /**
     * Return a RollingFileManager.
     * @param fileName The file name.
     * @param pattern The pattern for rolling file.
     * @param append true if the file should be appended to.
     * @param bufferedIO true if data should be buffered.
     * @return A RollingFileManager.
     */
    public static RollingFileManager getFileManager(String fileName, String pattern, boolean append,
                                                    boolean bufferedIO) {

        return (RollingFileManager) getManager(fileName, factory, new FactoryData(pattern, append,
            bufferedIO));
    }

    protected synchronized void write(byte[] bytes, int offset, int length) {
        size += length;
        super.write(bytes, offset, length);
    }

    /**
     * Return the current size of the file.
     * @return The size of the file in bytes.
     */
    public long getFileSize() {
        return size;
    }

    /**
     * Return the time the file was created.
     * @return The time the file was created.
     */
    public long getFileTime() {
        return initialTime;
    }

    /**
     * Determine if a rollover should occur.
     * @param event The LogEvent.
     * @param policy The TriggeringPolicy.
     * @param strategy The RolloverStrategy.
     */
    public synchronized void checkRollover(LogEvent event, TriggeringPolicy policy, RolloverStrategy strategy) {
        if (policy.isTriggeringEvent(event) && rollover(strategy)) {
            try {
                size = 0;
                initialTime = System.currentTimeMillis();
                OutputStream os = new FileOutputStream(getFileName(), isAppend());
                setOutputStream(os);
            } catch (FileNotFoundException ex) {
                LOGGER.error("FileManager (" + getFileName() + ") " + ex);
            }
        }
    }

    /**
     * Return the pattern processor.
     * @return The PatternProcessor.
     */
    public PatternProcessor getProcessor() {
        return processor;
    }

    private boolean rollover(RolloverStrategy strategy) {

        try {
            // Block until the asynchronous operation is completed.
            semaphore.acquire();
        } catch (InterruptedException ie) {
            LOGGER.error("Thread interrupted while attempting to check rollover", ie);
            return false;
        }

        RolloverDescription descriptor = strategy.rollover(this);

        if (descriptor != null) {

            close();

            boolean success = false;
            try {

                if (descriptor.getSynchronous() != null) {

                    try {
                        success = descriptor.getSynchronous().execute();
                    } catch (Exception ex) {
                        LOGGER.error("Error in synchronous task", ex);
                    }
                }

                if (success) {
                    Action async = new AsyncAction(descriptor.getAsynchronous(), this);
                    if (async != null) {
                        new Thread(async).start();
                    }
                }
            } finally {
                if (!success && semaphore.availablePermits() == 0) {
                    semaphore.release();
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Performs actions asynchronously.
     */
    private static class AsyncAction extends ActionBase {

        private final Action action;
        private final RollingFileManager manager;

        /**
         * Constructor.
         * @param act The action to perform.
         * @param manager The manager.
         */
        public AsyncAction(Action act, RollingFileManager manager) {
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
        public void close() {
            action.close();
        }

        /**
         * Determines if action has been completed.
         *
         * @return true if action is complete.
         */
        public boolean isComplete() {
            return action.isComplete();
        }
    }

    /**
     * Factory data.
     */
    private static class FactoryData {
        private String pattern;
        private boolean append;
        private boolean bufferedIO;

        /**
         * Create the data for the factory.
         * @param pattern The pattern.
         * @param append The append flag.
         * @param bufferedIO The bufferedIO flag.
         */
        public FactoryData(String pattern, boolean append, boolean bufferedIO) {
            this.pattern = pattern;
            this.append = append;
            this.bufferedIO = bufferedIO;
        }
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
        public RollingFileManager createManager(String name, FactoryData data) {
            File file = new File(name);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException ioe) {
                LOGGER.error("Unable to create file " + name, ioe);
                return null;
            }
            long size = data.append ? file.length() : 0;
            long time = file.lastModified();

            OutputStream os;
            try {
                os = new FileOutputStream(name, data.append);
                if (data.bufferedIO) {
                    os = new BufferedOutputStream(os);
                }
                return new RollingFileManager(name, data.pattern, os, data.append, size, time);
            } catch (FileNotFoundException ex) {
                LOGGER.error("FileManager (" + name + ") " + ex);
            }
            return null;
        }
    }

}
