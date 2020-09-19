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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

@ExtendWith(MockitoExtension.class)
public class AbstractDatabaseAppenderTest {
    private static class LocalAbstractDatabaseAppender extends AbstractDatabaseAppender<LocalAbstractDatabaseManager> {

        public LocalAbstractDatabaseAppender(final String name, final Filter filter, final boolean ignoreExceptions,
                                             final LocalAbstractDatabaseManager manager) {
            super(name, filter, null, ignoreExceptions, Property.EMPTY_ARRAY, manager);
        }
    }
    private static abstract class LocalAbstractDatabaseManager extends AbstractDatabaseManager {
        public LocalAbstractDatabaseManager(final String name, final int bufferSize) {
            super(name, bufferSize);
        }
    }

    private LocalAbstractDatabaseAppender appender;

    @Mock
    private LocalAbstractDatabaseManager manager;

    public void setUp(final String name) {
        appender = new LocalAbstractDatabaseAppender(name, null, true, manager);
    }

    @Test
    public void testAppend() {
        setUp("name");
        given(manager.commitAndClose()).willReturn(true);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);

        appender.append(event1);
        then(manager).should().isBuffered();
        then(manager).should().writeThrough(same(event1), (Serializable) isNull());
        reset(manager);

        appender.append(event2);
        then(manager).should().isBuffered();
        then(manager).should().writeThrough(same(event2), (Serializable) isNull());
        reset(manager);
    }

    @Test
    public void testNameAndGetLayout01() {
        setUp("testName01");

        assertEquals("testName01", appender.getName(), "The name is not correct.");
        assertNull(appender.getLayout(), "The layout should always be null.");
    }

    @Test
    public void testNameAndGetLayout02() {
        setUp("anotherName02");

        assertEquals("anotherName02", appender.getName(), "The name is not correct.");
        assertNull(appender.getLayout(), "The layout should always be null.");
    }

    @Test
    public void testReplaceManager() throws Exception {
        setUp("name");

        final LocalAbstractDatabaseManager oldManager = appender.getManager();
        assertSame(manager, oldManager, "The manager should be the same.");

        final LocalAbstractDatabaseManager newManager = mock(LocalAbstractDatabaseManager.class);
        appender.replaceManager(newManager);
        then(manager).should().close();
        then(newManager).should().startupInternal();

        appender.stop();
        then(newManager).should().stop(0L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testStartAndStop() throws Exception {
        setUp("name");

        appender.start();
        then(manager).should().startupInternal();

        appender.stop();
        then(manager).should().stop(0L, TimeUnit.MILLISECONDS);
    }
}
