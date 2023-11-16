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
package org.apache.logging.slf4j;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.testUtil.StringListAppender;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.CloseableThreadContext.Instance;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@UsingStatusListener
@LoggerContextSource
public class LogBuilderTest {

    private static final CharSequence CHAR_SEQUENCE = "CharSequence";
    private static final String STRING = "String";
    private static final Message MESSAGE = new SimpleMessage();
    private static final Object[] P = {"p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9"};
    private static final Object OBJECT = "Object";

    // Log4j objects
    private static Logger logger;
    // Logback objects
    private static LoggerContext context;
    private static StringListAppender<ILoggingEvent> list;

    @BeforeAll
    public static void setUp() throws Exception {
        final org.slf4j.Logger slf4jLogger = context.getLogger(LogBuilderTest.class);
        logger = LogManager.getLogger(LogBuilderTest.class);
        assertThat(slf4jLogger).isSameAs(((SLF4JLogger) logger).getLogger());
        final ch.qos.logback.classic.Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.detachAppender("console");
        list = TestUtil.getListAppender(rootLogger, "LIST");
        assertThat(list).isNotNull().extracting("strList").isNotNull();
        list.strList.clear();
    }

    static Stream<Consumer<LogBuilder>> logBuilderMethods() {
        return Stream.of(
                logBuilder -> logBuilder.log(),
                logBuilder -> logBuilder.log(CHAR_SEQUENCE),
                logBuilder -> logBuilder.log(MESSAGE),
                logBuilder -> logBuilder.log(OBJECT),
                logBuilder -> logBuilder.log(STRING),
                logBuilder -> logBuilder.log(STRING, P[0]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2], P[3]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2], P[3], P[4]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2], P[3], P[4], P[5]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2], P[3], P[4], P[5], P[6]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2], P[3], P[4], P[5], P[6], P[7]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2], P[3], P[4], P[5], P[6], P[7], P[8]),
                logBuilder -> logBuilder.log(STRING, P[0], P[1], P[2], P[3], P[4], P[5], P[6], P[7], P[8], P[9]),
                logBuilder -> logBuilder.log(STRING, P),
                logBuilder -> logBuilder.log(STRING, () -> OBJECT),
                logBuilder -> logBuilder.log(() -> MESSAGE));
    }

    @ParameterizedTest
    @MethodSource("logBuilderMethods")
    void testTurboFilter(final Consumer<LogBuilder> consumer) {
        consumer.accept(logger.atTrace());
        try (final Instance c = CloseableThreadContext.put("callerId", "Log4j2")) {
            consumer.accept(logger.atTrace());
            assertThat(list.strList).hasSize(1);
        }
        list.strList.clear();
    }

    @ParameterizedTest
    @MethodSource("logBuilderMethods")
    void testLevelThreshold(final Consumer<LogBuilder> consumer) {
        consumer.accept(logger.atInfo());
        assertThat(list.strList).hasSize(1);
        list.strList.clear();
    }
}
