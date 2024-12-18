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
package org.apache.logging.log4j.core.appender.nosql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.test.junit.UsingAnyThreadContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@UsingAnyThreadContext
public class NoSqlDatabaseManagerTest {

    @Mock
    private NoSqlConnection<Map<String, Object>, DefaultNoSqlObject> connection;

    @Mock
    private NoSqlProvider<NoSqlConnection<Map<String, Object>, DefaultNoSqlObject>> provider;

    @Mock
    private Message message;

    @Captor
    private ArgumentCaptor<NoSqlObject<Map<String, Object>>> captor;

    @BeforeEach
    public void setUp() {
        given(provider.getConnection()).willReturn(connection);
        given(connection.createObject()).willAnswer(invocation -> new DefaultNoSqlObject());
        given(connection.createList(anyInt()))
                .willAnswer(invocation -> new DefaultNoSqlObject[invocation.<Integer>getArgument(0)]);
    }

    @Test
    public void testConnection() {
        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, provider, null, null)) {

            assertNotNull(manager, "The manager should not be null.");

            manager.connectAndStart();
            then(provider).should().getConnection();
            manager.commitAndClose();
        }
    }

    @Test
    public void testWriteInternalNotConnected01() {
        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, provider, null, null)) {
            assertThrows(AppenderLoggingException.class, () -> manager.writeInternal(mock(LogEvent.class), null));
        }
    }

    @Test
    public void testWriteInternalNotConnected02() {
        given(connection.isClosed()).willReturn(true);

        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, provider, null, null)) {

            manager.startup();
            manager.connectAndStart();
            then(provider).should().getConnection();

            assertThrows(AppenderLoggingException.class, () -> manager.writeInternal(mock(LogEvent.class), null));
        }
    }

    @Test
    public void testWriteInternal01() {
        given(connection.isClosed()).willReturn(false);
        given(message.getFormattedMessage()).willReturn("My formatted message 01.");

        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, provider, null, null)) {

            manager.startup();
            manager.connectAndStart();
            then(provider).should().getConnection();

            final LogEvent event = Log4jLogEvent.newBuilder()
                    .setLevel(Level.WARN)
                    .setLoggerName("com.foo.NoSQLDbTest.testWriteInternal01")
                    .setMessage(message)
                    .setSource(new StackTraceElement("com.foo.Bar", "testMethod01", "Bar.java", 15))
                    .setThreadId(1L)
                    .setThreadName("MyThread-A")
                    .setThreadPriority(1)
                    .setTimeMillis(1234567890123L)
                    .build();

            manager.writeInternal(event, null);
            then(connection).should().insertObject(captor.capture());

            final NoSqlObject<Map<String, Object>> inserted = captor.getValue();
            assertNotNull(inserted, "The inserted value should not be null.");
            final Map<String, Object> object = inserted.unwrap();
            assertNotNull(object, "The unwrapped object should not be null.");

            assertEquals(Level.WARN, object.get("level"), "The level is not correct.");
            assertEquals(
                    "com.foo.NoSQLDbTest.testWriteInternal01", object.get("loggerName"), "The logger is not correct.");
            assertEquals("My formatted message 01.", object.get("message"), "The message is not correct.");
            assertEquals("MyThread-A", object.get("threadName"), "The thread is not correct.");
            assertEquals(1234567890123L, object.get("millis"), "The millis is not correct.");
            assertEquals(1234567890123L, ((Date) object.get("date")).getTime(), "The date is not correct.");

            assertTrue(object.get("source") instanceof Map, "The source should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertEquals("com.foo.Bar", source.get("className"), "The class is not correct.");
            assertEquals("testMethod01", source.get("methodName"), "The method is not correct.");
            assertEquals("Bar.java", source.get("fileName"), "The file name is not correct.");
            assertEquals(15, source.get("lineNumber"), "The line number is not correct.");

            assertNull(object.get("marker"), "The marker should be null.");

            assertNull(object.get("thrown"), "The thrown should be null.");

            assertTrue(((Map) object.get("contextMap")).isEmpty(), "The context map should be empty.");

            assertTrue(((Collection) object.get("contextStack")).isEmpty(), "The context stack should be null.");
        }
    }

    @Test
    public void testWriteInternal02() {
        given(connection.isClosed()).willReturn(false);
        given(message.getFormattedMessage()).willReturn("Another cool message 02.");

        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, provider, null, null)) {
            manager.startup();

            manager.connectAndStart();
            then(provider).should().getConnection();

            final RuntimeException exception = new RuntimeException("This is something cool!");
            final Map<String, String> context = new HashMap<>();
            context.put("hello", "world");
            context.put("user", "pass");

            ThreadContext.push("message1");
            ThreadContext.push("stack2");
            final ThreadContext.ContextStack stack = ThreadContext.getImmutableStack();
            ThreadContext.clearStack();

            final LogEvent event = Log4jLogEvent.newBuilder()
                    .setLevel(Level.DEBUG)
                    .setLoggerName("com.foo.NoSQLDbTest.testWriteInternal02")
                    .setMessage(message)
                    .setSource(new StackTraceElement("com.bar.Foo", "anotherMethod03", "Foo.java", 9))
                    .setMarker(MarkerManager.getMarker("LoneMarker"))
                    .setThreadId(1L)
                    .setThreadName("AnotherThread-B")
                    .setThreadPriority(1)
                    .setTimeMillis(987654321564L)
                    .setThrown(exception)
                    .setContextData(ContextDataFactory.createContextData(context))
                    .setContextStack(stack)
                    .build();

            manager.writeInternal(event, null);
            then(connection).should().insertObject(captor.capture());

            final NoSqlObject<Map<String, Object>> inserted = captor.getValue();
            assertNotNull(inserted, "The inserted value should not be null.");
            final Map<String, Object> object = inserted.unwrap();
            assertNotNull(object, "The unwrapped object should not be null.");

            assertEquals(Level.DEBUG, object.get("level"), "The level is not correct.");
            assertEquals(
                    "com.foo.NoSQLDbTest.testWriteInternal02", object.get("loggerName"), "The logger is not correct.");
            assertEquals("Another cool message 02.", object.get("message"), "The message is not correct.");
            assertEquals("AnotherThread-B", object.get("threadName"), "The thread is not correct.");
            assertEquals(987654321564L, object.get("millis"), "The millis is not correct.");
            assertEquals(987654321564L, ((Date) object.get("date")).getTime(), "The date is not correct.");

            assertTrue(object.get("source") instanceof Map, "The source should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertEquals("com.bar.Foo", source.get("className"), "The class is not correct.");
            assertEquals("anotherMethod03", source.get("methodName"), "The method is not correct.");
            assertEquals("Foo.java", source.get("fileName"), "The file name is not correct.");
            assertEquals(9, source.get("lineNumber"), "The line number is not correct.");

            assertTrue(object.get("marker") instanceof Map, "The marker should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> marker = (Map<String, Object>) object.get("marker");
            assertEquals("LoneMarker", marker.get("name"), "The marker name is not correct.");
            assertNull(marker.get("parent"), "The marker parent should be null.");

            assertTrue(object.get("thrown") instanceof Map, "The thrown should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> thrown = (Map<String, Object>) object.get("thrown");
            assertEquals("java.lang.RuntimeException", thrown.get("type"), "The thrown type is not correct.");
            assertEquals("This is something cool!", thrown.get("message"), "The thrown message is not correct.");
            assertTrue(thrown.get("stackTrace") instanceof List, "The thrown stack trace should be a list.");
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) thrown.get("stackTrace");
            assertEquals(
                    exception.getStackTrace().length,
                    stackTrace.size(),
                    "The thrown stack trace length is not correct.");
            for (int i = 0; i < exception.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception.getStackTrace()[i];
                final Map<String, Object> e2 = stackTrace.get(i);

                assertEquals(e1.getClassName(), e2.get("className"), "Element class name [" + i + "] is not correct.");
                assertEquals(
                        e1.getMethodName(), e2.get("methodName"), "Element method name [" + i + "] is not correct.");
                assertEquals(e1.getFileName(), e2.get("fileName"), "Element file name [" + i + "] is not correct.");
                assertEquals(
                        e1.getLineNumber(), e2.get("lineNumber"), "Element line number [" + i + "] is not correct.");
            }
            assertNull(thrown.get("cause"), "The thrown should have no cause.");

            assertTrue(object.get("contextMap") instanceof Map, "The context map should be a map.");
            assertEquals(context, object.get("contextMap"), "The context map is not correct.");

            assertTrue(object.get("contextStack") instanceof List, "The context stack should be list.");
            assertEquals(stack.asList(), object.get("contextStack"), "The context stack is not correct.");
        }
    }

    @Test
    public void testWriteInternal03() {
        given(connection.isClosed()).willReturn(false);
        given(message.getFormattedMessage()).willReturn("Another cool message 02.");

        try (final NoSqlDatabaseManager<?> manager =
                NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0, provider, null, null)) {
            manager.startup();

            manager.connectAndStart();
            then(provider).should().getConnection();

            final IOException exception1 = new IOException("This is the cause.");
            final SQLException exception2 = new SQLException("This is the result.", exception1);
            final Map<String, String> context = new HashMap<>();
            context.put("hello", "world");
            context.put("user", "pass");

            ThreadContext.push("message1");
            ThreadContext.push("stack2");
            final ThreadContext.ContextStack stack = ThreadContext.getImmutableStack();
            ThreadContext.clearStack();

            final LogEvent event = Log4jLogEvent.newBuilder()
                    .setLevel(Level.DEBUG)
                    .setLoggerName("com.foo.NoSQLDbTest.testWriteInternal02")
                    .setMessage(message)
                    .setSource(new StackTraceElement("com.bar.Foo", "anotherMethod03", "Foo.java", 9))
                    .setMarker(MarkerManager.getMarker("AnotherMarker")
                            .addParents(
                                    MarkerManager.getMarker("Parent1")
                                            .addParents(MarkerManager.getMarker("GrandParent1")),
                                    MarkerManager.getMarker("Parent2")))
                    .setThreadId(1L)
                    .setThreadName("AnotherThread-B")
                    .setThreadPriority(1)
                    .setTimeMillis(987654321564L)
                    .setThrown(exception2)
                    .setContextData(ContextDataFactory.createContextData(context))
                    .setContextStack(stack)
                    .build();

            manager.writeInternal(event, null);
            then(connection).should().insertObject(captor.capture());

            final NoSqlObject<Map<String, Object>> inserted = captor.getValue();
            assertNotNull(inserted, "The inserted value should not be null.");
            final Map<String, Object> object = inserted.unwrap();
            assertNotNull(object, "The unwrapped object should not be null.");

            assertEquals(Level.DEBUG, object.get("level"), "The level is not correct.");
            assertEquals(
                    "com.foo.NoSQLDbTest.testWriteInternal02", object.get("loggerName"), "The logger is not correct.");
            assertEquals("Another cool message 02.", object.get("message"), "The message is not correct.");
            assertEquals("AnotherThread-B", object.get("threadName"), "The thread is not correct.");
            assertEquals(987654321564L, object.get("millis"), "The millis is not correct.");
            assertEquals(987654321564L, ((Date) object.get("date")).getTime(), "The date is not correct.");

            assertTrue(object.get("source") instanceof Map, "The source should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertEquals("com.bar.Foo", source.get("className"), "The class is not correct.");
            assertEquals("anotherMethod03", source.get("methodName"), "The method is not correct.");
            assertEquals("Foo.java", source.get("fileName"), "The file name is not correct.");
            assertEquals(9, source.get("lineNumber"), "The line number is not correct.");

            assertTrue(object.get("marker") instanceof Map, "The marker should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> marker = (Map<String, Object>) object.get("marker");
            assertEquals("AnotherMarker", marker.get("name"), "The marker name is not correct.");

            assertTrue(marker.get("parents") instanceof List, "The marker parents should be a list.");
            @SuppressWarnings("unchecked")
            final List<Object> markerParents = (List<Object>) marker.get("parents");
            assertEquals(2, markerParents.size(), "The marker parents should contain two parents");

            assertTrue(markerParents.get(0) instanceof Map, "The marker parents[0] should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent1 = (Map<String, Object>) markerParents.get(0);
            assertEquals("Parent1", parent1.get("name"), "The first marker parent name is not correct.");

            assertTrue(markerParents.get(1) instanceof Map, "The marker parents[1] should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent2 = (Map<String, Object>) markerParents.get(1);
            assertEquals("Parent2", parent2.get("name"), "The second marker parent name is not correct.");
            assertNull(parent2.get("parent"), "The second marker should have no parent.");

            assertTrue(parent1.get("parents") instanceof List, "The parent1 parents should be a list.");
            @SuppressWarnings("unchecked")
            final List<Object> parent1Parents = (List<Object>) parent1.get("parents");
            assertEquals(1, parent1Parents.size(), "The parent1 parents should have only one parent");

            assertTrue(parent1Parents.get(0) instanceof Map, "The parent1Parents[0] should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent1parent = (Map<String, Object>) parent1Parents.get(0);
            assertEquals("GrandParent1", parent1parent.get("name"), "The first parent1 parent name is not correct.");
            assertNull(parent1parent.get("parent"), "The parent1parent marker should have no parent.");

            assertTrue(object.get("thrown") instanceof Map, "The thrown should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> thrown = (Map<String, Object>) object.get("thrown");
            assertEquals("java.sql.SQLException", thrown.get("type"), "The thrown type is not correct.");
            assertEquals("This is the result.", thrown.get("message"), "The thrown message is not correct.");
            assertTrue(thrown.get("stackTrace") instanceof List, "The thrown stack trace should be a list.");
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) thrown.get("stackTrace");
            assertEquals(
                    exception2.getStackTrace().length,
                    stackTrace.size(),
                    "The thrown stack trace length is not correct.");
            for (int i = 0; i < exception2.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception2.getStackTrace()[i];
                final Map<String, Object> e2 = stackTrace.get(i);

                assertEquals(e1.getClassName(), e2.get("className"), "Element class name [" + i + "] is not correct.");
                assertEquals(
                        e1.getMethodName(), e2.get("methodName"), "Element method name [" + i + "] is not correct.");
                assertEquals(e1.getFileName(), e2.get("fileName"), "Element file name [" + i + "] is not correct.");
                assertEquals(
                        e1.getLineNumber(), e2.get("lineNumber"), "Element line number [" + i + "] is not correct.");
            }
            assertTrue(thrown.get("cause") instanceof Map, "The thrown cause should be a map.");
            @SuppressWarnings("unchecked")
            final Map<String, Object> cause = (Map<String, Object>) thrown.get("cause");
            assertEquals("java.io.IOException", cause.get("type"), "The cause type is not correct.");
            assertEquals("This is the cause.", cause.get("message"), "The cause message is not correct.");
            assertTrue(cause.get("stackTrace") instanceof List, "The cause stack trace should be a list.");
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> causeStackTrace = (List<Map<String, Object>>) cause.get("stackTrace");
            assertEquals(
                    exception1.getStackTrace().length,
                    causeStackTrace.size(),
                    "The cause stack trace length is not correct.");
            for (int i = 0; i < exception1.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception1.getStackTrace()[i];
                final Map<String, Object> e2 = causeStackTrace.get(i);

                assertEquals(e1.getClassName(), e2.get("className"), "Element class name [" + i + "] is not correct.");
                assertEquals(
                        e1.getMethodName(), e2.get("methodName"), "Element method name [" + i + "] is not correct.");
                assertEquals(e1.getFileName(), e2.get("fileName"), "Element file name [" + i + "] is not correct.");
                assertEquals(
                        e1.getLineNumber(), e2.get("lineNumber"), "Element line number [" + i + "] is not correct.");
            }
            assertNull(cause.get("cause"), "The cause should have no cause.");

            assertTrue(object.get("contextMap") instanceof Map, "The context map should be a map.");
            assertEquals(context, object.get("contextMap"), "The context map is not correct.");

            assertTrue(object.get("contextStack") instanceof List, "The context stack should be list.");
            assertEquals(stack.asList(), object.get("contextStack"), "The context stack is not correct.");
        }
    }
}
