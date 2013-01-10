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
package org.apache.logging.log4j.core.helpers;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.Properties;

/**
 * System Property utility methods.
 */
public final class PropertiesUtil {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private PropertiesUtil() {
    }

    /**
     * Return the specified system property, returning null if an error occurs.
     * @param key The key to locate.
     * @return The value associated with the key.
     */
    public static String getSystemProperty(final String key) {
        try {
            return System.getProperty(key);
        } catch (final SecurityException ex) {
            LOGGER.error("Unable to access system property {} due to security restrictions. Defaulting to null",
                key);
            return null;
        }
    }

    /**
     * Return the specified system property, returning the provided default value if the key does not exist or
     * an error occurs.
     * @param key The key to locate.
     * @param defaultValue The default value.
     * @return The value associated with the key or the default value if the key cannot be located.
     */
    public static String getSystemProperty(final String key, final String defaultValue) {
        try {
            final String value = System.getProperty(key);
            return value == null ? defaultValue : value;
        } catch (final SecurityException ex) {
            LOGGER.warn("Unable to access system property {} due to security restrictions. Defaulting to {}",
                key, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Return the system properties or an empty Properties object if an error occurs.
     * @return The system properties.
     */
    public static Properties getSystemProperties() {
        try {
            return new Properties(System.getProperties());
        } catch (final SecurityException ex) {
            LOGGER.error("Unable to access system properties.");
            // Sandboxed - can't read System Properties
            return new Properties();
        }
    }
}
