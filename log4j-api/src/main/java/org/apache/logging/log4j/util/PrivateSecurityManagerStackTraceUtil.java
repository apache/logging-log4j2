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
package org.apache.logging.log4j.util;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;

/**
 * Internal utility to share a fast implementation of {@code #getCurrentStackTrace()}
 * with the java 9 implementation of {@link StackLocator}.
 */
final class PrivateSecurityManagerStackTraceUtil {

    private static final PrivateSecurityManager SECURITY_MANAGER;

    static {
        PrivateSecurityManager psm;
        try {
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new RuntimePermission("createSecurityManager"));
            }
            psm = new PrivateSecurityManager();
        } catch (final SecurityException ignored) {
            psm = null;
        }

        SECURITY_MANAGER = psm;
    }

    private PrivateSecurityManagerStackTraceUtil() {
        // Utility Class
    }

    static boolean isEnabled() {
        return SECURITY_MANAGER != null;
    }

    /**
     * Returns the current execution stack as a Deque of classes.
     * <p>
     * The size of the Deque is the number of methods on the execution stack. The first element is the class that started
     * execution on this thread, the next element is the class that was called next, and so on, until the last element: the
     * method that called {@link SecurityManager#getClassContext()} to capture the stack.
     * </p>
     *
     * @return the execution stack.
     */
    // benchmarks show that using the SecurityManager is much faster than looping through getCallerClass(int)
    static Deque<Class<?>> getCurrentStackTrace() {
        final Class<?>[] array = SECURITY_MANAGER.getClassContext();
        final Deque<Class<?>> classes = new ArrayDeque<>(array.length);
        Collections.addAll(classes, array);
        return classes;
    }

    private static final class PrivateSecurityManager extends SecurityManager {

        @Override
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
}
