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
package org.apache.log4j.builders;

import static org.apache.log4j.xml.XmlConfiguration.NAME_ATTR;
import static org.apache.log4j.xml.XmlConfiguration.VALUE_ATTR;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.w3c.dom.Element;

/**
 * Base class for Log4j 1 component builders.
 *
 * @param <T> The type to build.
 */
public abstract class AbstractBuilder<T> implements Builder<T> {

    private static final Logger LOGGER = StatusLogger.getLogger();
    protected static final String FILE_PARAM = "File";
    protected static final String APPEND_PARAM = "Append";
    protected static final String BUFFERED_IO_PARAM = "BufferedIO";
    protected static final String BUFFER_SIZE_PARAM = "BufferSize";
    protected static final String IMMEDIATE_FLUSH_PARAM = "ImmediateFlush";
    protected static final String MAX_SIZE_PARAM = "MaxFileSize";
    protected static final String MAX_BACKUP_INDEX = "MaxBackupIndex";
    protected static final String RELATIVE = "RELATIVE";
    protected static final String NULL = "NULL";

    private final String prefix;
    private final Properties properties;

    public AbstractBuilder() {
        this(null, new Properties());
    }

    public AbstractBuilder(final String prefix, final Properties props) {
        this.prefix = prefix != null ? prefix + "." : null;
        this.properties = (Properties) props.clone();
        final Map<String, String> map = new HashMap<>();
        System.getProperties().forEach((k, v) -> map.put(k.toString(), v.toString()));
        props.forEach((k, v) -> map.put(k.toString(), v.toString()));
        // normalize keys to lower case for case-insensitive access.
        props.forEach((k, v) -> map.put(toBeanKey(k.toString()), v.toString()));
        props.entrySet().forEach(e -> this.properties.put(toBeanKey(e.getKey().toString()), e.getValue()));
    }

    protected static org.apache.logging.log4j.core.Filter buildFilters(final String level, final Filter filter) {
        Filter head = null;
        if (level != null) {
            final org.apache.logging.log4j.core.Filter thresholdFilter = ThresholdFilter.createFilter(
                    OptionConverter.convertLevel(level, Level.TRACE),
                    org.apache.logging.log4j.core.Filter.Result.NEUTRAL,
                    org.apache.logging.log4j.core.Filter.Result.DENY);
            head = new FilterWrapper(thresholdFilter);
        }
        if (filter != null) {
            head = FilterAdapter.addFilter(head, filter);
        }
        return FilterAdapter.adapt(head);
    }

    private String capitalize(final String value) {
        if (Strings.isEmpty(value) || Character.isUpperCase(value.charAt(0))) {
            return value;
        }
        final char[] chars = value.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public boolean getBooleanProperty(final String key, final boolean defaultValue) {
        return Boolean.parseBoolean(getProperty(key, Boolean.toString(defaultValue)));
    }

    public boolean getBooleanProperty(final String key) {
        return getBooleanProperty(key, false);
    }

    protected boolean getBooleanValueAttribute(final Element element) {
        return Boolean.parseBoolean(getValueAttribute(element));
    }

    public int getIntegerProperty(final String key, final int defaultValue) {
        String value = null;
        try {
            value = getProperty(key);
            if (value != null) {
                return Integer.parseInt(value);
            }
        } catch (final Exception ex) {
            LOGGER.warn("Error converting value {} of {} to an integer: {}", value, key, ex.getMessage());
        }
        return defaultValue;
    }

    public long getLongProperty(final String key, final long defaultValue) {
        String value = null;
        try {
            value = getProperty(key);
            if (value != null) {
                return Long.parseLong(value);
            }
        } catch (final Exception ex) {
            LOGGER.warn("Error converting value {} of {} to a long: {}", value, key, ex.getMessage());
        }
        return defaultValue;
    }

    protected String getNameAttribute(final Element element) {
        return element.getAttribute(NAME_ATTR);
    }

    protected String getNameAttributeKey(final Element element) {
        return toBeanKey(element.getAttribute(NAME_ATTR));
    }

    public Properties getProperties() {
        return properties;
    }

    public String getProperty(final String key) {
        return getProperty(key, null);
    }

    public String getProperty(final String key, final String defaultValue) {
        String value = properties.getProperty(prefix + toJavaKey(key));
        value = value != null ? value : properties.getProperty(prefix + toBeanKey(key), defaultValue);
        value = value != null ? substVars(value) : defaultValue;
        return value != null ? value.trim() : defaultValue;
    }

    protected String getValueAttribute(final Element element) {
        return getValueAttribute(element, null);
    }

    protected String getValueAttribute(final Element element, final String defaultValue) {
        final String attribute = element.getAttribute(VALUE_ATTR);
        return substVars(attribute != null ? attribute.trim() : defaultValue);
    }

    protected String substVars(final String value) {
        return OptionConverter.substVars(value, properties);
    }

    String toBeanKey(final String value) {
        return capitalize(value);
    }

    String toJavaKey(final String value) {
        return uncapitalize(value);
    }

    private String uncapitalize(final String value) {
        if (Strings.isEmpty(value) || Character.isLowerCase(value.charAt(0))) {
            return value;
        }
        final char[] chars = value.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    protected void set(final String name, final Element element, final AtomicBoolean ref) {
        final String value = getValueAttribute(element);
        if (value == null) {
            LOGGER.warn("No value for {} parameter, using default {}", name, ref);
        } else {
            ref.set(Boolean.parseBoolean(value));
        }
    }

    protected void set(final String name, final Element element, final AtomicInteger ref) {
        final String value = getValueAttribute(element);
        if (value == null) {
            LOGGER.warn("No value for {} parameter, using default {}", name, ref);
        } else {
            try {
                ref.set(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                LOGGER.warn(
                        "{} parsing {} parameter, using default {}: {}",
                        e.getClass().getName(),
                        name,
                        ref,
                        e.getMessage(),
                        e);
            }
        }
    }

    protected void set(final String name, final Element element, final AtomicLong ref) {
        final String value = getValueAttribute(element);
        if (value == null) {
            LOGGER.warn("No value for {} parameter, using default {}", name, ref);
        } else {
            try {
                ref.set(Long.parseLong(value));
            } catch (NumberFormatException e) {
                LOGGER.warn(
                        "{} parsing {} parameter, using default {}: {}",
                        e.getClass().getName(),
                        name,
                        ref,
                        e.getMessage(),
                        e);
            }
        }
    }

    protected void set(final String name, final Element element, final AtomicReference<String> ref) {
        final String value = getValueAttribute(element);
        if (value == null) {
            LOGGER.warn("No value for {} parameter, using default {}", name, ref);
        } else {
            ref.set(value);
        }
    }
}
