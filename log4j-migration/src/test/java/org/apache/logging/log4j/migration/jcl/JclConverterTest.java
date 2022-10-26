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
package org.apache.logging.log4j.migration.jcl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.migration.AbstractClassConverter;
import org.apache.logging.log4j.migration.ConverterProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@LoggerContextSource("log4j2-test.xml")
public class JclConverterTest {

    private static Class<?> convertedClass;
    private static Object testObject;

    @BeforeAll
    public static void setup() throws ReflectiveOperationException, IOException {
        final AbstractClassConverter converter = new JclClassConverter(ConverterProfile.FULL);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        converter.convert(null, JclConverterTest.class.getResourceAsStream("JclConverterExample.class"), os);
        convertedClass = MethodHandles.lookup().defineClass(os.toByteArray());
        testObject = convertedClass.getConstructor().newInstance();
    }

    @Test
    public void testLog(final @Named("List") ListAppender app) throws Exception {
        final Throwable t = new RuntimeException();
        convertedClass.getDeclaredMethod("testLog", Throwable.class).invoke(testObject, t);
        final List<LogEvent> events = app.getEvents();
        assertThat(events).extracting(LogEvent::getLevel).containsExactly(Level.FATAL, Level.FATAL, Level.ERROR,
                Level.ERROR, Level.WARN, Level.WARN, Level.INFO, Level.INFO, Level.DEBUG, Level.DEBUG, Level.TRACE,
                Level.TRACE);
        assertThat(events).extracting(LogEvent::getThrown).filteredOn(Objects::nonNull).allMatch(t::equals);
    }

    static Stream<Arguments> testInstances() {
        return Stream.of(Arguments.of("getLogFactory", org.apache.logging.log4j.spi.LoggerContext.class),
                Arguments.of("getLogFromString", Logger.class), Arguments.of("getLogFromClass", Logger.class),
                Arguments.of("getLogFromFactoryAndString", Logger.class),
                Arguments.of("getLogFromFactoryAndClass", Logger.class));
    }

    @ParameterizedTest
    @MethodSource
    public void testInstances(final String methodName, final Class<?> clazz) throws Exception {
        assertThat(convertedClass.getMethod(methodName).invoke(testObject)).isInstanceOf(clazz);
    }
}
