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
package org.apache.logging.log4j.core.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.List;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

/**
 * Tests for {@link ThrowableExtendedStackTraceRenderer}.
 *
 * <p>Verifies that {@link NoClassDefFoundError} during class loading
 * does not break the extended stack trace rendering ({@code %xEx}).</p>
 *
 * @see <a href="https://github.com/apache/logging-log4j2/issues/4028">#4028</a>
 */
class ThrowableExtendedStackTraceRendererTest {

    private static final PatternParser PATTERN_PARSER = PatternLayout.createPatternParser(null);

    /**
     * Verifies that the extended stack trace renderer does not propagate {@link NoClassDefFoundError}
     * when a class in the stack trace cannot be found by the class loader.
     */
    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4028")
    void loadClass_should_handle_NoClassDefFoundError() {

        // Create an exception with a stack trace element referencing a non-existent class.
        // When the extended renderer tries to resolve this class for JAR/version info,
        // the class loader will throw NoClassDefFoundError.
        final Exception exception = new Exception("test exception");
        final StackTraceElement[] originalTrace = exception.getStackTrace();
        final StackTraceElement[] modifiedTrace = new StackTraceElement[originalTrace.length + 1];
        modifiedTrace[0] = new StackTraceElement(
                "com.nonexistent.deliberately.missing.ClassName", "someMethod", "ClassName.java", 42);
        System.arraycopy(originalTrace, 0, modifiedTrace, 1, originalTrace.length);
        exception.setStackTrace(modifiedTrace);

        // Render using the extended throwable pattern (%xEx)
        final List<PatternFormatter> patternFormatters = PATTERN_PARSER.parse("%xEx", false, true, true);
        assertThat(patternFormatters).hasSize(1);
        final PatternFormatter patternFormatter = patternFormatters.get(0);

        final LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setThrown(exception)
                .setLevel(Level.ERROR)
                .build();
        final StringBuilder buffer = new StringBuilder();

        // The rendering should not throw any exception
        assertThatNoException().isThrownBy(() -> patternFormatter.format(logEvent, buffer));

        // The output should contain our non-existent class name and the exception message
        final String output = buffer.toString();
        assertThat(output).contains("com.nonexistent.deliberately.missing.ClassName");
        assertThat(output).contains("test exception");
    }

    /**
     * Verifies that the extended stack trace renderer gracefully handles a class loader
     * that throws {@link NoClassDefFoundError} during class resolution.
     *
     * <p>This simulates the real-world scenario from
     * <a href="https://github.com/apache/logging-log4j2/issues/4028">#4028</a>,
     * where custom class loaders (e.g., in application servers or frameworks like Frank!Framework)
     * throw {@link NoClassDefFoundError} for classes that have been unloaded.</p>
     */
    @Test
    @Issue("https://github.com/apache/logging-log4j2/issues/4028")
    void loadClass_should_handle_NoClassDefFoundError_from_custom_classloader() {

        // Create a class loader that always throws NoClassDefFoundError
        final ClassLoader errorThrowingLoader = new ClassLoader(null) {
            @Override
            public Class<?> loadClass(final String name) throws ClassNotFoundException {
                throw new NoClassDefFoundError("Simulated missing class: " + name);
            }
        };

        final Exception exception = new Exception("test with custom classloader");
        exception.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.example.UnloadedClass", "doWork", "UnloadedClass.java", 10),
            new StackTraceElement("com.example.CallerClass", "invoke", "CallerClass.java", 20)
        });

        final List<PatternFormatter> patternFormatters = PATTERN_PARSER.parse("%xEx", false, true, true);
        final PatternFormatter patternFormatter = patternFormatters.get(0);

        final LogEvent logEvent = Log4jLogEvent.newBuilder()
                .setThrown(exception)
                .setLevel(Level.ERROR)
                .build();
        final StringBuilder buffer = new StringBuilder();

        // Set the error-throwing class loader as the context class loader
        // so the renderer's loadClass method encounters it
        final Thread currentThread = Thread.currentThread();
        final ClassLoader originalLoader = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(errorThrowingLoader);

            // The rendering should succeed without propagating NoClassDefFoundError
            assertThatNoException().isThrownBy(() -> patternFormatter.format(logEvent, buffer));
        } finally {
            currentThread.setContextClassLoader(originalLoader);
        }

        // The output should still contain the exception and stack trace
        final String output = buffer.toString();
        assertThat(output).contains("test with custom classloader");
        assertThat(output).contains("com.example.UnloadedClass");
    }
}
