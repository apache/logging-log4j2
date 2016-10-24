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
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

public class AbstractDatabaseManagerTest {
    private AbstractDatabaseManager manager;

    public void setUp(final String name, final int buffer) {
        manager = spy(new StubDatabaseManager(name, buffer));
    }

    @Test
    public void testStartupShutdown01() throws Exception {
        setUp("testName01", 0);

        assertEquals("The name is not correct.", "testName01", manager.getName());
        assertFalse("The manager should not have started.", manager.isRunning());

        manager.startup();
        then(manager).should().startupInternal();
        assertTrue("The manager should be running now.", manager.isRunning());

        manager.shutdown();
        then(manager).should().shutdownInternal();
        assertFalse("The manager should not be running anymore.", manager.isRunning());
    }

    @Test
    public void testStartupShutdown02() throws Exception {
        setUp("anotherName02", 0);

        assertEquals("The name is not correct.", "anotherName02", manager.getName());
        assertFalse("The manager should not have started.", manager.isRunning());

        manager.startup();
        then(manager).should().startupInternal();
        assertTrue("The manager should be running now.", manager.isRunning());

        manager.releaseSub(-1, null);
        then(manager).should().shutdownInternal();
        assertFalse("The manager should not be running anymore.", manager.isRunning());
    }

    @Test
    public void testToString01() {
        setUp("someName01", 0);

        assertEquals("The string is not correct.", "someName01", manager.toString());
    }

    @Test
    public void testToString02() {
        setUp("bufferSize=12, anotherKey02=coolValue02", 12);

        assertEquals("The string is not correct.", "bufferSize=12, anotherKey02=coolValue02", manager.toString());
    }

    @Test
    public void testBuffering01() throws Exception {
        setUp("name", 0);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        manager.startup();
        then(manager).should().startupInternal();
        reset(manager);

        manager.write(event1);
        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event1));
        then(manager).should().commitAndClose();
        reset(manager);

        manager.write(event2);
        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event2));
        then(manager).should().commitAndClose();
        reset(manager);

        manager.write(event3);
        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event3));
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    public void testBuffering02() throws Exception {
        setUp("name", 4);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);
        final LogEvent event4 = mock(LogEvent.class);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1);
        manager.write(event2);
        manager.write(event3);
        manager.write(event4);

        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event1));
        then(manager).should().writeInternal(same(event2));
        then(manager).should().writeInternal(same(event3));
        then(manager).should().writeInternal(same(event4));
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    public void testBuffering03() throws Exception {
        setUp("name", 10);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1);
        manager.write(event2);
        manager.write(event3);
        manager.flush();

        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event1));
        then(manager).should().writeInternal(same(event2));
        then(manager).should().writeInternal(same(event3));
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    public void testBuffering04() throws Exception {
        setUp("name", 10);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1);
        manager.write(event2);
        manager.write(event3);
        manager.shutdown();

        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event1));
        then(manager).should().writeInternal(same(event2));
        then(manager).should().writeInternal(same(event3));
        then(manager).should().commitAndClose();
        then(manager).should().shutdownInternal();
        then(manager).shouldHaveNoMoreInteractions();
    }

    // this stub is provided because mocking constructors is hard
    private static class StubDatabaseManager extends AbstractDatabaseManager {

        protected StubDatabaseManager(final String name, final int bufferSize) {
            super(name, bufferSize);
        }

        @Override
        protected void startupInternal() throws Exception {
        }

        @Override
        protected boolean shutdownInternal() throws Exception {
            return true;
        }

        @Override
        protected void connectAndStart() {
        }

        @Override
        protected void writeInternal(final LogEvent event) {
        }

        @Override
        protected boolean commitAndClose() {
            return true;
        }
    }
}
