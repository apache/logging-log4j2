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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.internal.StatusLogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;


/**
 *
 */
public class FileManager extends OutputStreamManager {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger logger = StatusLogger.getLogger();

    private boolean isAppend;
    private boolean isLocking;

    private static ManagerFactory factory = new FileManagerFactory();

    public static FileManager getFileManager(String fileName, boolean append, boolean locking, boolean bufferedIO) {

        if (locking && bufferedIO) {
            locking = false;
        }
        return (FileManager) getManager(fileName, factory, new FactoryData(fileName, append, locking, bufferedIO));
    }

    public FileManager(String fileName, OutputStream os, boolean append, boolean locking) {
        super(os, fileName);
        this.isAppend = append;
        this.isLocking = locking;
    }

    protected synchronized void write(byte[] bytes, int offset, int length)  {

        if (isLocking) {
            FileChannel channel = ((FileOutputStream)getOutputStream()).getChannel();
            try {
                /* Lock the whole file. This could be optimized to only lock from the current file
                   position. Note that locking may be advisory on some systems and mandatory on others,
                   so locking just from the current position would allow reading on systems where
                   locking is mandatory.  Also, Java 6 will throw an exception if the region of the
                   file is already locked by another FileChannel in the same JVM. Hopefully, that will
                   be avoided since every file should have a single file manager - unless two different
                   files strings are configured that somehow map to the same file.*/
                FileLock lock = channel.lock(0, Long.MAX_VALUE, false);
                try {
                    super.write(bytes, offset, length);
                } finally {
                    lock.release();
                }
            } catch (IOException ex) {
                throw new AppenderRuntimeException("Unable to obtain lock on " + getName(), ex);
            }

        } else {
            super.write(bytes, offset, length);
        }
    }

    public String getFileName() {
        return getName();
    }

    public boolean isAppend() {
        return isAppend;
    }

    public boolean isLocking() {
        return isLocking;
    }

    private static class FactoryData {
        String fileName;
        boolean append;
        boolean locking;
        boolean bufferedIO;

        public FactoryData(String fileName, boolean append, boolean locking, boolean bufferedIO) {
            this.fileName = fileName;
            this.append = append;
            this.locking = locking;
            this.bufferedIO = bufferedIO;
        }
    }

    private static class FileManagerFactory implements ManagerFactory<FileManager, FactoryData> {

        public FileManager createManager(FactoryData data) {
            File file = new File(data.fileName);
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
                return new FileManager(data.fileName, os, data.append, data.locking);
            } catch (FileNotFoundException ex) {
                logger.error("FileManager (" + data.fileName + ") " + ex);
            }
            return null;
        }
    }

}
