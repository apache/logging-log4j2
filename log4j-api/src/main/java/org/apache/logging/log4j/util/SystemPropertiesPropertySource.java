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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PropertySource backed by the current system properties. Other than having a
 * higher priority over normal properties, this follows the same rules as
 * {@link PropertiesPropertySource}.
 *
 * @since 2.10.0
 */
public class SystemPropertiesPropertySource extends ContextAwarePropertySource implements PropertySource {

    private static final int DEFAULT_PRIORITY = 0;
    private static final String PREFIX = "log4j2.";

    private static final String DELIM = ".";

    private static volatile int hashcode = 0;

    private static final Map<String, Properties> systemPropertiesMap = new ConcurrentHashMap<>();

    public SystemPropertiesPropertySource() {
        super(null, SYSTEM_CONTEXT, true);
    }

    /**
     * Used by bootstrap code to get system properties without loading PropertiesUtil.
     */
    public static String getSystemProperty(final String key, final String defaultValue) {
        try {
            return System.getProperty(key, defaultValue);
        } catch (SecurityException e) {
            // Silently ignore the exception
            return defaultValue;
        }
    }

    @Override
    public int getPriority() {
        return DEFAULT_PRIORITY;
    }

    @Override
    public void forEach(final BiConsumer<String, String> action) {
        if (!refreshProperties()) {
            return;
        }
        final Properties properties = systemPropertiesMap.get(SYSTEM_CONTEXT);
        // Then traverse for an unknown amount of time.
        // Some keys may now be absent, in which case, the value is null.
        for (final Object key : properties.stringPropertyNames()) {
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
        return getPropertyNames(SYSTEM_CONTEXT);
    }

    @Override
    public Collection<String> getPropertyNames(final String contextName) {
        refreshProperties();
        final Properties properties = propertiesMap.get(contextName);
        return properties != null ? properties.stringPropertyNames() : Collections.emptyList();
    }

    @Override
    public String getProperty(final String contextName, final String key) {
        if (contextName != null && !contextName.equals(SYSTEM_CONTEXT)) {
            return getSystemProperty(PREFIX + contextName + DELIM + key, null);
        } else {
            String result = getSystemProperty(PREFIX + SYSTEM_CONTEXT + DELIM + key, null);
            if (result == null) {
                result = getSystemProperty(key, null);
            }
            return result;
        }
    }

    @Override
    public boolean containsProperty(final String contextName, final String key) {
        if (contextName != null && !contextName.equals(SYSTEM_CONTEXT)) {
            return getSystemProperty(PREFIX + contextName + DELIM + key, null) != null;
        } else {
            return getSystemProperty(PREFIX + SYSTEM_CONTEXT + DELIM + key, null) != null
                    || getSystemProperty(key, null) != null;
        }
    }

    @Override
    public Map<String, Properties> getPropertiesMap() {
        return systemPropertiesMap;
    }

    private boolean refreshProperties() {
        boolean refresh = false;
        Properties sysProps;
        /**
         * Copy the properties while locked.
         */
        synchronized (PREFIX) {
            final Properties props = getProperties();
            if (props == null) {
                return false;
            }
            sysProps = new Properties();
            sysProps.putAll(props);
        }
        if (sysProps == null) {
            return false;
        }
        if (hashcode == 0) {
            refresh = true;
            hashcode = sysProps.hashCode();
        } else {
            final int hash = sysProps.hashCode();
            if (hash != hashcode) {
                refresh = true;
                hashcode = hash;
            }
        }
        if (refresh) {
            final Map<String, Properties> map = parseProperties(sysProps, SYSTEM_CONTEXT, true);
            systemPropertiesMap.putAll(map);
        }
        if (refresh) {
            PropertiesUtil.getProperties().reload();
        }
        return true;
    }

    private static Properties getProperties() {
        try {
            return System.getProperties();
        } catch (final SecurityException e) {
            return null;
        }
    }

}
