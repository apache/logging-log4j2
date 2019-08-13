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

import java.util.NoSuchElementException;
import java.util.Stack;

import org.apache.logging.log4j.LogBuilder;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * <em>Consider this class private.</em> Provides various methods to determine the caller class. <h3>Background</h3>
 */
public final class StackLocatorUtil {
    private static StackLocator stackLocator = null;
    private static volatile boolean errorLogged = false;

    static {
        stackLocator = StackLocator.getInstance();
    }

    private StackLocatorUtil() {
    }

    // TODO: return Object.class instead of null (though it will have a null ClassLoader)
    // (MS) I believe this would work without any modifications elsewhere, but I could be wrong

    // migrated from ReflectiveCallerClassUtility
    @PerformanceSensitive
    public static Class<?> getCallerClass(final int depth) {
        return stackLocator.getCallerClass(depth + 1);
    }

    public static StackTraceElement getStackTraceElement(final int depth) {
        return stackLocator.getStackTraceElement(depth + 1);
    }

    // migrated from ClassLoaderContextSelector
    @PerformanceSensitive
    public static Class<?> getCallerClass(final String fqcn) {
        return getCallerClass(fqcn, Strings.EMPTY);
    }

    // migrated from Log4jLoggerFactory
    @PerformanceSensitive
    public static Class<?> getCallerClass(final String fqcn, final String pkg) {
        return stackLocator.getCallerClass(fqcn, pkg);
    }

    // added for use in LoggerAdapter implementations mainly
    @PerformanceSensitive
    public static Class<?> getCallerClass(final Class<?> anchor) {
        return stackLocator.getCallerClass(anchor);
    }

    // migrated from ThrowableProxy
    @PerformanceSensitive
    public static Stack<Class<?>> getCurrentStackTrace() {
        return stackLocator.getCurrentStackTrace();
    }

    public static StackTraceElement calcLocation(final String fqcnOfLogger) {
        try {
            return stackLocator.calcLocation(fqcnOfLogger);
        } catch (NoSuchElementException ex) {
            if (!errorLogged) {
                errorLogged = true;
                StatusLogger.getLogger().warn("Unable to locate stack trace element for {}", fqcnOfLogger, ex);
            }
            return null;
        }
    }
}
