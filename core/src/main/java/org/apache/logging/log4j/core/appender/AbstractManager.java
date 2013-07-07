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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Abstract base class used to register managers.
 */
public abstract class AbstractManager {

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger LOGGER = StatusLogger.getLogger();

    // Need to lock that map instead of using a ConcurrentMap due to stop removing the
    // manager from the map and closing the stream, requiring the whole stop method to be locked.
    private static final Map<String, AbstractManager> MAP = new HashMap<String, AbstractManager>();

    private static final Lock LOCK = new ReentrantLock();

    /**
     * Number of Appenders using this manager.
     */
    protected int count;

    private final String name;

    protected AbstractManager(final String name) {
        this.name = name;
        LOGGER.debug("Starting {} {}", this.getClass().getSimpleName(), name);
    }

    /**
     * Retrieves a Manager if it has been previously created or creates a new Manager.
     * @param name The name of the Manager to retrieve.
     * @param factory The Factory to use to create the Manager.
     * @param data An Object that should be passed to the factory when creating the Manager.
     * @param <M> The Type of the Manager to be created.
     * @param <T> The type of the Factory data.
     * @return A Manager with the specified name and type.
     */
    public static <M extends AbstractManager, T> M getManager(final String name, final ManagerFactory<M, T> factory,
                                                              final T data) {
        LOCK.lock();
        try {
            @SuppressWarnings("unchecked")
            M manager = (M) MAP.get(name);
            if (manager == null) {
                manager = factory.createManager(name, data);
                if (manager == null) {
                    throw new IllegalStateException("Unable to create a manager");
                }
                MAP.put(name, manager);
            }
            manager.count++;
            return manager;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Determines if a Manager with the specified name exists.
     * @param name The name of the Manager.
     * @return True if the Manager exists, false otherwise.
     */
    public static boolean hasManager(final String name) {
        LOCK.lock();
        try {
            return MAP.containsKey(name);
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * May be overridden by Managers to perform processing while the Manager is being released and the
     * lock is held.
     */
    protected void releaseSub() {
    }

    protected int getCount() {
        return count;
    }

    /**
     * Called to signify that this Manager is no longer required by an Appender.
     */
    public void release() {
        LOCK.lock();
        try {
            --count;
            if (count <= 0) {
                MAP.remove(name);
                LOGGER.debug("Shutting down {} {}", this.getClass().getSimpleName(), getName());
                releaseSub();
            }
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Returns the name of the Manager.
     * @return The name of the Manager.
     */
    public String getName() {
        return name;
    }

    /**
     * Provide a description of the content format supported by this Manager.  Default implementation returns an empty
     * (unspecified) Map.
     *
     * @return a Map of key/value pairs describing the Manager-specific content format, or an empty Map if no content
     * format descriptors are specified.
     */
    public Map<String, String> getContentFormat() {
        return new HashMap<String, String>();
    }
}
