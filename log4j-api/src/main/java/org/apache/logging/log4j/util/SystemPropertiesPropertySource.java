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

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceProvider;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PropertySource backed by the current system properties. Other than having a
 * higher priority over normal properties, this follows the same rules as
 * {@link PropertiesPropertySource}.
 *
 * @since 2.10.0
 */
@ServiceProvider(value = PropertySource.class, resolution = Resolution.OPTIONAL)
public class SystemPropertiesPropertySource implements PropertySource {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int DEFAULT_PRIORITY = 0;
    private static final String PREFIX = "log4j2.";

    private static final PropertySource INSTANCE = new SystemPropertiesPropertySource();

    /**
     * Method used by Java 9+ to instantiate providers
     * @since 2.24.0
     * @see java.util.ServiceLoader
     */
    public static PropertySource provider() {
        return INSTANCE;
    }

    public static String getSystemProperty(final String key, final String defaultValue) {
        final String value = INSTANCE.getProperty(key);
        return value != null ? value : defaultValue;
    }

    private static void logException(final SecurityException error) {
        LOGGER.error("The Java system properties are not available to Log4j due to security restrictions.", error);
    }

    private static void logException(final SecurityException error, final String key) {
        LOGGER.error("The Java system property {} is not available to Log4j due to security restrictions.", key, error);
    }

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public void forEach(final BiConsumer<String, String> action) {
        final Properties properties;
        try {
            properties = System.getProperties();
        } catch (final SecurityException e) {
            logException(e);
            return;
        }
        // Lock properties only long enough to get a thread-safe SAFE snapshot of its
        // current keys, an array.
        final Object[] keySet;
        synchronized (properties) {
            keySet = properties.keySet().toArray();
        }
        // Then traverse for an unknown amount of time.
        // Some keys may now be absent, in which case, the value is null.
        for (final Object key : keySet) {
            final String keyStr = Objects.toString(key, null);
            action.accept(keyStr, properties.getProperty(keyStr));
        }
    }

    @Override
    public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        return PREFIX + Util.joinAsCamelCase(tokens);
    }

    @Override
    public Collection<String> getPropertyNames() {
        try {
            return System.getProperties().stringPropertyNames();
        } catch (final SecurityException e) {
            logException(e);
        }
        return PropertySource.super.getPropertyNames();
    }

    @Override
    public String getProperty(final String key) {
        try {
            return System.getProperty(key);
        } catch (final SecurityException e) {
            logException(e, key);
        }
        return PropertySource.super.getProperty(key);
    }

    @Override
    public boolean containsProperty(final String key) {
        return getProperty(key) != null;
    }
}
