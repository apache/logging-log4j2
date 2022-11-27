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
package org.apache.logging.log4j.instrument.location;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.util.stream.Stream;

import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@LoggerContextSource("log4j2-test.xml")
public class LocationTest {

    private static Class<?> convertedClass;
    private static Object testObject;
    private static ListAppender appender;

    @BeforeAll
    public static void setup(final @Named("List") ListAppender appender) throws ReflectiveOperationException, IOException {
        final ByteArrayOutputStream dest = new ByteArrayOutputStream();
        final LocationClassConverter converter = new LocationClassConverter();
        final LocationCache locationCache = new LocationCache();
        converter.convert(LocationTest.class.getResourceAsStream("LocationExample.class"), dest, locationCache);

        final Lookup lookup = MethodHandles.lookup();
        locationCache.generateClasses().values().forEach(t -> assertDoesNotThrow(() -> lookup.defineClass(t)));
        convertedClass = lookup.defineClass(dest.toByteArray());
        testObject = convertedClass.getConstructor().newInstance();

        LocationTest.appender = appender;
    }

    static Stream<String> testLocationConverter() {
        return Stream.of("testFatal", "testError", "testWarn", "testInfo", "testDebug", "testLog", "testFrames");
    }

    @ParameterizedTest
    @MethodSource
    public void testLocationConverter(final String methodName) throws Exception {
        convertedClass.getMethod(methodName, ListAppender.class).invoke(testObject, appender);
    }
}
