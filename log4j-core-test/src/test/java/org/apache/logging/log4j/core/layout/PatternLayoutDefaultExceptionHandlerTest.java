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
package org.apache.logging.log4j.core.layout;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.Test;

class PatternLayoutDefaultExceptionHandlerTest {

    private static final Configuration CONFIG = new NullConfiguration();

    private static final Exception EXCEPTION = new RuntimeException("foo");

    @Test
    void default_exception_handler_should_be_provided() {
        final String threadName = Thread.currentThread().getName();
        final String exceptionClassName = EXCEPTION.getClass().getCanonicalName();
        final String exceptionMessage = EXCEPTION.getMessage();
        final String firstLine = String.format("%s%n%s: %s", threadName, exceptionClassName, exceptionMessage);
        assertThatPatternEncodes("%t", true).startsWith(firstLine);
    }

    @Test
    void default_exception_handler_should_be_provided_after_newline() {
        final String threadName = Thread.currentThread().getName();
        final String exceptionClassName = EXCEPTION.getClass().getCanonicalName();
        final String exceptionMessage = EXCEPTION.getMessage();
        final String firstLine = String.format("%s%n%s: %s", threadName, exceptionClassName, exceptionMessage);
        assertThatPatternEncodes("%t%n", true).startsWith(firstLine);
    }

    @Test
    void default_exception_handler_should_not_be_provided_if_user_provides_one() {
        final String className = EXCEPTION.getStackTrace()[0].getClassName();
        assertThatPatternEncodes("%ex{short.className}", true).isEqualTo(className);
    }

    @Test
    void default_exception_handler_should_not_be_provided_if_alwaysWriteExceptions_disabled() {
        final String threadName = Thread.currentThread().getName();
        assertThatPatternEncodes("%t", false).isEqualTo(threadName);
    }

    private static AbstractStringAssert<?> assertThatPatternEncodes(
            final String pattern, final boolean alwaysWriteExceptions) {
        final Layout<String> layout = PatternLayout.newBuilder()
                .withConfiguration(CONFIG)
                .withPattern(pattern)
                .withAlwaysWriteExceptions(alwaysWriteExceptions)
                .build();
        final LogEvent event = Log4jLogEvent.newBuilder().setThrown(EXCEPTION).build();
        return assertThat(layout.toSerializable(event)).as("pattern=`%s`", pattern);
    }
}
