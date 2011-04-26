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
import org.apache.logging.log4j.core.appender.CompressionType;
import org.apache.logging.log4j.core.appender.FileManager;
import org.apache.logging.log4j.core.appender.ManagerFactory;
import org.apache.logging.log4j.internal.StatusLogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 *
 */
public class RollingFileManager extends FileManager {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger logger = StatusLogger.getLogger();

    private CompressionType type = null;
    private long size;
    private long initialTime;
    private PatternProcessor processor;

    private static ManagerFactory factory = new RollingFileManagerFactory();

    public static RollingFileManager getFileManager(String fileName, String pattern, boolean append, boolean bufferedIO,
                                                    CompressionType type) {

        return (RollingFileManager) getManager(fileName, factory, new FactoryData(fileName, pattern, append, bufferedIO,
                                               type));
    }

    public RollingFileManager(String fileName, String pattern, OutputStream os, boolean append, long size,
                              CompressionType type, long time) {
        super(fileName, os, append, false);
        this.size = size;
        this.type = type;
        this.initialTime = time;
        processor = new PatternProcessor(pattern);
    }

    @Override
    public void release() {
        super.release();
        if (!isOpen() && type != null) {
            doCompress();
        }
    }

    protected synchronized void write(byte[] bytes, int offset, int length)  {
        size += length;
        super.write(bytes, offset, length);
    }

    public CompressionType getCompressionType() {
        return this.type;
    }

    private void doCompress() {

    }
    public long getFileSize() {
        return size;
    }

    public long getFileTime() {
        return initialTime;
    }

    public synchronized void checkRollover(LogEvent event, TriggeringPolicy policy, RolloverStrategy strategy) {
        if (policy.isTriggeringEvent(event)) {
            close();
            strategy.rollover(this);
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

    public PatternProcessor getProcessor() {
        return processor;
    }

    private static class FactoryData {
        String fileName;
        String pattern;
        boolean append;
        boolean bufferedIO;
        CompressionType type;

        public FactoryData(String fileName, String pattern, boolean append, boolean bufferedIO, CompressionType type) {
            this.fileName = fileName;
            this.pattern = pattern;
            this.append = append;
            this.bufferedIO = bufferedIO;
            this.type = type;
        }
    }

    private static class RollingFileManagerFactory implements ManagerFactory<RollingFileManager, FactoryData> {

        public RollingFileManager createManager(FactoryData data) {
            File file = new File(data.fileName);
            long size = data.append ? file.length() : 0;
            long time = file.lastModified();
            final File parent = file.getParentFile();
            if (null != parent && !parent.exists()) {
                parent.mkdirs();
            }

            OutputStream os;
            try {
                os = new FileOutputStream(data.fileName, data.append);
                if (data.bufferedIO) {
                    os = new BufferedOutputStream(os);
                }
                return new RollingFileManager(data.fileName, data.pattern, os, data.append, size, data.type, time);
            } catch (FileNotFoundException ex) {
                logger.error("FileManager (" + data.fileName + ") " + ex);
            }
            return null;
        }
    }

}
