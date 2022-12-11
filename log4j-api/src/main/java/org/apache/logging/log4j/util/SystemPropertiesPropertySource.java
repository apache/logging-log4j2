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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * PropertySource backed by the current system properties. Other than having a
 * higher priority over normal properties, this follows the same rules as
 * {@link PropertiesPropertySource}.
 *
 * @since 2.10.0
 */
public class SystemPropertiesPropertySource implements PropertySource {

    private static final int DEFAULT_PRIORITY = 0;
    private static final String PREFIX = "log4j2.";

    private static final PrivilegedAction<Properties> GET_SYSTEM_PROPERTIES = System::getProperties;

    private static PrivilegedAction<String> getSystemPropertyAction(final String key) {
        return () -> System.getProperty(key);
    }

    private static PrivilegedAction<String> getSystemPropertyAction(final String key, final String defaultKey) {
        return () -> System.getProperty(key, defaultKey);
    }

    /**
     * Used by bootstrap code to get system properties without loading PropertiesUtil.
     */
    public static String getSystemProperty(final String key, final String defaultValue) {
        try {
            return System.getSecurityManager() == null ?
                    System.getProperty(key, defaultValue) :
                    AccessController.doPrivileged(getSystemPropertyAction(key, defaultValue));
        } catch (SecurityException ignored) {
            // Silently ignore the exception
            return defaultValue;
        }
    }

    private final SystemProperties systemProperties = new DefaultSystemProperties();
    private final SystemProperties securedProperties = new AccessControlledSystemProperties();

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public void forEach(final BiConsumer<String, String> action) {
        final Properties properties = getSystemProperties().getProperties();
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
        return getSystemProperties().getProperties().stringPropertyNames();
    }

    @Override
    public String getProperty(String key) {
        return getSystemProperties().getProperty(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return getProperty(key) != null;
    }

    private SystemProperties getSystemProperties() {
        return System.getSecurityManager() == null ? systemProperties : securedProperties;
    }

    private interface SystemProperties {
        Properties getProperties();

        String getProperty(String key);

        String getProperty(String key, String defaultValue);
    }

    private static class DefaultSystemProperties implements SystemProperties {
        @Override
        public Properties getProperties() {
            try {
                return System.getProperties();
            } catch (final SecurityException ignored) {
                return new Properties();
            }
        }

        @Override
        public String getProperty(final String key) {
            try {
                return System.getProperty(key);
            } catch (final SecurityException ignored) {
                return null;
            }
        }

        @Override
        public String getProperty(final String key, final String defaultValue) {
            try {
                return System.getProperty(key, defaultValue);
            } catch (final SecurityException ignored) {
                return defaultValue;
            }
        }
    }

    private static class AccessControlledSystemProperties implements SystemProperties {
        @Override
        public Properties getProperties() {
            try {
                return AccessController.doPrivileged(GET_SYSTEM_PROPERTIES);
            } catch (final SecurityException ignored) {
                // (1) There is no status logger.
                // (2) LowLevelLogUtil also consults system properties ("line.separator") to
                // open a BufferedWriter, so this may fail as well. Just having a hard reference
                // in this code to LowLevelLogUtil would cause a problem.
                // (3) We could log to System.err (nah) or just be quiet as we do now.
                return new Properties();
            }
        }

        @Override
        public String getProperty(final String key) {
            try {
                return AccessController.doPrivileged(getSystemPropertyAction(key));
            } catch (final SecurityException ignored) {
                return null;
            }
        }

        @Override
        public String getProperty(final String key, final String defaultValue) {
            try {
                return AccessController.doPrivileged(getSystemPropertyAction(key, defaultValue));
            } catch (final SecurityException ignored) {
                return defaultValue;
            }
        }
    }

}
