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
package org.apache.logging.log4j.message;

import org.junit.Test;

import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class ThreadDumpMessageTest {

    @Test
    public void testMessage() {
        final ThreadDumpMessage msg = new ThreadDumpMessage("Testing");

        final String message = msg.getFormattedMessage();
        //System.out.print(message);
        assertTrue("No header", message.contains("Testing"));
        assertTrue("No RUNNABLE", message.contains("RUNNABLE"));
        assertTrue("No ThreadDumpMessage", message.contains("ThreadDumpMessage"));
    }


    @Test
    public void testMessageWithLocks() throws Exception {
        final ReentrantLock lock = new ReentrantLock();
        lock.lock();
        final Thread thread1 = new Thread1(lock);
        thread1.start();
        ThreadDumpMessage msg;
        synchronized(this) {
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
        //System.out.print(message);
        assertTrue("No header", message.contains("Testing"));
        assertTrue("No RUNNABLE", message.contains("RUNNABLE"));
        assertTrue("No ThreadDumpMessage", message.contains("ThreadDumpMessage"));
        //assertTrue("No Locks", message.contains("waiting on"));
        //assertTrue("No syncronizers", message.contains("locked syncrhonizers"));
    }

    @Test
    public void testToString() {
        final ThreadDumpMessage msg = new ThreadDumpMessage("Test");
        final String expected = "ThreadDumpMessage[Title=\"Test\"]";
        assertEquals(expected, msg.toString());
    }

    private class Thread1 extends Thread {
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

    private class Thread2 extends Thread {
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
}
