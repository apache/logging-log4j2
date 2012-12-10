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
package org.apache.logging.log4j.core.helpers;

import org.apache.logging.log4j.core.helpers.UUIDUtil;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertTrue;
/**
 *
 */
public class UUIDTest {

    private static final int COUNT = 200;
    private static final int THREADS = 10;

    private static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

    @Test
    public void testTimeBaseUUID() {
        final UUID uuid = UUIDUtil.getTimeBasedUUID();
        final UUID uuid2 = UUIDUtil.getTimeBasedUUID();
        final long current = (System.currentTimeMillis() * 10000) + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
        final long time = uuid.timestamp();
        assertTrue("Incorrect time", current + 10000 - time > 0);
        final UUID[] uuids = new UUID[COUNT];
        final long start = System.nanoTime();
        for (int i=0; i < COUNT; ++i) {
            uuids[i] = UUIDUtil.getTimeBasedUUID();
        }
        final long elapsed = System.nanoTime() - start;
        System.out.println("Elapsed for " + COUNT + " UUIDS = " + elapsed + " Average = " + elapsed / COUNT + " ns");
        int errors = 0;
        for (int i=0; i < COUNT; ++i) {
            for (int j=i+1; j < COUNT; ++j) {
                if (uuids[i].equals(uuids[j])) {
                    ++errors;
                    System.out.println("UUID " + i + " equals UUID " + j);
                }
            }
        }
        assertTrue(errors + " duplicate UUIDS", errors == 0);
        final int variant = uuid.variant();
        assertTrue("Incorrect variant. Expected 2 got " + variant, variant == 2);
        final int version = uuid.version();
        assertTrue("Incorrect version. Expected 1 got " + version, version == 1);
        final long node = uuid.node();
        assertTrue("Invalid node", node != 0);
    }

    @Test
    public void testThreads() throws Exception {
        final Thread[] threads = new Thread[THREADS];
        final UUID[] uuids = new UUID[COUNT * THREADS];
        final long[] elapsed = new long[THREADS];
        for (int i=0; i < THREADS; ++i) {
            threads[i] = new Worker(uuids, elapsed, i, COUNT);
        }
        for (int i=0; i < THREADS; ++i) {
            threads[i].start();
        }
        long elapsedTime = 0;
        for (int i=0; i < THREADS; ++i) {
            threads[i].join();
            elapsedTime += elapsed[i];
        }
        System.out.println("Elapsed for " + COUNT * THREADS + " UUIDS = " + elapsedTime + " Average = " +
                elapsedTime / (COUNT * THREADS) + " ns");
        int errors = 0;
        for (int i=0; i < COUNT * THREADS; ++i) {
            for (int j=i+1; j < COUNT * THREADS; ++j) {
                if (uuids[i].equals(uuids[j])) {
                    ++errors;
                    System.out.println("UUID " + i + " equals UUID " + j);
                }
            }
        }
        assertTrue(errors + " duplicate UUIDS", errors == 0);
    }



    private class Worker extends Thread {

        private final UUID[] uuids;
        private final long[] elapsed;
        private final int index;
        private final int count;

        public Worker(final UUID[] uuids, final long[] elapsed, final int index, final int count) {
            this.uuids = uuids;
            this.index = index;
            this.count = count;
            this.elapsed = elapsed;
        }

        @Override
        public void run() {
            final int pos = index * count;
            final long start = System.nanoTime();
            for (int i=pos; i < pos + count; ++i) {
                uuids[i] = UUIDUtil.getTimeBasedUUID();
            }
            elapsed[index] = System.nanoTime() - start;
        }
    }
}
