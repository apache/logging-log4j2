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

package org.apache.logging.log4j.perf.jmh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.ThreadDumpMessage;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Reproducer for LOG4J2-2965
 */
@State(Scope.Benchmark)
@Fork(100)
@Threads(1)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
public class JulReproBenchmark {

    @Setup
    public void up() {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        System.setProperty("log4j.configurationFile", "log4j2-perf2.xml");
        System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
        System.setProperty("log4j2.enable.threadlocals", "true");
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void reproducer() throws Exception {
        CountDownLatch threadsWaitingLatch = new CountDownLatch(2);
        CountDownLatch latch = new CountDownLatch(1);
        Thread t1 = startedThread("jul-logger", () -> {
            threadsWaitingLatch.countDown();
            try {
                latch.await();
                java.util.logging.Logger.getLogger("jul-repro-jul").log(Level.SEVERE, "test");
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });
        Thread t2 = startedThread("log4j2-logger", () -> {
            threadsWaitingLatch.countDown();
            try {
                latch.await();
                LogManager.getLogger("jul-repro-log4j").error("test");
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });

        threadsWaitingLatch.await();
        latch.countDown();
        t1.join(2000);
        t2.join(2000);
        if (t1.isAlive() && t2.isAlive()) {
            System.err.println(new ThreadDumpMessage("Deadlock detected").getFormattedMessage());
            t1.join();
            t2.join();
        }
    }

    private static Thread startedThread(String name, Runnable task) {
        Thread thread = new Thread(task);
        thread.setName(name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }
}
