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
package org.apache.logging.log4j.core.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@LoggerContextSource("org/apache/logging/log4j/core/test/LogBuilderTest.xml")
public class LogBuilderTest {

    private static final Marker MARKER = MarkerManager.getMarker("TestMarker");
    private static final CharSequence CHAR_SEQUENCE = "CharSequence";
    private static final String STRING = "String";
    private static final Message MESSAGE = new SimpleMessage();
    private static final Throwable THROWABLE = new RuntimeException();
    private static final Object[] P = {"p0", "p1", "p2", "p3", "p4", "p5", "p6", "p7", "p8", "p9"};
    private static final Object OBJECT = "Object";

    private Logger logger;
    private ListAppender appender;

    @BeforeEach
    public void setupAppender(final LoggerContext context, @Named("LIST") final ListAppender appender) {
        this.logger = context.getLogger(getClass());
        this.appender = appender;
    }

    static Stream<Consumer<LogBuilder>> testMarkerFilter() {
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
    @MethodSource
    void testMarkerFilter(final Consumer<LogBuilder> consumer) {
        appender.clear();
        consumer.accept(logger.atTrace().withMarker(MARKER));
        consumer.accept(logger.atTrace().withThrowable(THROWABLE).withMarker(MARKER));
        assertThat(appender.getEvents()).hasSize(2);
    }
}
