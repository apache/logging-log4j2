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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests the ReusableMessageFactory class.
 */
public class ReusableMessageFactoryTest {

    @Test
    public void testCreateEventReturnsDifferentInstanceIfNotReleased() throws Exception {
        final ReusableMessageFactory factory = new ReusableMessageFactory();
        final Message message1 = factory.newMessage("text, p0={} p1={} p2={} p3={}", 1, 2, 3, 4);
        final Message message2 = factory.newMessage("text, p0={} p1={} p2={} p3={}", 9, 8, 7, 6);
        assertNotSame(message1, message2);
        ReusableMessageFactory.release(message1);
        ReusableMessageFactory.release(message2);
    }

    @Test
    public void testCreateEventReturnsSameInstance() throws Exception {
        final ReusableMessageFactory factory = new ReusableMessageFactory();
        final Message message1 = factory.newMessage("text, p0={} p1={} p2={} p3={}", 1, 2, 3, 4);

        ReusableMessageFactory.release(message1);
        final Message message2 = factory.newMessage("text, p0={} p1={} p2={} p3={}", 9, 8, 7, 6);
        assertSame(message1, message2);

        ReusableMessageFactory.release(message2);
        final Message message3 = factory.newMessage("text, AAA={} BBB={} p2={} p3={}", 9, 8, 7, 6);
        assertSame(message2, message3);
        ReusableMessageFactory.release(message3);
    }

    private void assertReusableParameterizeMessage(final Message message, final String txt, final Object[] params) {
        assertTrue(message instanceof ReusableParameterizedMessage);
        final ReusableParameterizedMessage msg = (ReusableParameterizedMessage) message;
        assertTrue(msg.reserved, "reserved");

        assertEquals(txt, msg.getFormat());
        assertEquals(msg.getParameterCount(), params.length, "count");
        final Object[] messageParams = msg.getParameters();
        for (int i = 0; i < params.length; i++) {
            assertEquals(messageParams[i], params[i]);
        }
    }

    @Test
    public void testCreateEventOverwritesFields() throws Exception {
        final ReusableMessageFactory factory = new ReusableMessageFactory();
        final Message message1 = factory.newMessage("text, p0={} p1={} p2={} p3={}", 1, 2, 3, 4);
        assertReusableParameterizeMessage(message1, "text, p0={} p1={} p2={} p3={}", new Object[]{
                new Integer(1), //
                new Integer(2), //
                new Integer(3), //
                new Integer(4), //
        });

        ReusableMessageFactory.release(message1);
        final Message message2 = factory.newMessage("other, A={} B={} C={} D={}", 1, 2, 3, 4);
        assertReusableParameterizeMessage(message1, "other, A={} B={} C={} D={}", new Object[]{
                new Integer(1), //
                new Integer(2), //
                new Integer(3), //
                new Integer(4), //
        });
        assertSame(message1, message2);
        ReusableMessageFactory.release(message2);
    }

    @Test
    public void testCreateEventReturnsThreadLocalInstance() throws Exception {
        final ReusableMessageFactory factory = new ReusableMessageFactory();
        final AtomicReference<Message> message1 = new AtomicReference<>();
        final AtomicReference<Message> message2 = new AtomicReference<>();
        final Thread t1 = new Thread("THREAD 1") {
            @Override
            public void run() {
                message1.set(factory.newMessage("text, p0={} p1={} p2={} p3={}", 1, 2, 3, 4));
            }
        };
        final Thread t2 = new Thread("Thread 2") {
            @Override
            public void run() {
                message2.set(factory.newMessage("other, A={} B={} C={} D={}", 1, 2, 3, 4));
            }
        };
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertNotNull(message1.get());
        assertNotNull(message2.get());
        assertNotSame(message1.get(), message2.get());
        assertReusableParameterizeMessage(message1.get(), "text, p0={} p1={} p2={} p3={}", new Object[]{
                new Integer(1), //
                new Integer(2), //
                new Integer(3), //
                new Integer(4), //
        });

        assertReusableParameterizeMessage(message2.get(), "other, A={} B={} C={} D={}", new Object[]{
                new Integer(1), //
                new Integer(2), //
                new Integer(3), //
                new Integer(4), //
        });
        ReusableMessageFactory.release(message1.get());
        ReusableMessageFactory.release(message2.get());
    }

}