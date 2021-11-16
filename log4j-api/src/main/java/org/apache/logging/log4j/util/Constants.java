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

/**
 * Log4j API Constants.
 *
 * @since 2.6.2
 */
public final class Constants {
    /**
     * {@code true} if we think we are running in a web container, based on the boolean value of system property
     * "log4j2.is.webapp", or (if this system property is not set) whether the  {@code javax.servlet.Servlet} class
     * is present in the classpath.
     */
    public static final boolean IS_WEB_APP = PropertiesUtil.getProperties().getBooleanProperty(
            "log4j2.is.webapp", isClassAvailable("javax.servlet.Servlet")
                    || isClassAvailable("jakarta.servlet.Servlet"));

    /**
     * Kill switch for object pooling in ThreadLocals that enables much of the LOG4J2-1270 no-GC behaviour.
     * <p>
     * {@code True} for non-{@link #IS_WEB_APP web apps}, disable by setting system property
     * "log4j2.enable.threadlocals" to "false".
     * </p>
     */
    public static final boolean ENABLE_THREADLOCALS = !IS_WEB_APP && PropertiesUtil.getProperties().getBooleanProperty(
            "log4j2.enable.threadlocals", true);

    public static final int JAVA_MAJOR_VERSION = getMajorVersion();

    /**
     * Maximum size of the StringBuilders used in RingBuffer LogEvents to store the contents of reusable Messages.
     * After a large message has been delivered to the appenders, the StringBuilder is trimmed to this size.
     * <p>
     * The default value is {@value}, which allows the StringBuilder to resize three times from its initial size.
     * Users can override with system property "log4j.maxReusableMsgSize".
     * </p>
     * @since 2.9
     */
    public static final int MAX_REUSABLE_MESSAGE_SIZE = size("log4j.maxReusableMsgSize", (128 * 2 + 2) * 2 + 2);

    /**
     * Name of the system property that will turn on TRACE level internal log4j2 status logging.
     * <p>
     * If system property {@value} is either defined empty or its value equals to {@code true} (ignoring case), all internal log4j2 logging will be
     * printed to the console. The presence of this system property overrides any value set in the configuration's
     * {@code <Configuration status="<level>" ...>} status attribute, as well as any value set for
     * system property {@code org.apache.logging.log4j.simplelog.StatusLogger.level}.
     * </p>
     */
    public static final String LOG4J2_DEBUG = "log4j2.debug";

    private static int size(final String property, final int defaultValue) {
        return PropertiesUtil.getProperties().getIntegerProperty(property, defaultValue);
    }

    /**
     * Determines if a named Class can be loaded or not.
     *
     * @param className The class name.
     * @return {@code true} if the class could be found or {@code false} otherwise.
     */
    private static boolean isClassAvailable(final String className) {
        try {
            return LoaderUtil.loadClass(className) != null;
        } catch (final Throwable e) {
            return false;
        }
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
    private Constants() {
    }

    private static int getMajorVersion() {
        return getMajorVersion(System.getProperty("java.version"));
    }

    static int getMajorVersion(final String version) {
        final String[] parts = version.split("-|\\.");
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
