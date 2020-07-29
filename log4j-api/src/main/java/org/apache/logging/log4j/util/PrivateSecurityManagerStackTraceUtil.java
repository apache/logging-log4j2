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

/**
 * Internal utility to share a fast implementation of {@link #getCurrentStackTrace()}
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

    // benchmarks show that using the SecurityManager is much faster than looping through getCallerClass(int)
    static Stack<Class<?>> getCurrentStackTrace() {
        final Class<?>[] array = SECURITY_MANAGER.getClassContext();
        final Stack<Class<?>> classes = new Stack<>();
        classes.ensureCapacity(array.length);
        for (final Class<?> clazz : array) {
            classes.push(clazz);
        }
        return classes;
    }

    private static final class PrivateSecurityManager extends SecurityManager {

        @Override
        protected Class<?>[] getClassContext() {
            return super.getClassContext();
        }

    }
}
