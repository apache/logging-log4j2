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

import org.apache.logging.log4j.spi.Provider;

/**
 * Log4j API Constants.
 *
 * @since 2.6.2
 */
public final class Constants {
    /**
     * Specifies whether Log4j is used in a servlet container
     * <p>
     *     If {@code true} Log4j disables the features, which are incompatible with a typical servlet application:
     * </p>
     * <ol>
     *     <li>It disables the usage of {@link ThreadLocal}s for object pooling (unless the user has explicitly provided a {@link #ENABLE_THREADLOCALS} property)</li>
     *     <li>It uses a web-application safe implementation of {@link org.apache.logging.log4j.spi.ThreadContextMap}
     *     (see {@link Provider#getThreadContextMap()}),</li>
     *     <li>It disables the shutdown hook,</li>
     *     <li>It uses the caller thread to send JMX notifications.</li>
     * </ol>
     * <p>
     *     The value of this constant depends upon the presence of the Servlet API on the classpath and can be
     *     overridden using the {@code "log4j2.isWebapp"} system property.
     * </p>
     */
    public static final boolean IS_WEB_APP = PropertiesUtil.getProperties()
            .getBooleanProperty(
                    "log4j2.is.webapp",
                    isClassAvailable("javax.servlet.Servlet") || isClassAvailable("jakarta.servlet.Servlet"));

    /**
     * Specifies whether Log4j can bind non-JRE types to {@link ThreadLocal}s
     * <p>
     *     The value of this constant is {@code true}, unless Log4j is running in a servlet container (cf.
     *     {@link #IS_WEB_APP}). Use the {@code "log4j2.enableThreadlocals} system property to override its value.
     * </p>
     * <p>
     *     In order to enable the garbage-free behavior described in
     *     <a href="https://issues.apache.org/jira/browse/LOG4J2-1270">LOG4J2-1270</a>, this constant must be {@code
     *     true}.
     * </p>
     * <p>
     *     <strong>Warning:</strong> This setting does <strong>not</strong> disable all thread locals. It only
     *     disables those thread locals that can cause a classloader memory leak.
     * </p>
     */
    public static final boolean ENABLE_THREADLOCALS =
            PropertiesUtil.getProperties().getBooleanProperty("log4j2.enable.threadlocals", !IS_WEB_APP);

    /**
     * Java major version.
     */
    public static final int JAVA_MAJOR_VERSION = getMajorVersion();

    /**
     * Maximum size of the StringBuilders used in RingBuffer LogEvents to store the contents of reusable Messages.
     * After a large message has been delivered to the appenders, the StringBuilder is trimmed to this size.
     * <p>
     * The default value is 518, which allows the StringBuilder to resize three times from its initial size.
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
