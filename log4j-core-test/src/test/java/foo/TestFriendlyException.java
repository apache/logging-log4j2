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
package foo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.Socket;
import java.util.Arrays;
import java.util.stream.Stream;
import org.apache.logging.log4j.util.Constants;

/**
 * A testing friendly exception featuring
 * <ul>
 * <li>Distinct localized message</li>
 * <li>Non-Log4j<sup>1</sup> and fixed<sup>2</sup> (to {@code bar}) package origin</li>
 * <li>Sufficient causal chain depth</li>
 * <li>Circular causal chain</li>
 * <li>Suppressed exceptions</li>
 * <li>Clutter-free stack trace (i.e., elements from JUnit, JDK, etc.)</li>
 * <li>Stack trace elements from named modules<sup>3</sup></li>
 * </ul>
 * <p>
 * <sup>1</sup> Helps with observing stack trace manipulation effects of Log4j.
 * </p>
 * <p>
 * <sup>2</sup> Helps to make the origin of {@link #INSTANCE} independent of the first test accessing to it.
 * </p>
 * <p>
 * <sup>3</sup> Helps with testing module name serialization.
 * </p>
 */
public final class TestFriendlyException extends RuntimeException {

    static {
        // Ensure the distinct packaging
        assertThat(TestFriendlyException.class.getPackage().getName()).doesNotStartWith("org.apache");
    }

    public static final StackTraceElement ORG_APACHE_REPLACEMENT_STACK_TRACE_ELEMENT =
            new StackTraceElement("bar.OrgApacheReplacement", "someMethod", "OrgApacheReplacement.java", 0);

    public static final StackTraceElement NAMED_MODULE_STACK_TRACE_ELEMENT = namedModuleStackTraceElement();

    @SuppressWarnings("resource")
    private static StackTraceElement namedModuleStackTraceElement() {
        try {
            new Socket("0.0.0.0", -1);
        } catch (final Exception error) {
            final StackTraceElement[] stackTraceElements = error.getStackTrace();
            final String socketClassName = Socket.class.getCanonicalName();
            for (final StackTraceElement stackTraceElement : stackTraceElements) {
                if (stackTraceElement.getClassName().equals(socketClassName)) {
                    if (Constants.JAVA_MAJOR_VERSION > 8) {
                        final String stackTraceElementString = stackTraceElement.toString();
                        assertThat(stackTraceElementString).startsWith("java.base/");
                    }
                    return stackTraceElement;
                }
            }
        }
        throw new IllegalStateException("should not have reached here");
    }

    private static final String[] EXCLUDED_CLASS_NAME_PREFIXES = {
        "java.lang", "jdk.internal", "org.junit", "sun.reflect"
    };

    public static final TestFriendlyException INSTANCE = create("r", 0, 2, new boolean[] {false}, new boolean[] {true});

    private static TestFriendlyException create(
            final String name,
            final int depth,
            final int maxDepth,
            final boolean[] circular,
            final boolean[] namedModuleAllowed) {
        final TestFriendlyException error = new TestFriendlyException(name, namedModuleAllowed);
        if (depth < maxDepth) {
            final TestFriendlyException cause = create(name + "_c", depth + 1, maxDepth, circular, namedModuleAllowed);
            error.initCause(cause);
            final TestFriendlyException suppressed =
                    create(name + "_s", depth + 1, maxDepth, circular, namedModuleAllowed);
            error.addSuppressed(suppressed);
            final boolean circularAllowed = depth + 1 == maxDepth && !circular[0];
            if (circularAllowed) {
                cause.initCause(error);
                suppressed.initCause(error);
                circular[0] = true;
            }
        }
        return error;
    }

    private TestFriendlyException(final String message, final boolean[] namedModuleAllowed) {
        super(message);
        removeExcludedStackTraceElements(namedModuleAllowed);
    }

    private void removeExcludedStackTraceElements(final boolean[] namedModuleAllowed) {
        final StackTraceElement[] oldStackTrace = getStackTrace();
        final boolean[] seenExcludedStackTraceElement = {false};
        final StackTraceElement[] newStackTrace = Arrays.stream(oldStackTrace)
                .flatMap(stackTraceElement ->
                        mapStackTraceElement(stackTraceElement, namedModuleAllowed, seenExcludedStackTraceElement))
                .toArray(StackTraceElement[]::new);
        setStackTrace(newStackTrace);
    }

    private static Stream<StackTraceElement> mapStackTraceElement(
            final StackTraceElement stackTraceElement,
            final boolean[] namedModuleAllowed,
            final boolean[] seenExcludedStackTraceElement) {
        final Stream<StackTraceElement> filteredStackTraceElement =
                filterStackTraceElement(stackTraceElement, seenExcludedStackTraceElement);
        final Stream<StackTraceElement> javaBaseIncludedStackTraceElement =
                namedModuleIncludedStackTraceElement(namedModuleAllowed);
        return Stream.concat(javaBaseIncludedStackTraceElement, filteredStackTraceElement);
    }

    private static Stream<StackTraceElement> filterStackTraceElement(
            final StackTraceElement stackTraceElement, final boolean[] seenExcludedStackTraceElement) {

        // Short-circuit if we have already encountered an excluded stack trace element
        if (seenExcludedStackTraceElement[0]) {
            return Stream.empty();
        }

        // Check if the class name is excluded
        final String className = stackTraceElement.getClassName();
        for (final String excludedClassNamePrefix : EXCLUDED_CLASS_NAME_PREFIXES) {
            if (className.startsWith(excludedClassNamePrefix)) {
                seenExcludedStackTraceElement[0] = true;
                return Stream.empty();
            }
        }

        // Replace `org.apache`-originating entries with a constant one.
        // Without this, `INSTANCE` might yield different origin depending on the first class accessing to it.
        // We remove this ambiguity and fix our origin to a constant instead.
        if (className.startsWith("org.apache")) {
            return Stream.of(ORG_APACHE_REPLACEMENT_STACK_TRACE_ELEMENT);
        }

        // Otherwise, it looks good
        return Stream.of(stackTraceElement);
    }

    private static Stream<StackTraceElement> namedModuleIncludedStackTraceElement(final boolean[] namedModuleAllowed) {
        if (!namedModuleAllowed[0]) {
            return Stream.of();
        }
        namedModuleAllowed[0] = false;
        return Stream.of(NAMED_MODULE_STACK_TRACE_ELEMENT);
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage() + " [localized]";
    }
}
