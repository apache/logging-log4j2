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

import java.util.Arrays;

/**
 * A testing friendly exception featuring
 * <ul>
 * <li>Non-Log4j package origin<sup>1</sup></li>
 * <li>Sufficient causal chain depth</li>
 * <li>Decorated with suppressed exceptions</li>
 * <li>A stack trace free of clutter (i.e., elements from JUnit, JDK, etc.)</li>
 * </ul>
 * <p>
 * <sup>1</sup> This becomes handy for tests observing stack trace manipulation effects of Log4j.
 * </p>
 */
public final class TestFriendlyException extends RuntimeException {

    private static final String[] EXCLUDED_CLASS_NAME_PREFIXES = {
        "java.lang", "jdk.internal", "org.junit", "sun.reflect"
    };

    public static final TestFriendlyException INSTANCE = create("r", 0, 2);

    static {
        // Ensure the distinct packaging
        assertThat(TestFriendlyException.class.getPackage().getName()).doesNotStartWith("org.apache");
    }

    private static TestFriendlyException create(final String name, final int depth, final int maxDepth) {
        final TestFriendlyException error = new TestFriendlyException(name);
        if (depth < maxDepth) {
            error.initCause(create(name + "_c", depth + 1, maxDepth));
            error.addSuppressed(create(name + "_s", depth + 1, maxDepth));
        }
        return error;
    }

    private TestFriendlyException(final String message) {
        super(message);
        removeExcludedStackTraceElements();
    }

    private void removeExcludedStackTraceElements() {
        final StackTraceElement[] oldStackTrace = getStackTrace();
        final boolean[] seenExcludedStackTraceElement = {false};
        final StackTraceElement[] newStackTrace = Arrays.stream(oldStackTrace)
                .filter(stackTraceElement -> {
                    if (seenExcludedStackTraceElement[0]) {
                        return false;
                    }
                    final String className = stackTraceElement.getClassName();
                    for (final String excludedClassNamePrefix : EXCLUDED_CLASS_NAME_PREFIXES) {
                        if (className.startsWith(excludedClassNamePrefix)) {
                            seenExcludedStackTraceElement[0] = true;
                            return false;
                        }
                    }
                    return true;
                })
                .toArray(StackTraceElement[]::new);
        setStackTrace(newStackTrace);
    }
}
