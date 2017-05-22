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

import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * <em>Consider this class private.</em> Determines the caller's class.
 */
public class StackWalkerStackLocator implements StackLocator {

    private final static StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private final static StackWalker stackWalker = StackWalker.getInstance();

    public Class<?> getCallerClass(final String fqcn) {
        return getCallerClass(fqcn, "");
    }

    public Class<?> getCallerClass(final String fqcn, final String pkg) {
        return walker.walk(s -> s.filter(new ClassNamePredicate(fqcn)).findFirst()).get().getDeclaringClass();
    }

    public Class<?> getCallerClass(final Class<?> anchor) {
        return walker.walk(s -> s.filter(new ClassPredicate(anchor)).findFirst()).get().getDeclaringClass();
    }

    public Class<?> getCallerClass(final int depth) {
        ;
        return walker.walk(s -> s.skip(depth).findFirst()).get().getDeclaringClass();
    }

    public Stack<Class<?>> getCurrentStackTrace() {
        Stack<Class<?>> stack = new Stack<Class<?>>();
        List<Class<?>> classes = walker.walk(s -> s.map(f -> f.getDeclaringClass()).collect(Collectors.toList()));
        stack.addAll(classes);
        return stack;
    }

    public StackTraceElement calcLocation(final String fqcnOfLogger) {
        return stackWalker.walk(s -> s.filter(new ClassNamePredicate(fqcnOfLogger)).findFirst()).get().toStackTraceElement();
    }

    public StackTraceElement getStackTraceElement(final int depth) {
        return stackWalker.walk(s -> s.skip(depth).findFirst()).get().toStackTraceElement();
    }
}
