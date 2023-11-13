/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Cast;

/**
 * Registry for {@link AbstractManager} instances. Keyed by name, managers can be reused by multiple appenders and
 * are released when all references have been released.
 *
 * @since 3.0.0
 */
public class ManagerRegistry {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final Map<String, AbstractManager> managers = new HashMap<>();
    private final Lock registryLock = new ReentrantLock();

    public boolean hasManager(final String name) {
        registryLock.lock();
        try {
            return managers.containsKey(name);
        } finally {
            registryLock.unlock();
        }
    }

    public <M extends AbstractManager, D> M getOrCreateManager(
            final String name, final ManagerFactory<M, D> factory, final D data) {
        registryLock.lock();
        try {
            M manager = Cast.cast(managers.get(name));
            if (manager == null) {
                manager = factory.createManager(name, data);
                if (manager == null) {
                    final String message = "Error creating manager with name '" + name + "' from ManagerFactory " +
                            factory + " using data " + data;
                    throw new ManagerException(message);
                }
                managers.put(name, manager);
            } else {
                manager.updateData(data);
            }
            manager.count++;
            return manager;
        } finally {
            registryLock.unlock();
        }
    }

    public boolean releaseManager(final AbstractManager manager, final long timeout, final TimeUnit unit) {
        final String name = manager.getName();
        registryLock.lock();
        try {
            if (--manager.count <= 0 && removeManager(name, manager)) {
                final String simpleName = manager.getClass().getSimpleName();
                LOGGER.debug("Shutting down {} {}", simpleName, name);
                final boolean stopped = manager.releaseSub(timeout, unit);
                LOGGER.debug("Shut down {} {}, all resources released: {}", simpleName, name, stopped);
                return stopped;
            }
            return true;
        } finally {
            registryLock.unlock();
        }
    }

    boolean removeManager(final String name, final AbstractManager manager) {
        return managers.remove(name, manager);
    }

}
