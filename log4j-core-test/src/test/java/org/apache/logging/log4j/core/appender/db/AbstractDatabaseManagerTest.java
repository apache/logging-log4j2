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
package org.apache.logging.log4j.core.appender.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;

class AbstractDatabaseManagerTest {
    // this stub is provided because mocking constructors is hard
    private static class StubDatabaseManager extends AbstractDatabaseManager {

        protected StubDatabaseManager(final String name, final int bufferSize) {
            super(name, bufferSize);
        }

        @Override
        protected boolean commitAndClose() {
            return true;
        }

        @Override
        protected void connectAndStart() {
            // noop
        }

        @Override
        protected boolean shutdownInternal() {
            return true;
        }

        @Override
        protected void startupInternal() {
            // noop
        }

        @Override
        protected void writeInternal(final LogEvent event, final Serializable serializable) {
            // noop
        }
    }

    private AbstractDatabaseManager manager;

    public void setUp(final String name, final int buffer) {
        manager = spy(new StubDatabaseManager(name, buffer));
    }

    @Test
    void testBuffering01() throws Exception {
        setUp("name", 0);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        manager.startup();
        then(manager).should().startupInternal();
        reset(manager);

        manager.write(event1, null);
        then(manager).should().writeThrough(same(event1), isNull());
        then(manager).should().connectAndStart();
        then(manager).should().isBuffered();
        then(manager).should().writeInternal(same(event1), isNull());
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
        reset(manager);

        manager.write(event2, null);
        then(manager).should().writeThrough(same(event2), isNull());
        then(manager).should().connectAndStart();
        then(manager).should().isBuffered();
        then(manager).should().writeInternal(same(event2), isNull());
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
        reset(manager);

        manager.write(event3, null);
        then(manager).should().writeThrough(same(event3), isNull());
        then(manager).should().connectAndStart();
        then(manager).should().isBuffered();
        then(manager).should().writeInternal(same(event3), isNull());
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
        reset(manager);
    }

    @Test
    void testBuffering02() throws Exception {
        setUp("name", 4);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);
        final LogEvent event4 = mock(LogEvent.class);

        final LogEvent event1copy = mock(LogEvent.class);
        final LogEvent event2copy = mock(LogEvent.class);
        final LogEvent event3copy = mock(LogEvent.class);
        final LogEvent event4copy = mock(LogEvent.class);

        when(event1.toImmutable()).thenReturn(event1copy);
        when(event2.toImmutable()).thenReturn(event2copy);
        when(event3.toImmutable()).thenReturn(event3copy);
        when(event4.toImmutable()).thenReturn(event4copy);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1, null);
        manager.write(event2, null);
        manager.write(event3, null);
        manager.write(event4, null);

        then(manager).should().connectAndStart();
        verify(manager, times(5)).isBuffered(); // 4 + 1 in flush()
        then(manager).should().writeInternal(same(event1copy), isNull());
        then(manager).should().buffer(event1);
        then(manager).should().writeInternal(same(event2copy), isNull());
        then(manager).should().buffer(event2);
        then(manager).should().writeInternal(same(event3copy), isNull());
        then(manager).should().buffer(event3);
        then(manager).should().writeInternal(same(event4copy), isNull());
        then(manager).should().buffer(event4);
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    void testBuffering03() throws Exception {
        setUp("name", 10);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        final LogEvent event1copy = mock(LogEvent.class);
        final LogEvent event2copy = mock(LogEvent.class);
        final LogEvent event3copy = mock(LogEvent.class);

        when(event1.toImmutable()).thenReturn(event1copy);
        when(event2.toImmutable()).thenReturn(event2copy);
        when(event3.toImmutable()).thenReturn(event3copy);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1, null);
        manager.write(event2, null);
        manager.write(event3, null);
        manager.flush();

        then(manager).should().connectAndStart();
        verify(manager, times(4)).isBuffered();
        then(manager).should().writeInternal(same(event1copy), isNull());
        then(manager).should().buffer(event1);
        then(manager).should().writeInternal(same(event2copy), isNull());
        then(manager).should().buffer(event2);
        then(manager).should().writeInternal(same(event3copy), isNull());
        then(manager).should().buffer(event3);
        then(manager).should().commitAndClose();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    void testBuffering04() throws Exception {
        setUp("name", 10);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);
        final LogEvent event3 = mock(LogEvent.class);

        final LogEvent event1copy = mock(LogEvent.class);
        final LogEvent event2copy = mock(LogEvent.class);
        final LogEvent event3copy = mock(LogEvent.class);

        when(event1.toImmutable()).thenReturn(event1copy);
        when(event2.toImmutable()).thenReturn(event2copy);
        when(event3.toImmutable()).thenReturn(event3copy);

        manager.startup();
        then(manager).should().startupInternal();

        manager.write(event1, null);
        manager.write(event2, null);
        manager.write(event3, null);
        manager.shutdown();

        then(manager).should().connectAndStart();
        verify(manager, times(4)).isBuffered();
        then(manager).should().writeInternal(same(event1copy), isNull());
        then(manager).should().buffer(event1);
        then(manager).should().writeInternal(same(event2copy), isNull());
        then(manager).should().buffer(event2);
        then(manager).should().writeInternal(same(event3copy), isNull());
        then(manager).should().buffer(event3);
        then(manager).should().commitAndClose();
        then(manager).should().shutdownInternal();
        then(manager).shouldHaveNoMoreInteractions();
    }

    @Test
    void testStartupShutdown01() throws Exception {
        setUp("testName01", 0);

        assertEquals("testName01", manager.getName(), "The name is not correct.");
        assertFalse(manager.isRunning(), "The manager should not have started.");

        manager.startup();
        then(manager).should().startupInternal();
        assertTrue(manager.isRunning(), "The manager should be running now.");

        manager.shutdown();
        then(manager).should().shutdownInternal();
        assertFalse(manager.isRunning(), "The manager should not be running anymore.");
    }

    @Test
    void testStartupShutdown02() throws Exception {
        setUp("anotherName02", 0);

        assertEquals("anotherName02", manager.getName(), "The name is not correct.");
        assertFalse(manager.isRunning(), "The manager should not have started.");

        manager.startup();
        then(manager).should().startupInternal();
        assertTrue(manager.isRunning(), "The manager should be running now.");

        manager.releaseSub(-1, null);
        then(manager).should().shutdownInternal();
        assertFalse(manager.isRunning(), "The manager should not be running anymore.");
    }

    @Test
    void testToString01() {
        setUp("someName01", 0);

        assertEquals("someName01", manager.toString(), "The string is not correct.");
    }

    @Test
    void testToString02() {
        setUp("bufferSize=12, anotherKey02=coolValue02", 12);

        assertEquals("bufferSize=12, anotherKey02=coolValue02", manager.toString(), "The string is not correct.");
    }
}
