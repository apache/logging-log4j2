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

import org.apache.logging.log4j.spi.LoggingSystemProperty;

/**
 * Log4j API Constants.
 *
 * @since 2.6.2
 */
@InternalApi
public final class Constants {

    private static final LazyBoolean isWebApp = new LazyBoolean(() -> PropertiesUtil.getProperties()
            .getBooleanProperty(
                    LoggingSystemProperty.IS_WEBAPP,
                    LoaderUtil.isClassAvailable("javax.servlet.Servlet")
                            || LoaderUtil.isClassAvailable("jakarta.servlet.Servlet")));

    /**
     * {@code true} if we think we are running in a web container, based on the boolean value of system property
     * "log4j2.is.webapp", or (if this system property is not set) whether the  {@code javax.servlet.Servlet} class
     * is present in the classpath.
     */
    public static boolean isWebApp() {
        return isWebApp.getAsBoolean();
    }

    public static void setWebApp(final boolean webApp) {
        isWebApp.setAsBoolean(webApp);
    }

    public static void resetWebApp() {
        isWebApp.reset();
    }

    private static final LazyBoolean threadLocalsEnabled = new LazyBoolean(() -> !isWebApp()
            && PropertiesUtil.getProperties().getBooleanProperty(LoggingSystemProperty.THREAD_LOCALS_ENABLE, true));

    /**
     * Kill switch for object pooling in ThreadLocals that enables much of the LOG4J2-1270 no-GC behaviour.
     * <p>
     * {@code True} for non-{@link #isWebApp()} web apps}, disable by setting system property
     * "log4j2.enable.threadlocals" to "false".
     * </p>
     */
    public static boolean isThreadLocalsEnabled() {
        return threadLocalsEnabled.getAsBoolean();
    }

    public static void setThreadLocalsEnabled(final boolean enabled) {
        threadLocalsEnabled.setAsBoolean(enabled);
    }

    public static void resetThreadLocalsEnabled() {
        threadLocalsEnabled.reset();
    }

    public static final int JAVA_MAJOR_VERSION = getMajorVersion();

    /**
     * Maximum size of the StringBuilders used in RingBuffer LogEvents to store the contents of reusable Messages.
     * After a large message has been delivered to the appenders, the StringBuilder is trimmed to this size.
     * <p>
     * The default value is 518, which allows the StringBuilder to resize three times from its initial size.
     * Users can override with system property {@link LoggingSystemProperty#GC_REUSABLE_MESSAGE_MAX_SIZE}.
     * </p>
     * @since 2.9
     */
    public static final int MAX_REUSABLE_MESSAGE_SIZE =
            size(LoggingSystemProperty.GC_REUSABLE_MESSAGE_MAX_SIZE, (128 * 2 + 2) * 2 + 2);

    private static int size(final PropertyKey property, final int defaultValue) {
        return PropertiesUtil.getProperties().getIntegerProperty(property, defaultValue);
    }

    /**
     * The empty array.
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = {};

    /**
     * The empty array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = {};

    /**
     * Prevent class instantiation.
     */
    private Constants() {}

    private static int getMajorVersion() {
        return getMajorVersion(System.getProperty("java.version"));
    }

    static int getMajorVersion(final String version) {
        // Split into `major.minor.rest`
        final String[] parts = version.split("-|\\.", 3);
        boolean isJEP223;
        try {
            final int token = Integer.parseInt(parts[0]);
            isJEP223 = token != 1;
            if (isJEP223) {
                return token;
            }
            return Integer.parseInt(parts[1]);
        } catch (final Exception ex) {
            return 0;
        }
    }
}
