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

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

@RunWith(MockitoJUnitRunner.class)
public class AbstractDatabaseAppenderTest {
    private LocalAbstractDatabaseAppender appender;
    @Mock
    private LocalAbstractDatabaseManager manager;

    public void setUp(final String name) {
        appender = new LocalAbstractDatabaseAppender(name, null, true, manager);
    }

    @Test
    public void testNameAndGetLayout01() {
        setUp("testName01");

        assertEquals("The name is not correct.", "testName01", appender.getName());
        assertNull("The layout should always be null.", appender.getLayout());
    }

    @Test
    public void testNameAndGetLayout02() {
        setUp("anotherName02");

        assertEquals("The name is not correct.", "anotherName02", appender.getName());
        assertNull("The layout should always be null.", appender.getLayout());
    }

    @Test
    public void testStartAndStop() throws Exception {
        setUp("name");

        appender.start();
        then(manager).should().startupInternal();

        appender.stop();
        then(manager).should().stop(0L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testReplaceManager() throws Exception {
        setUp("name");

        final LocalAbstractDatabaseManager oldManager = appender.getManager();
        assertSame("The manager should be the same.", manager, oldManager);

        final LocalAbstractDatabaseManager newManager = mock(LocalAbstractDatabaseManager.class);
        appender.replaceManager(newManager);
        then(manager).should().close();
        then(newManager).should().startupInternal();

        appender.stop();
        then(newManager).should().stop(0L, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testAppend() {
        setUp("name");
        given(manager.commitAndClose()).willReturn(true);

        final LogEvent event1 = mock(LogEvent.class);
        final LogEvent event2 = mock(LogEvent.class);

        appender.append(event1);
        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event1));
        then(manager).should().commitAndClose();

        reset(manager);

        appender.append(event2);
        then(manager).should().connectAndStart();
        then(manager).should().writeInternal(same(event2));
        then(manager).should().commitAndClose();
    }

    private static abstract class LocalAbstractDatabaseManager extends AbstractDatabaseManager {
        public LocalAbstractDatabaseManager(final String name, final int bufferSize) {
            super(name, bufferSize);
        }
    }

    private static class LocalAbstractDatabaseAppender extends AbstractDatabaseAppender<LocalAbstractDatabaseManager> {

        public LocalAbstractDatabaseAppender(final String name, final Filter filter, final boolean exceptionSuppressed,
                                             final LocalAbstractDatabaseManager manager) {
            super(name, filter, exceptionSuppressed, manager);
        }
    }
}
