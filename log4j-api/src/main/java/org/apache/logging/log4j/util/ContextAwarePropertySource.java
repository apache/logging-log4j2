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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.spi.PropertyComponent;

/**
 * The original version of PropertySource did not support context aware PropertySources, so a call
 * to getProperty(contextName, key) must resolve to getProperty(key). However, when a PropertySource
 * is context aware it is desirable to have getProperty(key) resolve to getProperty(SYSTEM_CONTEXT, key).
 */
public abstract class ContextAwarePropertySource implements PropertySource {

    protected final Map<String, Properties> propertiesMap;
    private final String contextName;
    private boolean includeInvalid;

    public ContextAwarePropertySource(final Properties properties, final String contextName,
                                      final boolean includeInvalid) {
        if (properties != null) {
            this.propertiesMap = parseProperties(properties, contextName, includeInvalid);
        } else {
            propertiesMap = new ConcurrentHashMap<>();
        }
        this.contextName = contextName;
        this.includeInvalid = includeInvalid;
    }

    /**
     * Used only for System Environment properties.
     * @param properties The map from the Environment.
     */
    public ContextAwarePropertySource(final Map<String, String> properties) {
        this.propertiesMap = parseProperties(properties);
        this.contextName = SYSTEM_CONTEXT;
        this.includeInvalid = false;
    }

    @Override
    public Collection<String> getPropertyNames() {
        return getPropertyNames(SYSTEM_CONTEXT);
    }

    public Collection<String> getPropertyNames(final String contextName) {
        Properties properties = getPropertiesMap().get(contextName);
        return properties != null ? properties.stringPropertyNames() : Collections.emptyList();
    }

    @Override
    public String getProperty(final String key) {
        return getProperty(SYSTEM_CONTEXT, key);
    }

    public String getProperty(final String contextName, final String key) {
        Properties properties = getPropertiesMap().get(contextName);
        String value = properties != null ? properties.getProperty(key) : null;
        if (Strings.isEmpty(value)) {
            return null;
        }
        return value;
    }

    @Override
    public boolean containsProperty(final String key) {
        return containsProperty(SYSTEM_CONTEXT, key);
    }

    public boolean containsProperty(final String contextName, final String key) {
        Map<String, Properties> propertiesMap = getPropertiesMap();
        Properties properties = propertiesMap.get(contextName);
        if (properties != null) {
            String value = properties.getProperty(key);
            return Strings.isNotEmpty(value);
        }
        return false;
    }

    public Map<String, Properties> getPropertiesMap() {
        return propertiesMap;
    }

    /**
     * Used to parse environment variables.
     * @param properties The map of properties.
     * @return The Properties Map.
     */
    protected Map<String, Properties> parseProperties(final Map<String, String> properties) {
        Map<String, Properties> propertiesMap = new ConcurrentHashMap<>();
        for (String propName : properties.keySet()) {
            if (propName.startsWith(PREFIX)) {
                storeProperty(propertiesMap, propName, properties.get(propName));
            }
        }
        return propertiesMap;
    }

    protected Map<String, Properties> parseProperties(Properties properties) {
        return parseProperties(properties, contextName, includeInvalid);
    }

    /**
     * Used to parse properties in Properties objects.
     * @param properties The input properties.
     * @param contextName The context name.
     * @param includeInvalid stores properties that do not match the Log4j2 convention.
     * @return The Properties Map.
     */
    protected Map<String, Properties> parseProperties(final Properties properties, final String contextName,
                                                    final boolean includeInvalid) {
        Map<String, Properties> propertiesMap = new ConcurrentHashMap<>();
        if (contextName == null || contextName.equals(SYSTEM_CONTEXT)) {
            for (String propertyName : properties.stringPropertyNames()) {
                String propName = Util.resolveKey(propertyName);
                if (propName.startsWith(PREFIX)) {
                    storeProperty(propertiesMap, propName, properties.getProperty(propertyName));
                } else if (propName.startsWith(PropertyComponent.Constant.LOG4J)
                        || propName.startsWith(PropertyComponent.Constant.LOG4J1)) {
                    Properties props = propertiesMap.get(SYSTEM_CONTEXT);
                    if (props == null) {
                        props = new Properties();
                        propertiesMap.put(SYSTEM_CONTEXT, props);
                    }
                    props.setProperty(propName, properties.getProperty(propertyName));
                } else {
                    if (includeInvalid) {
                        Properties props = propertiesMap.get(SYSTEM_CONTEXT);
                        if (props == null) {
                            props = new Properties();
                            propertiesMap.put(SYSTEM_CONTEXT, props);
                        }
                        props.setProperty(propName, properties.getProperty(propertyName));
                    } else {
                        LowLevelLogUtil.log("Key " + propName +
                                " is invalid. Log4j properties must be in the form \"log4j2.{contextName}.{componentName}.{key}\"");
                    }
                }
            }
        } else {
            for (String propName : properties.stringPropertyNames()) {
                if (!propName.startsWith(PREFIX)) {
                    Properties props = propertiesMap.get(contextName);
                    if (props == null) {
                        props = new Properties();
                        propertiesMap.put(contextName, props);
                    }
                    props.put(propName, properties.getProperty(propName));
                } else {
                    LowLevelLogUtil.log("Invalid key " + propName + " for context properties");
                }
            }
        }
        return propertiesMap;
    }

    private void storeProperty(final Map<String, Properties> propertiesMap, final String propName, final String value) {
        if (propName == null || value == null) {
            return;
        }
        List<CharSequence> tokens = Util.getTokens(propName);
        if (tokens.size() < 4) {
            LowLevelLogUtil.log("Key " + propName + " is invalid. Log4j properties must be "
                    + "in the form \"log4j2.{contextName}.{componentName}.{key}\"");
            return;
        }
        String name = tokens.get(1).toString();
        Properties props = propertiesMap.get(name);
        if (props == null) {
            props = new Properties();
            propertiesMap.put(name, props);
        }
        String key = Util.join(tokens.subList(2, tokens.size())).toString();
        props.setProperty(key, value);
    }
}
