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
    private static final int UPDATE_THRESHOLD = 0x3FF;
    private static volatile CachedClock instance;
    private static final Object INSTANCE_LOCK = new Object();
    private volatile long millis = System.currentTimeMillis();
    private volatile short count = 0;

    private CachedClock() {
        final Thread updater = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    final long time = System.currentTimeMillis();
                    millis = time;

                    // avoid explicit dependency on sun.misc.Util
                    LockSupport.parkNanos(1000 * 1000);
                }
            }
        }, "Clock Updater Thread");
        updater.setDaemon(true);
        updater.start();
    }

    public static CachedClock instance() {
        // LOG4J2-819: use lazy initialization of threads
        if (instance == null) {
            synchronized (INSTANCE_LOCK) {
                if (instance == null) {
                    instance = new CachedClock();
                }
            }
        }
        return instance;
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

        // improve granularity: also update time field every 1024 calls.
        // (the bit fiddling means we don't need to worry about overflows)
        if ((++count & UPDATE_THRESHOLD) == UPDATE_THRESHOLD) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }
}
