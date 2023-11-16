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
package org.apache.logging.log4j.message;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;

public class ThreadDumpMessageTest {

    @Test
    public void testMessage() {
        final ThreadDumpMessage msg = new ThreadDumpMessage("Testing");

        final String message = msg.getFormattedMessage();
        // System.out.print(message);
        assertTrue(message.contains("Testing"), "No header");
        assertTrue(message.contains("RUNNABLE"), "No RUNNABLE");
        assertTrue(message.contains("ThreadDumpMessage"), "No ThreadDumpMessage");
    }

    @Test
    public void testMessageWithLocks() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final Thread thread1 = new Thread1(lock);
        thread1.start();
        ThreadDumpMessage msg;
        synchronized (this) {
            final Thread thread2 = new Thread2(this);
            thread2.start();
            try {
                Thread.sleep(200);
                msg = new ThreadDumpMessage("Testing");
            } finally {
                lock.unlock();
            }
        }

        final String message = msg.getFormattedMessage();
        // System.out.print(message);
        assertTrue(message.contains("Testing"), "No header");
        assertTrue(message.contains("RUNNABLE"), "No RUNNABLE");
        assertTrue(message.contains("ThreadDumpMessage"), "No ThreadDumpMessage");
        // assertTrue("No Locks", message.contains("waiting on"));
        // assertTrue("No syncronizers", message.contains("locked syncrhonizers"));
    }

    @Test
    public void testToString() {
        final ThreadDumpMessage msg = new ThreadDumpMessage("Test");
        final String actual = msg.toString();
        assertTrue(actual.contains("Test"));
        assertTrue(actual.contains("RUNNABLE"));
        assertTrue(actual.contains(getClass().getName()));
    }

    @Test
    public void testUseConstructorThread() throws InterruptedException { // LOG4J2-763
        final ThreadDumpMessage msg = new ThreadDumpMessage("Test");

        final AtomicReference<String> actual = new AtomicReference<>();
        final Thread other = new Thread("OtherThread") {
            @Override
            public void run() {
                actual.set(msg.getFormattedMessage());
            }
        };
        other.start();
        other.join();

        assertFalse(actual.get().contains("OtherThread"), "No mention of other thread in msg");
    }

    @Test
    public void formatTo_usesCachedMessageString() throws Exception {

        final ThreadDumpMessage message = new ThreadDumpMessage("");
        final String initial = message.getFormattedMessage();
        assertFalse(initial.contains("ThreadWithCountDownLatch"), "no ThreadWithCountDownLatch thread yet");

        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch keepAlive = new CountDownLatch(1);
        final ThreadWithCountDownLatch thread = new ThreadWithCountDownLatch(started, keepAlive);
        thread.start();
        started.await(); // ensure thread is running

        final StringBuilder result = new StringBuilder();
        message.formatTo(result);
        assertFalse(result.toString().contains("ThreadWithCountDownLatch"), "no ThreadWithCountDownLatch captured");
        assertEquals(initial, result.toString());
        keepAlive.countDown(); // allow thread to die
    }

    private static class Thread1 extends Thread {
        private final ReentrantLock lock;

        public Thread1(final ReentrantLock lock) {
            this.lock = lock;
        }

        @Override
        public void run() {
            lock.lock();
            lock.unlock();
        }
    }

    private static class Thread2 extends Thread {
        private final Object obj;

        public Thread2(final Object obj) {
            this.obj = obj;
        }

        @Override
        public void run() {
            synchronized (obj) {
            }
        }
    }

    private static class ThreadWithCountDownLatch extends Thread {
        private final CountDownLatch started;
        private final CountDownLatch keepAlive;
        volatile boolean finished;

        public ThreadWithCountDownLatch(final CountDownLatch started, final CountDownLatch keepAlive) {
            super("ThreadWithCountDownLatch");
            this.started = started;
            this.keepAlive = keepAlive;
            setDaemon(true);
        }

        @Override
        public void run() {
            started.countDown();
            try {
                keepAlive.await();
            } catch (final InterruptedException e) {
                // ignored
            }
            finished = true;
        }
    }
}
