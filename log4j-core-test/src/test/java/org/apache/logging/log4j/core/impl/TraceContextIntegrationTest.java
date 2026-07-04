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
package org.apache.logging.log4j.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.RingBufferLogEvent;
import org.apache.logging.log4j.core.async.RingBufferLogEventTranslator;
import org.apache.logging.log4j.core.pattern.SpanIdPatternConverter;
import org.apache.logging.log4j.core.pattern.TraceFlagsPatternConverter;
import org.apache.logging.log4j.core.pattern.TraceIdPatternConverter;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.core.util.TraceContextProviderService;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.TraceContextProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TraceContextIntegrationTest {

    private final TestTraceContextProvider testProvider = new TestTraceContextProvider();

    @BeforeEach
    void setUp() {
        TraceContextProviderService.setActiveProvider(testProvider);
        TestTraceContextProvider.setContext("4bf92f3577b34da6a3ce929d0e0e4736", "00f067aa0ba902b7", "01");
        ThreadContext.clearMap();
    }

    @AfterEach
    void tearDown() {
        TraceContextProviderService.setActiveProvider(null);
        TestTraceContextProvider.clearContext();
        ThreadContext.clearMap();
    }

    @Test
    public void testLog4jLogEventNativeCaptureWithEmptyThreadContext() {
        assertThat(ThreadContext.getContext()).isEmpty();

        final LogEvent event = Log4jLogEvent.newBuilder()
                .setLoggerName("TestLogger")
                .setLevel(Level.INFO)
                .setMessage(new SimpleMessage("Standard event testing"))
                .build();

        assertThat(event.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(event.getSpanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(event.getTraceFlags()).isEqualTo("01");
        assertThat(event.getContextData().isEmpty()).isTrue();
    }

    @Test
    public void testMutableLogEventNativeCapture() {
        final MutableLogEvent event = new MutableLogEvent();
        event.setMessage(new SimpleMessage("Mutable event testing"));
        event.initTime(ClockFactory.getClock(), new DummyNanoClock());

        assertThat(event.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(event.getSpanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(event.getTraceFlags()).isEqualTo("01");
        assertThat(event.getContextData().isEmpty()).isTrue();
    }

    @Test
    public void testRingBufferLogEventTranslatorNativeCapture() {
        final RingBufferLogEvent event = new RingBufferLogEvent();
        final RingBufferLogEventTranslator translator = new RingBufferLogEventTranslator();

        translator.setBasicValues(
                null,
                "Logger",
                null,
                "FQCN",
                Level.INFO,
                new SimpleMessage("Async event testing"),
                null,
                ThreadContext.getImmutableStack(),
                null,
                ClockFactory.getClock(),
                new DummyNanoClock());

        translator.translateTo(event, 0);

        assertThat(event.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(event.getSpanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(event.getTraceFlags()).isEqualTo("01");
        assertThat(event.getContextData().isEmpty()).isTrue();
    }

    @Test
    public void testLog4jLogEventDirectConstructorNativeCapture() {
        final LogEvent event = new Log4jLogEvent(
                "TestLogger", null, "FQCN", Level.INFO, new SimpleMessage("Direct constructor testing"), null, null);

        assertThat(event.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(event.getSpanId()).isEqualTo("00f067aa0ba902b7");
        assertThat(event.getTraceFlags()).isEqualTo("01");
        assertThat(event.getContextData().isEmpty()).isTrue();
    }

    @Test
    public void testProviderExceptionSafety() {
        TraceContextProviderService.setActiveProvider(new TraceContextProvider() {
            @Override
            public String getTraceId() {
                throw new RuntimeException("Simulated provider failure");
            }

            @Override
            public String getSpanId() {
                return null;
            }

            @Override
            public String getTraceFlags() {
                return null;
            }
        });

        assertThatCode(() -> {
                    final LogEvent event = Log4jLogEvent.newBuilder()
                            .setLoggerName("BuggyLogger")
                            .setLevel(Level.ERROR)
                            .setMessage(new SimpleMessage("Exception safety test"))
                            .build();

                    assertThat(event.getTraceId()).isEmpty();
                })
                .doesNotThrowAnyException();
    }

    @Test
    public void testTracePatternConverters() {
        // 1. Setup an event with trace data
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setTraceId("test-trace-123")
                .setSpanId("test-span-456")
                .setTraceFlags("01")
                .build();

        // 2. Test Trace ID Converter
        final StringBuilder traceSb = new StringBuilder();
        org.apache.logging.log4j.core.pattern.TraceIdPatternConverter.newInstance(null)
                .format(event, traceSb);
        assertThat(traceSb.toString()).isEqualTo("test-trace-123");

        // 3. Test Span ID Converter
        final StringBuilder spanSb = new StringBuilder();
        org.apache.logging.log4j.core.pattern.SpanIdPatternConverter.newInstance(null)
                .format(event, spanSb);
        assertThat(spanSb.toString()).isEqualTo("test-span-456");

        // 4. Test Trace Flags Converter
        final StringBuilder flagsSb = new StringBuilder();
        org.apache.logging.log4j.core.pattern.TraceFlagsPatternConverter.newInstance(null)
                .format(event, flagsSb);
        assertThat(flagsSb.toString()).isEqualTo("01");
    }

    @Test
    public void testTracePatternConvertersWithNullValues() {
        TestTraceContextProvider.clearContext();

        final LogEvent event = Log4jLogEvent.newBuilder().build();
        final StringBuilder sb = new StringBuilder();

        TraceIdPatternConverter.newInstance(null).format(event, sb);
        SpanIdPatternConverter.newInstance(null).format(event, sb);
        TraceFlagsPatternConverter.newInstance(null).format(event, sb);

        assertThat(sb.toString()).isEmpty();
    }
}
