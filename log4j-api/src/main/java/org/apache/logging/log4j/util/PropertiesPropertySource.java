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
import java.util.Map;
import java.util.Properties;

/**
 * PropertySource backed by a {@link Properties} instance. Normalized property names follow a scheme like this:
 * {@code Log4jContextSelector} would normalize to {@code log4j2.contextSelector}.
 *
 * @since 2.10.0
 */
public class PropertiesPropertySource implements PropertySource {

    private static final int DEFAULT_PRIORITY = 200;
    private static final String PREFIX = "log4j2.";

    private final Properties properties;
    private final int priority;

    public PropertiesPropertySource(final Properties properties) {
        this(properties, DEFAULT_PRIORITY);
    }

    public PropertiesPropertySource(final Properties properties, final int priority) {
        this.properties = properties;
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void forEach(final BiConsumer<String, String> action) {
        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            action.accept(((String) entry.getKey()), ((String) entry.getValue()));
        }
    }

    @Override
    public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
        final CharSequence camelCase = Util.joinAsCamelCase(tokens);
        return camelCase.length() > 0 ? PREFIX + camelCase : null;
    }

    @Override
    public Collection<String> getPropertyNames() {
        return properties.stringPropertyNames();
    }

    @Override
    public String getProperty(final String key) {
        return properties.getProperty(key);
    }

    @Override
    public boolean containsProperty(final String key) {
        return getProperty(key) != null;
    }
}
