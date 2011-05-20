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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.core.appender.rolling.helper.Action;
import org.apache.logging.log4j.core.appender.rolling.helper.ActionBase;
import org.apache.logging.log4j.internal.StatusLogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class RollingFileManager extends FileManager {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger logger = StatusLogger.getLogger();

    private long size;
    private long initialTime;
    private PatternProcessor processor;
    private final Semaphore semaphore = new Semaphore(1);
    private static int count = 0;

    private static ManagerFactory factory = new RollingFileManagerFactory();

    public static RollingFileManager getFileManager(String fileName, String pattern, boolean append,
                                                    boolean bufferedIO) {

        return (RollingFileManager) getManager(fileName, factory, new FactoryData(fileName, pattern, append,
            bufferedIO));
    }

    public RollingFileManager(String fileName, String pattern, OutputStream os, boolean append, long size, long time) {
        super(fileName, os, append, false);
        this.size = size;
        this.initialTime = time;
        processor = new PatternProcessor(pattern);
    }


    protected synchronized void write(byte[] bytes, int offset, int length) {
        size += length;
        super.write(bytes, offset, length);
    }

    public long getFileSize() {
        return size;
    }

    public long getFileTime() {
        return initialTime;
    }

    public synchronized void checkRollover(LogEvent event, TriggeringPolicy policy, RolloverStrategy strategy) {
        if (policy.isTriggeringEvent(event)) {
            if (rollover(strategy)) {
                try {
                    size = 0;
                    initialTime = System.currentTimeMillis();
                    OutputStream os = new FileOutputStream(getFileName(), isAppend());
                    setOutputStream(os);
                } catch (FileNotFoundException ex) {
                    logger.error("FileManager (" + getFileName() + ") " + ex);
                }
            }
        }
    }

    public PatternProcessor getProcessor() {
        return processor;
    }

    private boolean rollover(RolloverStrategy strategy) {

        try {
            // Block until the asynchronous operation is completed.
            semaphore.acquire();
        } catch (InterruptedException ie) {
            logger.error("Thread interrupted while attempting to check rollover", ie);
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
                        logger.error("Error in synchronous task", ex);
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

    private static class AsyncAction extends ActionBase {

        private final Action action;
        private final RollingFileManager manager;

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

    private static class FactoryData {
        String fileName;
        String pattern;
        boolean append;
        boolean bufferedIO;

        public FactoryData(String fileName, String pattern, boolean append, boolean bufferedIO) {
            this.fileName = fileName;
            this.pattern = pattern;
            this.append = append;
            this.bufferedIO = bufferedIO;
        }
    }

    private static class RollingFileManagerFactory implements ManagerFactory<RollingFileManager, FactoryData> {

        public RollingFileManager createManager(FactoryData data) {
            File file = new File(data.fileName);
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException ioe) {
                logger.error("Unable to create file " + data.fileName, ioe);
                return null;
            }
            long size = data.append ? file.length() : 0;
            long time = file.lastModified();

            OutputStream os;
            try {
                os = new FileOutputStream(data.fileName, data.append);
                if (data.bufferedIO) {
                    os = new BufferedOutputStream(os);
                }
                return new RollingFileManager(data.fileName, data.pattern, os, data.append, size, time);
            } catch (FileNotFoundException ex) {
                logger.error("FileManager (" + data.fileName + ") " + ex);
            }
            return null;
        }
    }

}
