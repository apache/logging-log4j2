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
package org.apache.logging.log4j.core.util;

import java.util.concurrent.locks.LockSupport;

/**
 * Implementation of the {@code Clock} interface that tracks the time in a
 * private long field that is updated by a background thread once every
 * millisecond. Timers on most platforms do not have millisecond granularity, so
 * the returned value may "jump" every 10 or 16 milliseconds. To reduce this
 * problem, this class also updates the internal time value every 1024 calls to
 * {@code currentTimeMillis()}.
 */
public final class CachedClock implements Clock {
    private static final int UPDATE_THRESHOLD = 1000;
    private static volatile CachedClock instance;
    private static final Object INSTANCE_LOCK = new Object();
    private volatile long millis = System.currentTimeMillis();
    private short count = 0;

    private CachedClock() {
        final Thread updater = new Log4jThread(
                () -> {
                    while (true) {
                        final long time = System.currentTimeMillis();
                        millis = time;

                        // avoid explicit dependency on sun.misc.Util
                        LockSupport.parkNanos(1000 * 1000);
                    }
                },
                "CachedClock Updater Thread");
        updater.setDaemon(true);
        updater.start();
    }

    public static CachedClock instance() {
        // LOG4J2-819: use lazy initialization of threads
        CachedClock result = instance;
        if (result == null) {
            synchronized (INSTANCE_LOCK) {
                result = instance;
                if (result == null) {
                    instance = result = new CachedClock();
                }
            }
        }
        return result;
    }

    /**
     * Returns the value of a private long field that is updated by a background
     * thread once every millisecond. Timers on most platforms do not
     * have millisecond granularity, the returned value may "jump" every 10 or
     * 16 milliseconds. To reduce this problem, this method also updates the
     * internal time value every 1024 calls.
     * @return the cached time
     */
    @Override
    public long currentTimeMillis() {

        // The count field is not volatile on purpose to reduce contention on this field.
        // This means that some threads may not see the increments made to this field
        // by other threads. This is not a problem: the timestamp does not need to be
        // updated exactly every 1000 calls.
        if (++count > UPDATE_THRESHOLD) {
            millis = System.currentTimeMillis(); // update volatile field: store-store barrier
            count = 0; // after a memory barrier: this change _is_ visible to other threads
        }
        return millis;
    }
}
