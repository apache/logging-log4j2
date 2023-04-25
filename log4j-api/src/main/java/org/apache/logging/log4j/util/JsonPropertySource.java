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
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Allows Properties to be specified as JSON.
 */
public class JsonPropertySource implements PropertySource {
    private static final int DEFAULT_PRIORITY = 200;

    private final int priority;

    private final PropertiesPropertySource propertySource;

    public JsonPropertySource(final String json) {
        this(json, DEFAULT_PRIORITY);
    }

    public JsonPropertySource(final String json, final int priority) {
        final Map<String, Object> root = Cast.cast(JsonReader.read(json));
        Properties props = new Properties();
        populateProperties(props, "", root);
        propertySource = new PropertiesPropertySource(props);
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void forEach(BiConsumer<String, String> action) {
        propertySource.forEach(action);
    }

    @Override
    public Collection<String> getPropertyNames() {
        return propertySource.getPropertyNames();
    }

    @Override
    public CharSequence getNormalForm(Iterable<? extends CharSequence> tokens) {
        return propertySource.getNormalForm(tokens);
    }

    @Override
    public String getProperty(String key) {
        return propertySource.getProperty(key);
    }

    @Override
    public boolean containsProperty(String key) {
        return propertySource.containsProperty(key);
    }

    private void populateProperties(Properties props, String prefix, Map<String, Object> root) {
        if (!root.isEmpty()) {
            for (Map.Entry<String, Object> entry : root.entrySet()) {
                if (entry.getValue() instanceof String) {
                    props.setProperty(createKey(prefix, entry.getKey()), (String) entry.getValue());
                } else if (entry.getValue() instanceof List) {
                    final StringBuilder sb = new StringBuilder();
                    ((List<Object>) entry.getValue()).forEach((obj) -> {
                        if (sb.length() > 0) {
                            sb.append(",");
                        }
                        sb.append(obj.toString());
                    });
                    props.setProperty(createKey(prefix, entry.getKey()), sb.toString());
                } else {
                    populateProperties(props, createKey(prefix, entry.getKey()), (Map<String, Object>) entry.getValue());
                }
            }
        }
    }

    private String createKey(String prefix, String suffix) {
        if (prefix.isEmpty()) {
            return suffix;
        }
        return prefix + "." + suffix;
    }
}
