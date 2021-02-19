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
package org.apache.logging.log4j.core.appender.nosql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.io.IOException;
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
import org.apache.logging.log4j.junit.ThreadContextStackRule;
import org.apache.logging.log4j.message.Message;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class NoSqlDatabaseManagerTest {
    @Mock
    private NoSqlConnection<Map<String, Object>, DefaultNoSqlObject> connection;
    @Mock
    private NoSqlProvider<NoSqlConnection<Map<String, Object>, DefaultNoSqlObject>> provider;
    @Mock
    private Message message;
    @Captor
    private ArgumentCaptor<NoSqlObject<Map<String, Object>>> captor;

    @Rule
    public final ThreadContextStackRule threadContextRule = new ThreadContextStackRule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        given(provider.getConnection()).willReturn(connection);
        given(connection.createObject()).willAnswer(new Answer<DefaultNoSqlObject>() {
            @Override
            public DefaultNoSqlObject answer(final InvocationOnMock invocation) throws Throwable {
                return new DefaultNoSqlObject();
            }
        });
        given(connection.createList(anyInt())).willAnswer(new Answer<DefaultNoSqlObject[]>() {
            @Override
            public DefaultNoSqlObject[] answer(final InvocationOnMock invocation) throws Throwable {
                return new DefaultNoSqlObject[invocation.<Integer>getArgument(0)];
            }
        });
    }

    @Test
    public void testConnection() {
        try (final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0,
            provider)) {

            assertThat(manager).describedAs("The manager should not be null.").isNotNull();

            manager.connectAndStart();
            then(provider).should().getConnection();
            manager.commitAndClose();
        }
    }

    @Test
    public void testWriteInternalNotConnected01() {
        try (final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0,
            provider)) {
            expectedException.expect(AppenderLoggingException.class);
            manager.writeInternal(mock(LogEvent.class), null);
        }
    }

    @Test
    public void testWriteInternalNotConnected02() {
        given(connection.isClosed()).willReturn(true);

        try (final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0,
            provider)) {

            manager.startup();
            manager.connectAndStart();
            then(provider).should().getConnection();

            expectedException.expect(AppenderLoggingException.class);
            manager.writeInternal(mock(LogEvent.class), null);
        }
    }

    @Test
    public void testWriteInternal01() {
        given(connection.isClosed()).willReturn(false);
        given(message.getFormattedMessage()).willReturn("My formatted message 01.");

        try (final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0,
            provider)) {

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
            assertThat(inserted).describedAs("The inserted value should not be null.").isNotNull();
            final Map<String, Object> object = inserted.unwrap();
            assertThat(object).describedAs("The unwrapped object should not be null.").isNotNull();

            assertThat(object.get("level")).describedAs("The level is not correct.").isEqualTo(Level.WARN);
            assertThat(object.get("loggerName")).describedAs("The logger is not correct.").isEqualTo("com.foo.NoSQLDbTest.testWriteInternal01");
            assertThat(object.get("message")).describedAs("The message is not correct.").isEqualTo("My formatted message 01.");
            assertThat(object.get("threadName")).describedAs("The thread is not correct.").isEqualTo("MyThread-A");
            assertThat(object.get("millis")).describedAs("The millis is not correct.").isEqualTo(1234567890123L);
            assertThat(((Date) object.get("date")).getTime()).describedAs("The date is not correct.").isEqualTo(1234567890123L);

            assertThat(object.get("source") instanceof Map).describedAs("The source should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertThat(source.get("className")).describedAs("The class is not correct.").isEqualTo("com.foo.Bar");
            assertThat(source.get("methodName")).describedAs("The method is not correct.").isEqualTo("testMethod01");
            assertThat(source.get("fileName")).describedAs("The file name is not correct.").isEqualTo("Bar.java");
            assertThat(source.get("lineNumber")).describedAs("The line number is not correct.").isEqualTo(15);

            assertThat(object.get("marker")).describedAs("The marker should be null.").isNull();

            assertThat(object.get("thrown")).describedAs("The thrown should be null.").isNull();

            assertThat(((Map<?, ?>) object.get("contextMap")).isEmpty()).describedAs("The context map should be empty.").isTrue();

            assertThat(((Collection<?>) object.get("contextStack")).isEmpty()).describedAs("The context stack should be null.").isTrue();

        }
    }

    @Test
    public void testWriteInternal02() {
        given(connection.isClosed()).willReturn(false);
        given(message.getFormattedMessage()).willReturn("Another cool message 02.");

        try (final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0,
            provider)) {
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
            assertThat(inserted).describedAs("The inserted value should not be null.").isNotNull();
            final Map<String, Object> object = inserted.unwrap();
            assertThat(object).describedAs("The unwrapped object should not be null.").isNotNull();

            assertThat(object.get("level")).describedAs("The level is not correct.").isEqualTo(Level.DEBUG);
            assertThat(object.get("loggerName")).describedAs("The logger is not correct.").isEqualTo("com.foo.NoSQLDbTest.testWriteInternal02");
            assertThat(object.get("message")).describedAs("The message is not correct.").isEqualTo("Another cool message 02.");
            assertThat(object.get("threadName")).describedAs("The thread is not correct.").isEqualTo("AnotherThread-B");
            assertThat(object.get("millis")).describedAs("The millis is not correct.").isEqualTo(987654321564L);
            assertThat(((Date) object.get("date")).getTime()).describedAs("The date is not correct.").isEqualTo(987654321564L);

            assertThat(object.get("source") instanceof Map).describedAs("The source should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertThat(source.get("className")).describedAs("The class is not correct.").isEqualTo("com.bar.Foo");
            assertThat(source.get("methodName")).describedAs("The method is not correct.").isEqualTo("anotherMethod03");
            assertThat(source.get("fileName")).describedAs("The file name is not correct.").isEqualTo("Foo.java");
            assertThat(source.get("lineNumber")).describedAs("The line number is not correct.").isEqualTo(9);

            assertThat(object.get("marker") instanceof Map).describedAs("The marker should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> marker = (Map<String, Object>) object.get("marker");
            assertThat(marker.get("name")).describedAs("The marker name is not correct.").isEqualTo("LoneMarker");
            assertThat(marker.get("parent")).describedAs("The marker parent should be null.").isNull();

            assertThat(object.get("thrown") instanceof Map).describedAs("The thrown should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> thrown = (Map<String, Object>) object.get("thrown");
            assertThat(thrown.get("type")).describedAs("The thrown type is not correct.").isEqualTo("java.lang.RuntimeException");
            assertThat(thrown.get("message")).describedAs("The thrown message is not correct.").isEqualTo("This is something cool!");
            assertThat(thrown.get("stackTrace") instanceof List).describedAs("The thrown stack trace should be a list.").isTrue();
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) thrown.get("stackTrace");
            assertThat(stackTrace).describedAs("The thrown stack trace length is not correct.").hasSameSizeAs(exception.getStackTrace());
            for (int i = 0; i < exception.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception.getStackTrace()[i];
                final Map<String, Object> e2 = stackTrace.get(i);

                assertThat(e2.get("className")).describedAs("Element class name [" + i + "] is not correct.").isEqualTo(e1.getClassName());
                assertThat(e2.get("methodName")).describedAs("Element method name [" + i + "] is not correct.").isEqualTo(e1.getMethodName());
                assertThat(e2.get("fileName")).describedAs("Element file name [" + i + "] is not correct.").isEqualTo(e1.getFileName());
                assertThat(e2.get("lineNumber")).describedAs("Element line number [" + i + "] is not correct.").isEqualTo(e1.getLineNumber());
            }
            assertThat(thrown.get("cause")).describedAs("The thrown should have no cause.").isNull();

            assertThat(object.get("contextMap") instanceof Map).describedAs("The context map should be a map.").isTrue();
            assertThat(object.get("contextMap")).describedAs("The context map is not correct.").isEqualTo(context);

            assertThat(object.get("contextStack") instanceof List).describedAs("The context stack should be list.").isTrue();
            assertThat(object.get("contextStack")).describedAs("The context stack is not correct.").isEqualTo(stack.asList());
        }
    }

    @Test
    public void testWriteInternal03() {
        given(connection.isClosed()).willReturn(false);
        given(message.getFormattedMessage()).willReturn("Another cool message 02.");

        try (final NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager("name", 0,
            provider)) {
            manager.startup();

            manager.connectAndStart();
            then(provider).should().getConnection();

            final IOException exception1 = new IOException("This is the cause.");
            final IllegalStateException exception2 = new IllegalStateException("This is the result.", exception1);
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
                .setMarker(MarkerManager.getMarker("AnotherMarker").addParents(
                    MarkerManager.getMarker("Parent1").addParents(MarkerManager.getMarker("GrandParent1")),
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
            assertThat(inserted).describedAs("The inserted value should not be null.").isNotNull();
            final Map<String, Object> object = inserted.unwrap();
            assertThat(object).describedAs("The unwrapped object should not be null.").isNotNull();

            assertThat(object.get("level")).describedAs("The level is not correct.").isEqualTo(Level.DEBUG);
            assertThat(object.get("loggerName")).describedAs("The logger is not correct.").isEqualTo("com.foo.NoSQLDbTest.testWriteInternal02");
            assertThat(object.get("message")).describedAs("The message is not correct.").isEqualTo("Another cool message 02.");
            assertThat(object.get("threadName")).describedAs("The thread is not correct.").isEqualTo("AnotherThread-B");
            assertThat(object.get("millis")).describedAs("The millis is not correct.").isEqualTo(987654321564L);
            assertThat(((Date) object.get("date")).getTime()).describedAs("The date is not correct.").isEqualTo(987654321564L);

            assertThat(object.get("source") instanceof Map).describedAs("The source should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> source = (Map<String, Object>) object.get("source");
            assertThat(source.get("className")).describedAs("The class is not correct.").isEqualTo("com.bar.Foo");
            assertThat(source.get("methodName")).describedAs("The method is not correct.").isEqualTo("anotherMethod03");
            assertThat(source.get("fileName")).describedAs("The file name is not correct.").isEqualTo("Foo.java");
            assertThat(source.get("lineNumber")).describedAs("The line number is not correct.").isEqualTo(9);

            assertThat(object.get("marker") instanceof Map).describedAs("The marker should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> marker = (Map<String, Object>) object.get("marker");
            assertThat(marker.get("name")).describedAs("The marker name is not correct.").isEqualTo("AnotherMarker");

            assertThat(marker.get("parents") instanceof List).describedAs("The marker parents should be a list.").isTrue();
            @SuppressWarnings("unchecked")
            final List<Object> markerParents = (List<Object>) marker.get("parents");
            assertThat(markerParents).describedAs("The marker parents should contain two parents").hasSize(2);

            assertThat(markerParents.get(0) instanceof Map).describedAs("The marker parents[0] should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent1 = (Map<String, Object>) markerParents.get(0);
            assertThat(parent1.get("name")).describedAs("The first marker parent name is not correct.").isEqualTo("Parent1");

            assertThat(markerParents.get(1) instanceof Map).describedAs("The marker parents[1] should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent2 = (Map<String, Object>) markerParents.get(1);
            assertThat(parent2.get("name")).describedAs("The second marker parent name is not correct.").isEqualTo("Parent2");
            assertThat(parent2.get("parent")).describedAs("The second marker should have no parent.").isNull();

            assertThat(parent1.get("parents") instanceof List).describedAs("The parent1 parents should be a list.").isTrue();
            @SuppressWarnings("unchecked")
            final List<Object> parent1Parents = (List<Object>) parent1.get("parents");
            assertThat(parent1Parents).describedAs("The parent1 parents should have only one parent").hasSize(1);

            assertThat(parent1Parents.get(0) instanceof Map).describedAs("The parent1Parents[0] should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> parent1parent = (Map<String, Object>) parent1Parents.get(0);
            assertThat(parent1parent.get("name")).describedAs("The first parent1 parent name is not correct.").isEqualTo("GrandParent1");
            assertThat(parent1parent.get("parent")).describedAs("The parent1parent marker should have no parent.").isNull();

            assertThat(object.get("thrown") instanceof Map).describedAs("The thrown should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> thrown = (Map<String, Object>) object.get("thrown");
            assertThat(thrown.get("type")).describedAs("The thrown type is not correct.").isEqualTo("java.lang.IllegalStateException");
            assertThat(thrown.get("message")).describedAs("The thrown message is not correct.").isEqualTo("This is the result.");
            assertThat(thrown.get("stackTrace") instanceof List).describedAs("The thrown stack trace should be a list.").isTrue();
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> stackTrace = (List<Map<String, Object>>) thrown.get("stackTrace");
            assertThat(stackTrace).describedAs("The thrown stack trace length is not correct.").hasSameSizeAs(exception2.getStackTrace());
            for (int i = 0; i < exception2.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception2.getStackTrace()[i];
                final Map<String, Object> e2 = stackTrace.get(i);

                assertThat(e2.get("className")).describedAs("Element class name [" + i + "] is not correct.").isEqualTo(e1.getClassName());
                assertThat(e2.get("methodName")).describedAs("Element method name [" + i + "] is not correct.").isEqualTo(e1.getMethodName());
                assertThat(e2.get("fileName")).describedAs("Element file name [" + i + "] is not correct.").isEqualTo(e1.getFileName());
                assertThat(e2.get("lineNumber")).describedAs("Element line number [" + i + "] is not correct.").isEqualTo(e1.getLineNumber());
            }
            assertThat(thrown.get("cause") instanceof Map).describedAs("The thrown cause should be a map.").isTrue();
            @SuppressWarnings("unchecked")
            final Map<String, Object> cause = (Map<String, Object>) thrown.get("cause");
            assertThat(cause.get("type")).describedAs("The cause type is not correct.").isEqualTo("java.io.IOException");
            assertThat(cause.get("message")).describedAs("The cause message is not correct.").isEqualTo("This is the cause.");
            assertThat(cause.get("stackTrace") instanceof List).describedAs("The cause stack trace should be a list.").isTrue();
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> causeStackTrace = (List<Map<String, Object>>) cause.get("stackTrace");
            assertThat(causeStackTrace).describedAs("The cause stack trace length is not correct.").hasSameSizeAs(exception1.getStackTrace());
            for (int i = 0; i < exception1.getStackTrace().length; i++) {
                final StackTraceElement e1 = exception1.getStackTrace()[i];
                final Map<String, Object> e2 = causeStackTrace.get(i);

                assertThat(e2.get("className")).describedAs("Element class name [" + i + "] is not correct.").isEqualTo(e1.getClassName());
                assertThat(e2.get("methodName")).describedAs("Element method name [" + i + "] is not correct.").isEqualTo(e1.getMethodName());
                assertThat(e2.get("fileName")).describedAs("Element file name [" + i + "] is not correct.").isEqualTo(e1.getFileName());
                assertThat(e2.get("lineNumber")).describedAs("Element line number [" + i + "] is not correct.").isEqualTo(e1.getLineNumber());
            }
            assertThat(cause.get("cause")).describedAs("The cause should have no cause.").isNull();

            assertThat(object.get("contextMap") instanceof Map).describedAs("The context map should be a map.").isTrue();
            assertThat(object.get("contextMap")).describedAs("The context map is not correct.").isEqualTo(context);

            assertThat(object.get("contextStack") instanceof List).describedAs("The context stack should be list.").isTrue();
            assertThat(object.get("contextStack")).describedAs("The context stack is not correct.").isEqualTo(stack.asList());
        }
    }
}
