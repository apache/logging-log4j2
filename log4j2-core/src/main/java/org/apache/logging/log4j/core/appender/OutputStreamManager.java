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
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.internal.StatusLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Manage an OutputStream so that it can be shared by multiple Appenders and will
 * allow appenders to reconfigure without requiring a new stream.
 */
public class OutputStreamManager {

    // Need to lock that map instead of using a ConcurrentMap due to stop removing the
    // manager from the map and closing the stream, requiring the whole stop method to be locked.
    private static Map<String, OutputStreamManager> map = new HashMap<String, OutputStreamManager>();

    private static Lock lock = new ReentrantLock();

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger logger = StatusLogger.getLogger();

    private OutputStream os;
    private String name;

    private int count;

    private byte[] header = null;

    private byte[] footer = null;

    public StringBuilder buffer = new StringBuilder();

    public static OutputStreamManager getManager(String name, ManagerFactory<OutputStreamManager, Object> factory,
                                                 Object data) {
        lock.lock();
        try {
            OutputStreamManager manager = map.get(name);
            if (manager == null) {
                manager = factory.createManager(data);
                map.put(name, manager);
            }
            manager.count++;
            //System.out.println("Using manager " + name + " " + manager.count);
            return manager;
        } finally {
            lock.unlock();
        }
    }

    public static boolean hasManager(String name) {
        lock.lock();
        try {
            return map.containsKey(name);
        } finally {
            lock.unlock();
        }
    }

    public OutputStreamManager(OutputStream os, String streamName) {
        this.os = os;
        this.name = streamName;
    }

    public synchronized void setHeader(byte[] header) {
        if (header == null) {
            this.header = header;
        }
    }

    public synchronized void setFooter(byte[] footer) {
        if (footer == null) {
            this.footer = footer;
        }
    }

    public void release() {
        lock.lock();
        try {
            --count;
            //System.out.println("Released " + name + " " + count);
            if (count <= 0) {
                map.remove(name);
                if (footer != null) {
                    write(footer);
                }
                close();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isOpen() {
        return count > 0;
    }

    protected OutputStream getOutputStream() {
        return os;
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @param offset The offset into the byte array.
     * @param length The number of bytes to write.
     * @throws AppenderRuntimeException if an error occurs.
     */
    protected synchronized void write(byte[] bytes, int offset, int length)  {
        //System.out.println("write " + count);
        try {
            os.write(bytes, offset, length);
        } catch (IOException ex) {
            String msg = "Error writing to stream " + name;
            throw new AppenderRuntimeException(msg, ex);
        }
    }

    /**
     * Some output streams synchronize writes while others do not. Synchronizing here insures that
     * log events won't be intertwined.
     * @param bytes The serialized Log event.
     * @throws AppenderRuntimeException if an error occurs.
     */
    protected void write(byte[] bytes)  {
        write(bytes, 0, bytes.length);
    }

    protected void close() {
        if (os == System.out || os == System.err) {
            return;
        }
        try {
            os.close();
        } catch (IOException ex) {
            logger.error("Unable to close stream " + name + ". " + ex);
        }
    }

    public void flush() {
        try {
            os.flush();
        } catch (IOException ex) {
            String msg = "Error flushing stream " + name;
            throw new AppenderRuntimeException(msg, ex);
        }
    }

    public String getName() {
        return name;
    }
}
