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
package org.apache.logging.log4j.async;

import com.lmax.disruptor.util.Util;


public class CachedClock implements Clock {
    private static CachedClock instance = new CachedClock();
    private volatile long millis = System.currentTimeMillis();
    private volatile short count = 0;
    private final Thread updater = new Thread("Clock Updater Thread") {
        public void run() {
            while (true) {
                long time = System.currentTimeMillis();
                millis = time;
                Util.getUnsafe().park(true, time + 1); // abs (millis)
                // Util.getUnsafe().park(false, 1000 * 1000);// relative(nanos)
            }
        }
    };

    public static CachedClock instance() {
        return instance;
    }

    private CachedClock() {
        updater.setDaemon(true);
        updater.start();
    }

    // @Override
    public long currentTimeMillis() {

        // improve granularity: also update time field every 1024 calls.
        // (the bit fiddling means we don't need to worry about overflows)
        if ((++count & 0x3FF) == 0x3FF) {
            millis = System.currentTimeMillis();
        }
        return millis;
    }
}
