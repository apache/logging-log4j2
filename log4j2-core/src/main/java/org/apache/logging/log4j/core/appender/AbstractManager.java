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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class used to register managers.
 */
public abstract class AbstractManager {

    // Need to lock that map instead of using a ConcurrentMap due to stop removing the
    // manager from the map and closing the stream, requiring the whole stop method to be locked.
    private static Map<String, AbstractManager> map = new HashMap<String, AbstractManager>();

    private static Lock lock = new ReentrantLock();

    /**
     * Allow subclasses access to the status logger without creating another instance.
     */
    protected static final Logger logger = StatusLogger.getLogger();

    private String name;

    private int count;

    public StringBuilder buffer = new StringBuilder();

    public static <T extends AbstractManager> T getManager(String name, ManagerFactory<T, Object> factory,
                                                 Object data) {
        lock.lock();
        try {
            T manager = (T) map.get(name);
            if (manager == null) {
                manager = factory.createManager(name, data);
                map.put(name, manager);
            }
            manager.count++;
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

    protected AbstractManager(String name) {
        this.name = name;
    }

    public abstract void releaseSub();

    protected int getCount() {
        return count;
    }

    public void release() {
        lock.lock();
        try {
            --count;
            if (count <= 0) {
                map.remove(name);
                releaseSub();
            }
        } finally {
            lock.unlock();
        }
    }

    public String getName() {
        return name;
    }
}
