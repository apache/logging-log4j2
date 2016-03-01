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
import org.junit.After;
import org.junit.Test;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

public class AbstractDatabaseAppenderTest {
    private LocalAbstractDatabaseAppender appender;
    private LocalAbstractDatabaseManager manager;

    public void setUp(final String name) {
        this.manager = createMockBuilder(LocalAbstractDatabaseManager.class)
                .withConstructor(String.class, int.class)
                .withArgs(name, 0)
                .addMockedMethod("release")
                .createStrictMock();

        this.appender = createMockBuilder(LocalAbstractDatabaseAppender.class)
                .withConstructor(String.class, Filter.class, boolean.class, LocalAbstractDatabaseManager.class)
                .withArgs(name, null, true, this.manager)
                .createStrictMock();
    }

    @After
    public void tearDown() {
        verify(this.manager, this.appender);
    }

    @Test
    public void testNameAndGetLayout01() {
        this.setUp("testName01");

        replay(this.manager, this.appender);

        assertEquals("The name is not correct.", "testName01", this.appender.getName());
        assertNull("The layout should always be null.", this.appender.getLayout());
    }

    @Test
    public void testNameAndGetLayout02() {
        this.setUp("anotherName02");

        replay(this.manager, this.appender);

        assertEquals("The name is not correct.", "anotherName02", this.appender.getName());
        assertNull("The layout should always be null.", this.appender.getLayout());
    }

    @Test
    public void testStartAndStop() throws Exception {
        this.setUp("name");

        this.manager.startupInternal();
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.start();

        verify(this.manager, this.appender);
        reset(this.manager, this.appender);
        this.manager.release();
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.stop();
    }

    @Test
    public void testReplaceManager() throws Exception {
        this.setUp("name");

        replay(this.manager, this.appender);

        final LocalAbstractDatabaseManager manager = this.appender.getManager();

        assertSame("The manager should be the same.", this.manager, manager);

        verify(this.manager, this.appender);
        reset(this.manager, this.appender);
        this.manager.release();
        expectLastCall();
        final LocalAbstractDatabaseManager newManager = createMockBuilder(LocalAbstractDatabaseManager.class)
                .withConstructor(String.class, int.class).withArgs("name", 0).addMockedMethod("release")
                .createStrictMock();
        newManager.startupInternal();
        expectLastCall();
        replay(this.manager, this.appender, newManager);

        this.appender.replaceManager(newManager);

        verify(this.manager, this.appender, newManager);
        reset(this.manager, this.appender, newManager);
        newManager.release();
        expectLastCall();
        replay(this.manager, this.appender, newManager);

        this.appender.stop();

        verify(newManager);
    }

    @Test
    public void testAppend() {
        this.setUp("name");

        final LogEvent event1 = createStrictMock(LogEvent.class);
        final LogEvent event2 = createStrictMock(LogEvent.class);

        this.manager.connectAndStart();
        expectLastCall();
        this.manager.writeInternal(same(event1));
        expectLastCall();
        this.manager.commitAndClose();
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.append(event1);

        verify(this.manager, this.appender);
        reset(this.manager, this.appender);
        this.manager.connectAndStart();
        expectLastCall();
        this.manager.writeInternal(same(event2));
        expectLastCall();
        this.manager.commitAndClose();
        expectLastCall();
        replay(this.manager, this.appender);

        this.appender.append(event2);
    }

    private static abstract class LocalAbstractDatabaseManager extends AbstractDatabaseManager {
        public LocalAbstractDatabaseManager(final String name, final int bufferSize) {
            super(name, bufferSize);
        }
    }

    private static abstract class LocalAbstractDatabaseAppender extends
            AbstractDatabaseAppender<LocalAbstractDatabaseManager> {

        private static final long serialVersionUID = 1L;

        public LocalAbstractDatabaseAppender(final String name, final Filter filter, final boolean exceptionSuppressed,
                                             final LocalAbstractDatabaseManager manager) {
            super(name, filter, exceptionSuppressed, manager);
        }
    }
}
