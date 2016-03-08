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
package org.apache.logging.log4j.nosql.appender;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.message.Message;
import org.easymock.Capture;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;

import static org.junit.Assert.*;

public class NoSqlDatabaseManagerTest {
    private NoSqlConnection<Map<String, Object>, DefaultNoSqlObject> connection;
    private NoSqlProvider<NoSqlConnection<Map<String, Object>, DefaultNoSqlObject>> provider;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        this.provider = createStrictMock(NoSqlProvider.class);
        this.connection = createStrictMock(NoSqlConnection.class);
    }

    @After
    public void tearDown() {
        verify(this.provider, this.connection);
    }

    @Test
    public void testConnection() {
        replay(this.provider, this.connection);

        final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, this.provider);

        assertNotNull("The manager should not be null.", manager);

        try {
            verify(this.provider, this.connection);
            reset(this.provider, this.connection);
            expect(this.provider.getConnection()).andReturn(this.connection);
            replay(this.provider, this.connection);

            manager.connectAndStart();
            manager.commitAndClose();
        } finally {
            try {
                manager.release();
            } catch (final Throwable ignore) {
                /* */
            }
        }
    }

    @Test
    public void testWriteInternalNotConnected01() {
        replay(this.provider, this.connection);

        final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, this.provider);

        try {
            verify(this.provider, this.connection);
            reset(this.provider, this.connection);

            final LogEvent event = createStrictMock(LogEvent.class);
            replay(this.provider, this.connection, event);

            try {
                manager.writeInternal(event);
                fail("Expected AppenderLoggingException.");
            } catch (final AppenderLoggingException ignore) {
                /* */
            }

            verify(event);
        } finally {
            try {
                manager.release();
            } catch (final Throwable ignore) {
                /* */
            }
        }
    }

    @Test
    public void testWriteInternalNotConnected02() {
        final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, this.provider);

        try {
            replay(this.provider, this.connection);

            manager.startup();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);
            expect(this.provider.getConnection()).andReturn(this.connection);
            replay(this.provider, this.connection);

            manager.connectAndStart();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);

            final LogEvent event = createStrictMock(LogEvent.class);
            expect(this.connection.isClosed()).andReturn(true);
            replay(this.provider, this.connection, event);

            try {
                manager.writeInternal(event);
                fail("Expected AppenderLoggingException.");
            } catch (final AppenderLoggingException ignore) {
                /* */
            }
        } finally {
            try {
                manager.release();
            } catch (final Throwable ignore) {
                /* */
            }
        }
    }

    @Test
    public void testWriteInternal01() {
        final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, this.provider);

        try {
            replay(this.provider, this.connection);

            manager.startup();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);
            expect(this.provider.getConnection()).andReturn(this.connection);
            replay(this.provider, this.connection);

            manager.connectAndStart();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);

            final Capture<NoSqlObject<Map<String, Object>>> capture = new Capture<>();

            final LogEvent event = createStrictMock(LogEvent.class);
            final Message message = createStrictMock(Message.class);

            expect(this.connection.isClosed()).andReturn(false);
            expect(this.connection.createObject()).andAnswer(new IAnswer<DefaultNoSqlObject>() {
                @Override
                public DefaultNoSqlObject answer() throws Throwable {
                    return new DefaultNoSqlObject();
                }
            }).atLeastOnce();
            expect(event.getLevel()).andReturn(Level.WARN);
            expect(event.getLoggerName()).andReturn("com.foo.NoSQLDbTest.testWriteInternal01");
            expect(event.getMessage()).andReturn(message).times(2);
            expect(message.getFormattedMessage()).andReturn("My formatted message 01.");
            expect(event.getSource()).andReturn(new StackTraceElement("com.foo.Bar", "testMethod01", "Bar.java", 15));
            expect(event.getMarker()).andReturn(null);
            expect(event.getThreadId()).andReturn(1L);
            expect(event.getThreadName()).andReturn("MyThread-A");
            expect(event.getThreadPriority()).andReturn(1);
            expect(event.getTimeMillis()).andReturn(1234567890123L).times(2);
            expect(event.getThrown()).andReturn(null);
            expect(event.getContextMap()).andReturn(null);
            expect(event.getContextStack()).andReturn(null);
            this.connection.insertObject(capture(capture));
            expectLastCall();
            replay(this.provider, this.connection, event, message);

            manager.writeInternal(event);

            final NoSqlObject<Map<String, Object>> inserted = capture.getValue();
            assertNotNull("The inserted value should not be null.", inserted);
            final Map<String, Object> object = inserted.unwrap();
            assertNotNull("The unwrapped object should not be null.", object);

            assertEquals("The level is not correct.", Level.WARN, object.get("level"));
            assertEquals("The logger is not correct.", "com.foo.NoSQLDbTest.testWriteInternal01",
                    object.get("loggerName"));
            assertEquals("The message is not correct.", "My formatted message 01.", object.get("message"));
            assertEquals("The thread is not correct.", "MyThread-A", object.get("threadName"));
            assertEquals("The millis is not correct.", 1234567890123L, object.get("millis"));
            assertEquals("The date is not correct.", 1234567890123L, ((Date) object.get("date")).getTime());

            assertTrue("The source should be a map.", object.get("source") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertEquals("The class is not correct.", "com.foo.Bar", source.get("className"));
            assertEquals("The method is not correct.", "testMethod01", source.get("methodName"));
            assertEquals("The file name is not correct.", "Bar.java", source.get("fileName"));
            assertEquals("The line number is not correct.", 15, source.get("lineNumber"));

            assertNull("The marker should be null.", object.get("marker"));

            assertNull("The thrown should be null.", object.get("thrown"));

            assertNull("The context map should be null.", object.get("contextMap"));

            assertNull("The context stack should be null.", object.get("contextStack"));

            verify(this.provider, this.connection, event, message);
        } finally {
            try {
                manager.release();
            } catch (final Throwable ignore) {
                /* */
            }
        }
    }

    @Test
    public void testWriteInternal02() {
        final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, this.provider);

        try {
            replay(this.provider, this.connection);

            manager.startup();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);
            expect(this.provider.getConnection()).andReturn(this.connection);
            replay(this.provider, this.connection);

            manager.connectAndStart();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);

            final Capture<NoSqlObject<Map<String, Object>>> capture = new Capture<>();

            final RuntimeException exception = new RuntimeException("This is something cool!");
            final Map<String, String> context = new HashMap<>();
            context.put("hello", "world");
            context.put("user", "pass");

            final LogEvent event = createStrictMock(LogEvent.class);
            final Message message = createStrictMock(Message.class);
            ThreadContext.push("message1");
            ThreadContext.push("stack2");
            final ThreadContext.ContextStack stack = ThreadContext.getImmutableStack();
            ThreadContext.clearStack();

            expect(this.connection.isClosed()).andReturn(false);
            expect(this.connection.createObject()).andAnswer(new IAnswer<DefaultNoSqlObject>() {
                @Override
                public DefaultNoSqlObject answer() throws Throwable {
                    return new DefaultNoSqlObject();
                }
            }).atLeastOnce();
            expect(this.connection.createList(anyInt())).andAnswer(new IAnswer<DefaultNoSqlObject[]>() {
                @Override
                public DefaultNoSqlObject[] answer() throws Throwable {
                    return new DefaultNoSqlObject[(Integer) getCurrentArguments()[0]];
                }
            });
            expect(this.connection.createObject()).andAnswer(new IAnswer<DefaultNoSqlObject>() {
                @Override
                public DefaultNoSqlObject answer() throws Throwable {
                    return new DefaultNoSqlObject();
                }
            }).atLeastOnce();
            expect(event.getLevel()).andReturn(Level.DEBUG);
            expect(event.getLoggerName()).andReturn("com.foo.NoSQLDbTest.testWriteInternal02");
            expect(event.getMessage()).andReturn(message).times(2);
            expect(message.getFormattedMessage()).andReturn("Another cool message 02.");
            expect(event.getSource()).andReturn(new StackTraceElement("com.bar.Foo", "anotherMethod03", "Foo.java", 9));
            expect(event.getMarker()).andReturn(MarkerManager.getMarker("LoneMarker"));
            expect(event.getThreadId()).andReturn(1L);
            expect(event.getThreadName()).andReturn("AnotherThread-B");
            expect(event.getThreadPriority()).andReturn(1);
            expect(event.getTimeMillis()).andReturn(987654321564L).times(2);
            expect(event.getThrown()).andReturn(exception);
            expect(event.getContextMap()).andReturn(context);
            expect(event.getContextStack()).andReturn(stack);
            this.connection.insertObject(capture(capture));
            expectLastCall();
            replay(this.provider, this.connection, event, message);

            manager.writeInternal(event);

            final NoSqlObject<Map<String, Object>> inserted = capture.getValue();
            assertNotNull("The inserted value should not be null.", inserted);
            final Map<String, Object> object = inserted.unwrap();
            assertNotNull("The unwrapped object should not be null.", object);

            assertEquals("The level is not correct.", Level.DEBUG, object.get("level"));
            assertEquals("The logger is not correct.", "com.foo.NoSQLDbTest.testWriteInternal02",
                    object.get("loggerName"));
            assertEquals("The message is not correct.", "Another cool message 02.", object.get("message"));
            assertEquals("The thread is not correct.", "AnotherThread-B", object.get("threadName"));
            assertEquals("The millis is not correct.", 987654321564L, object.get("millis"));
            assertEquals("The date is not correct.", 987654321564L, ((Date) object.get("date")).getTime());

            assertTrue("The source should be a map.", object.get("source") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertEquals("The class is not correct.", "com.bar.Foo", source.get("className"));
            assertEquals("The method is not correct.", "anotherMethod03", source.get("methodName"));
            assertEquals("The file name is not correct.", "Foo.java", source.get("fileName"));
            assertEquals("The line number is not correct.", 9, source.get("lineNumber"));

            assertTrue("The marker should be a map.", object.get("marker") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> marker = (Map<String, Object>) object.get("marker");
            assertEquals("The marker name is not correct.", "LoneMarker", marker.get("name"));
            assertNull("The marker parent should be null.", marker.get("parent"));

            assertTrue("The thrown should be a map.", object.get("thrown") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> thrown = (Map<String, Object>) object.get("thrown");
            assertEquals("The thrown type is not correct.", "java.lang.RuntimeException", thrown.get("type"));
            assertEquals("The thrown message is not correct.", "This is something cool!", thrown.get("message"));
            assertTrue("The thrown stack trace should be a list.", thrown.get("stackTrace") instanceof List);
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) thrown.get("stackTrace");
            assertEquals("The thrown stack trace length is not correct.", exception.getStackTrace().length,
                    stackTrace.size());
            for (int i = 0; i < exception.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception.getStackTrace()[i];
                final Map<String, Object> e2 = stackTrace.get(i);

                assertEquals("Element class name [" + i + "] is not correct.", e1.getClassName(), e2.get("className"));
                assertEquals("Element method name [" + i + "] is not correct.", e1.getMethodName(),
                        e2.get("methodName"));
                assertEquals("Element file name [" + i + "] is not correct.", e1.getFileName(), e2.get("fileName"));
                assertEquals("Element line number [" + i + "] is not correct.", e1.getLineNumber(),
                        e2.get("lineNumber"));
            }
            assertNull("The thrown should have no cause.", thrown.get("cause"));

            assertTrue("The context map should be a map.", object.get("contextMap") instanceof Map);
            assertEquals("The context map is not correct.", context, object.get("contextMap"));

            assertTrue("The context stack should be list.", object.get("contextStack") instanceof List);
            assertEquals("The context stack is not correct.", stack.asList(), object.get("contextStack"));

            verify(this.provider, this.connection, event, message);
        } finally {
            try {
                manager.release();
            } catch (final Throwable ignore) {
                /* */
            }
        }
    }

    @Test
    public void testWriteInternal03() {
        final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, this.provider);

        try {
            replay(this.provider, this.connection);

            manager.startup();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);
            expect(this.provider.getConnection()).andReturn(this.connection);
            replay(this.provider, this.connection);

            manager.connectAndStart();

            verify(this.provider, this.connection);
            reset(this.provider, this.connection);

            final Capture<NoSqlObject<Map<String, Object>>> capture = new Capture<>();

            final IOException exception1 = new IOException("This is the cause.");
            final SQLException exception2 = new SQLException("This is the result.", exception1);
            final Map<String, String> context = new HashMap<>();
            context.put("hello", "world");
            context.put("user", "pass");

            final LogEvent event = createStrictMock(LogEvent.class);
            final Message message = createStrictMock(Message.class);
            ThreadContext.push("message1");
            ThreadContext.push("stack2");
            final ThreadContext.ContextStack stack = ThreadContext.getImmutableStack();
            ThreadContext.clearStack();

            expect(this.connection.isClosed()).andReturn(false);
            expect(this.connection.createObject()).andAnswer(new IAnswer<DefaultNoSqlObject>() {
                @Override
                public DefaultNoSqlObject answer() throws Throwable {
                    return new DefaultNoSqlObject();
                }
            }).atLeastOnce();
            expect(this.connection.createList(anyInt())).andAnswer(new IAnswer<DefaultNoSqlObject[]>() {
                @Override
                public DefaultNoSqlObject[] answer() throws Throwable {
                    return new DefaultNoSqlObject[(Integer) getCurrentArguments()[0]];
                }
            });
            expect(this.connection.createObject()).andAnswer(new IAnswer<DefaultNoSqlObject>() {
                @Override
                public DefaultNoSqlObject answer() throws Throwable {
                    return new DefaultNoSqlObject();
                }
            }).atLeastOnce();
            expect(this.connection.createList(anyInt())).andAnswer(new IAnswer<DefaultNoSqlObject[]>() {
                @Override
                public DefaultNoSqlObject[] answer() throws Throwable {
                    return new DefaultNoSqlObject[(Integer) getCurrentArguments()[0]];
                }
            });
            expect(this.connection.createObject()).andAnswer(new IAnswer<DefaultNoSqlObject>() {
                @Override
                public DefaultNoSqlObject answer() throws Throwable {
                    return new DefaultNoSqlObject();
                }
            }).atLeastOnce();
            expect(event.getLevel()).andReturn(Level.DEBUG);
            expect(event.getLoggerName()).andReturn("com.foo.NoSQLDbTest.testWriteInternal02");
            expect(event.getMessage()).andReturn(message).times(2);
            expect(message.getFormattedMessage()).andReturn("Another cool message 02.");
            expect(event.getSource()).andReturn(new StackTraceElement("com.bar.Foo", "anotherMethod03", "Foo.java", 9));
            expect(event.getMarker()).andReturn(
                    MarkerManager.getMarker("AnotherMarker").addParents(MarkerManager.getMarker("Parent1").addParents(MarkerManager.getMarker("GrandParent1")),
                            MarkerManager.getMarker("Parent2")));
            expect(event.getThreadId()).andReturn(1L);
            expect(event.getThreadName()).andReturn("AnotherThread-B");
            expect(event.getThreadPriority()).andReturn(1);
            expect(event.getTimeMillis()).andReturn(987654321564L).times(2);
            expect(event.getThrown()).andReturn(exception2);
            expect(event.getContextMap()).andReturn(context);
            expect(event.getContextStack()).andReturn(stack);
            this.connection.insertObject(capture(capture));
            expectLastCall();
            replay(this.provider, this.connection, event, message);

            manager.writeInternal(event);

            final NoSqlObject<Map<String, Object>> inserted = capture.getValue();
            assertNotNull("The inserted value should not be null.", inserted);
            final Map<String, Object> object = inserted.unwrap();
            assertNotNull("The unwrapped object should not be null.", object);

            assertEquals("The level is not correct.", Level.DEBUG, object.get("level"));
            assertEquals("The logger is not correct.", "com.foo.NoSQLDbTest.testWriteInternal02",
                    object.get("loggerName"));
            assertEquals("The message is not correct.", "Another cool message 02.", object.get("message"));
            assertEquals("The thread is not correct.", "AnotherThread-B", object.get("threadName"));
            assertEquals("The millis is not correct.", 987654321564L, object.get("millis"));
            assertEquals("The date is not correct.", 987654321564L, ((Date) object.get("date")).getTime());

            assertTrue("The source should be a map.", object.get("source") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertEquals("The class is not correct.", "com.bar.Foo", source.get("className"));
            assertEquals("The method is not correct.", "anotherMethod03", source.get("methodName"));
            assertEquals("The file name is not correct.", "Foo.java", source.get("fileName"));
            assertEquals("The line number is not correct.", 9, source.get("lineNumber"));

            assertTrue("The marker should be a map.", object.get("marker") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> marker = (Map<String, Object>) object.get("marker");
            assertEquals("The marker name is not correct.", "AnotherMarker", marker.get("name"));

            assertTrue("The marker parents should be a list.", marker.get("parents") instanceof List);
            @SuppressWarnings("unchecked")
            final List<Object> markerParents = (List<Object>) marker.get("parents");
            assertEquals("The marker parents should contain two parents", 2, markerParents.size());
            
            assertTrue("The marker parents[0] should be a map.", markerParents.get(0) instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent1 = (Map<String, Object>) markerParents.get(0);
            assertEquals("The first marker parent name is not correct.", "Parent1", parent1.get("name"));
            
            assertTrue("The marker parents[1] should be a map.", markerParents.get(1) instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent2 = (Map<String, Object>) markerParents.get(1);
            assertEquals("The second marker parent name is not correct.", "Parent2", parent2.get("name"));
            assertNull("The second marker should have no parent.", parent2.get("parent"));

            assertTrue("The parent1 parents should be a list.", parent1.get("parents") instanceof List);
            @SuppressWarnings("unchecked")
            final List<Object> parent1Parents = (List<Object>) parent1.get("parents");
            assertEquals("The parent1 parents should have only one parent", 1, parent1Parents.size());
            
            assertTrue("The parent1Parents[0] should be a map.", parent1Parents.get(0) instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent1parent = (Map<String, Object>) parent1Parents.get(0);
            assertEquals("The first parent1 parent name is not correct.", "GrandParent1", parent1parent.get("name"));
            assertNull("The parent1parent marker should have no parent.", parent1parent.get("parent"));

            assertTrue("The thrown should be a map.", object.get("thrown") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> thrown = (Map<String, Object>) object.get("thrown");
            assertEquals("The thrown type is not correct.", "java.sql.SQLException", thrown.get("type"));
            assertEquals("The thrown message is not correct.", "This is the result.", thrown.get("message"));
            assertTrue("The thrown stack trace should be a list.", thrown.get("stackTrace") instanceof List);
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) thrown.get("stackTrace");
            assertEquals("The thrown stack trace length is not correct.", exception2.getStackTrace().length,
                    stackTrace.size());
            for (int i = 0; i < exception2.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception2.getStackTrace()[i];
                final Map<String, Object> e2 = stackTrace.get(i);

                assertEquals("Element class name [" + i + "] is not correct.", e1.getClassName(), e2.get("className"));
                assertEquals("Element method name [" + i + "] is not correct.", e1.getMethodName(),
                        e2.get("methodName"));
                assertEquals("Element file name [" + i + "] is not correct.", e1.getFileName(), e2.get("fileName"));
                assertEquals("Element line number [" + i + "] is not correct.", e1.getLineNumber(),
                        e2.get("lineNumber"));
            }
            assertTrue("The thrown cause should be a map.", thrown.get("cause") instanceof Map);
            @SuppressWarnings("unchecked")
            final Map<String, Object> cause = (Map<String, Object>) thrown.get("cause");
            assertEquals("The cause type is not correct.", "java.io.IOException", cause.get("type"));
            assertEquals("The cause message is not correct.", "This is the cause.", cause.get("message"));
            assertTrue("The cause stack trace should be a list.", cause.get("stackTrace") instanceof List);
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> causeStackTrace = (List<Map<String, Object>>) cause.get("stackTrace");
            assertEquals("The cause stack trace length is not correct.", exception1.getStackTrace().length,
                    causeStackTrace.size());
            for (int i = 0; i < exception1.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception1.getStackTrace()[i];
                final Map<String, Object> e2 = causeStackTrace.get(i);

                assertEquals("Element class name [" + i + "] is not correct.", e1.getClassName(), e2.get("className"));
                assertEquals("Element method name [" + i + "] is not correct.", e1.getMethodName(),
                        e2.get("methodName"));
                assertEquals("Element file name [" + i + "] is not correct.", e1.getFileName(), e2.get("fileName"));
                assertEquals("Element line number [" + i + "] is not correct.", e1.getLineNumber(),
                        e2.get("lineNumber"));
            }
            assertNull("The cause should have no cause.", cause.get("cause"));

            assertTrue("The context map should be a map.", object.get("contextMap") instanceof Map);
            assertEquals("The context map is not correct.", context, object.get("contextMap"));

            assertTrue("The context stack should be list.", object.get("contextStack") instanceof List);
            assertEquals("The context stack is not correct.", stack.asList(), object.get("contextStack"));

            verify(this.provider, this.connection, event, message);
        } finally {
            try {
                manager.release();
            } catch (final Throwable ignore) {
                /* */
            }
        }
    }
}
