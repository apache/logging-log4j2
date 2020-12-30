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
package org.apache.logging.log4j.util;

import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * <em>Consider this class private.</em> Determines the caller's class.
 */
public class StackLocator {

    // Retaining class references is more expensive than the default configuration, 'stackWalker'
    // should be preferred when possible.
    private final static StackWalker walkerWithClassRefs =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final static StackWalker stackWalker = StackWalker.getInstance();

    private final static StackLocator INSTANCE = new StackLocator();

    // Lambda functions are extracted to reduce allocations.
    private static final Function<StackWalker.StackFrame, Class<?>> STACK_FRAME_DECLARING_CLASS =
            StackWalker.StackFrame::getDeclaringClass;

    private static final Function<StackWalker.StackFrame, StackTraceElement> STACK_FRAME_TRACE_ELEMENT =
            StackWalker.StackFrame::toStackTraceElement;

    private static final Function<Stream<StackWalker.StackFrame>, Stack<Class<?>>> STACK_TRACE_FUNCTION = s -> {
        Stack<Class<?>> stack = new Stack<>();
        s.map(STACK_FRAME_DECLARING_CLASS).forEach(stack::add);
        return stack;
    };

    public static StackLocator getInstance() {
        return INSTANCE;
    }

    private StackLocator() {
    }

    public Class<?> getCallerClass(final String fqcn) {
        return getCallerClass(fqcn, "");
    }

    public Class<?> getCallerClass(final String fqcn, final String pkg) {
        return walkerWithClassRefs.walk(s -> s.dropWhile(f -> !f.getClassName().equals(fqcn))
                    .dropWhile(f -> f.getClassName().equals(fqcn))
                    .dropWhile(f -> !f.getClassName().startsWith(pkg))
                    .findFirst())
                .map(STACK_FRAME_DECLARING_CLASS)
                .orElse(null);
    }

    public Class<?> getCallerClass(final Class<?> anchor) {
        return walkerWithClassRefs.walk(s -> s.dropWhile(f -> !f.getDeclaringClass().equals(anchor))
                    .dropWhile(f -> f.getDeclaringClass().equals(anchor))
                    .findFirst())
                .map(STACK_FRAME_DECLARING_CLASS)
                .orElse(null);
    }

    public Class<?> getCallerClass(final int depth) {
        return walkerWithClassRefs.walk(new GetCallerClassWalker(depth));
    }

    public Stack<Class<?>> getCurrentStackTrace() {
        // benchmarks show that using the SecurityManager is much faster than looping through getCallerClass(int)
        if (PrivateSecurityManagerStackTraceUtil.isEnabled()) {
            return PrivateSecurityManagerStackTraceUtil.getCurrentStackTrace();
        }
        return walkerWithClassRefs.walk(STACK_TRACE_FUNCTION);
    }

    public StackTraceElement calcLocation(final String fqcnOfLogger) {
        if (fqcnOfLogger == null) {
            return null;
        }
        return stackWalker.walk(
                s -> s.dropWhile(f -> !f.getClassName().equals(fqcnOfLogger)) // drop the top frames until we reach the logger
                        .dropWhile(f -> f.getClassName().equals(fqcnOfLogger)) // drop the logger frames
                        .findFirst())
                .map(STACK_FRAME_TRACE_ELEMENT)
                .orElse(null);
    }

    public StackTraceElement getStackTraceElement(final int depth) {
        return stackWalker.walk(new GetStackTraceElementWalker(depth));
    }

    // Complex functions are extracted to combine stream operations and reduce overhead.
    private static final class GetCallerClassWalker implements Function<Stream<StackWalker.StackFrame>, Class<?>> {

        private final int depth;

        GetCallerClassWalker(int depth) {
            this.depth = depth;
        }

        @Override
        public Class<?> apply(Stream<StackWalker.StackFrame> frames) {
            StackWalker.StackFrame frame = frames.skip(depth).findFirst().orElse(null);
            return frame != null ? frame.getDeclaringClass() : null;
        }
    }

    // Complex functions are extracted to combine stream operations and reduce overhead.
    private static final class GetStackTraceElementWalker
            implements Function<Stream<StackWalker.StackFrame>, StackTraceElement> {

        private final int depth;

        GetStackTraceElementWalker(int depth) {
            this.depth = depth;
        }

        @Override
        public StackTraceElement apply(Stream<StackWalker.StackFrame> frames) {
            StackWalker.StackFrame frame = frames.skip(depth).findFirst().orElse(null);
            return frame != null ? frame.toStackTraceElement() : null;
        }
    }
}
