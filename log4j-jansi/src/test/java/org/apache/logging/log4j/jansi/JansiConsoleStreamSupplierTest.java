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
package org.apache.logging.log4j.jansi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.fusesource.jansi.AnsiConsole;
import org.junit.jupiter.api.Test;

public class JansiConsoleStreamSupplierTest {

    @Test
    @LoggerContextSource("JansiConsoleStreamSupplierTest.xml")
    void usesJansiByDefault(final @Named("CONSOLE") ConsoleAppender appender) {
        assumeThat(AnsiConsole.out()).isNotEqualTo(System.out);

        final OutputStreamManager manager = appender.getManager();
        assertOutputStreamIsEqual(AnsiConsole.out(), manager);
    }

    @Test
    @SetTestProperty(key = "log4j.console.jansiEnabled", value = "false")
    @LoggerContextSource("JansiConsoleStreamSupplierTest.xml")
    void whenJansiDisabled_usesSystemOut(final @Named("CONSOLE") ConsoleAppender appender) {
        assumeThat(AnsiConsole.out()).isNotEqualTo(System.out);

        final OutputStreamManager manager = appender.getManager();
        assertOutputStreamIsEqual(System.out, manager);
    }

    private static void assertOutputStreamIsEqual(final OutputStream expected, final OutputStreamManager manager) {
        final OutputStream wrappedActual = assertDoesNotThrow(() -> {
            final Method getOutputStream = OutputStreamManager.class.getDeclaredMethod("getOutputStream");
            getOutputStream.setAccessible(true);
            return (OutputStream) getOutputStream.invoke(manager);
        });
        assertThat(wrappedActual).isInstanceOf(CloseShieldOutputStream.class);
        final OutputStream actual = assertDoesNotThrow(() -> {
            final Field delegate = CloseShieldOutputStream.class.getDeclaredField("delegate");
            delegate.setAccessible(true);
            return (OutputStream) delegate.get(wrappedActual);
        });
        assertThat(actual).isEqualTo(expected);
    }
}
