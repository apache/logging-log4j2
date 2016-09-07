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
package org.apache.logging.log4j.core.async.perftest;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;

import org.apache.logging.log4j.core.async.perftest.ResponseTimeTest.PrintingAsyncQueueFullPolicy;

import com.lmax.disruptor.collections.Histogram;

public abstract class AbstractRunQueue implements IPerfTestRunner {

    abstract BlockingQueue<String> createQueue(int capacity);

    private static final String STOP = "STOP_TEST";
    private volatile boolean stopped = false;
    private final BlockingQueue<String> queue = createQueue(256 * 1024);
    private final Thread backGroundThread;

    AbstractRunQueue() {
        backGroundThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    try {
                        if (Objects.equals(queue.take(), STOP)) {
                            break;
                        }
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        backGroundThread.start();
    }

    @Override
    public void runThroughputTest(final int lines, final Histogram histogram) {
    }


    @Override
    public void runLatencyTest(final int samples, final Histogram histogram,
                               final long nanoTimeCost, final int threadCount) {
    }


    @Override
    public final void shutdown() {
        stopped = true;
        try {
            queue.put(STOP);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public final void log(final String finalMessage) {
        if (stopped) {
            return;
        }
        if (!queue.offer(finalMessage)) {
            PrintingAsyncQueueFullPolicy.ringbufferFull.incrementAndGet();
            try {
                queue.put(finalMessage);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
