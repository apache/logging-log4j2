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
package org.apache.logging.log4j.core.appender.db;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.After;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class AbstractDatabaseManagerTest {
    private AbstractDatabaseManager manager;

    public void setUp(final String name, final int buffer) {
        this.manager = createMockBuilder(AbstractDatabaseManager.class)
                .withConstructor(String.class, int.class)
                .withArgs(name, buffer)
                .addMockedMethod("release")
                .createStrictMock();
    }

    @After
    public void tearDown() {
        verify(this.manager);
    }

    @Test
    public void testConnection01() throws Exception {
        this.setUp("testName01", 0);

        replay(this.manager);

        assertEquals("The name is not correct.", "testName01", this.manager.getName());
        assertFalse("The manager should not be connected.", this.manager.isConnected());

        verify(this.manager);
        reset(this.manager);
        this.manager.connectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.connect();
        assertTrue("The manager should be connected now.", this.manager.isConnected());

        verify(this.manager);
        reset(this.manager);
        this.manager.disconnectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.disconnect();
        assertFalse("The manager should not be connected anymore.", this.manager.isConnected());
    }

    @Test
    public void testConnection02() throws Exception {
        this.setUp("anotherName02", 0);

        replay(this.manager);

        assertEquals("The name is not correct.", "anotherName02", this.manager.getName());
        assertFalse("The manager should not be connected.", this.manager.isConnected());

        verify(this.manager);
        reset(this.manager);
        this.manager.connectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.connect();
        assertTrue("The manager should be connected now.", this.manager.isConnected());

        verify(this.manager);
        reset(this.manager);
        this.manager.disconnectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.releaseSub();
        assertFalse("The manager should not be connected anymore.", this.manager.isConnected());
    }

    @Test
    public void testToString01() {
        this.setUp("someName01", 0);

        replay(this.manager);

        assertEquals("The string is not correct.", "someName01", this.manager.toString());
    }

    @Test
    public void testToString02() {
        this.setUp("bufferSize=12, anotherKey02=coolValue02", 12);

        replay(this.manager);

        assertEquals("The string is not correct.", "bufferSize=12, anotherKey02=coolValue02", this.manager.toString());
    }

    @Test
    public void testBuffering01() throws Exception {
        this.setUp("name", 0);

        final LogEvent event1 = createStrictMock(LogEvent.class);
        final LogEvent event2 = createStrictMock(LogEvent.class);
        final LogEvent event3 = createStrictMock(LogEvent.class);

        this.manager.connectInternal();
        expectLastCall();
        this.manager.writeInternal(same(event1));
        expectLastCall();
        replay(this.manager);

        this.manager.connect();

        this.manager.write(event1);

        verify(this.manager);
        reset(this.manager);
        this.manager.writeInternal(same(event2));
        expectLastCall();
        replay(this.manager);

        this.manager.write(event2);

        verify(this.manager);
        reset(this.manager);
        this.manager.writeInternal(same(event3));
        expectLastCall();
        replay(this.manager);

        this.manager.write(event3);
    }

    @Test
    public void testBuffering02() throws Exception {
        this.setUp("name", 4);

        final LogEvent event1 = createStrictMock(LogEvent.class);
        final LogEvent event2 = createStrictMock(LogEvent.class);
        final LogEvent event3 = createStrictMock(LogEvent.class);
        final LogEvent event4 = createStrictMock(LogEvent.class);

        this.manager.connectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.connect();

        this.manager.write(event1);
        this.manager.write(event2);
        this.manager.write(event3);

        verify(this.manager);
        reset(this.manager);
        this.manager.writeInternal(same(event1));
        expectLastCall();
        this.manager.writeInternal(same(event2));
        expectLastCall();
        this.manager.writeInternal(same(event3));
        expectLastCall();
        this.manager.writeInternal(same(event4));
        expectLastCall();
        replay(this.manager);

        this.manager.write(event4);
    }

    @Test
    public void testBuffering03() throws Exception {
        this.setUp("name", 10);

        final LogEvent event1 = createStrictMock(LogEvent.class);
        final LogEvent event2 = createStrictMock(LogEvent.class);
        final LogEvent event3 = createStrictMock(LogEvent.class);

        this.manager.connectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.connect();

        this.manager.write(event1);
        this.manager.write(event2);
        this.manager.write(event3);

        verify(this.manager);
        reset(this.manager);
        this.manager.writeInternal(same(event1));
        expectLastCall();
        this.manager.writeInternal(same(event2));
        expectLastCall();
        this.manager.writeInternal(same(event3));
        expectLastCall();
        replay(this.manager);

        this.manager.flush();
    }

    @Test
    public void testBuffering04() throws Exception {
        this.setUp("name", 10);

        final LogEvent event1 = createStrictMock(LogEvent.class);
        final LogEvent event2 = createStrictMock(LogEvent.class);
        final LogEvent event3 = createStrictMock(LogEvent.class);

        this.manager.connectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.connect();

        this.manager.write(event1);
        this.manager.write(event2);
        this.manager.write(event3);

        verify(this.manager);
        reset(this.manager);
        this.manager.writeInternal(same(event1));
        expectLastCall();
        this.manager.writeInternal(same(event2));
        expectLastCall();
        this.manager.writeInternal(same(event3));
        expectLastCall();
        this.manager.disconnectInternal();
        expectLastCall();
        replay(this.manager);

        this.manager.disconnect();
    }
}
