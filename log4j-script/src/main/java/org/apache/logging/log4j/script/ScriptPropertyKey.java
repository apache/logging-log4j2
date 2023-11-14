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
package org.apache.logging.log4j.script;

import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.spi.PropertyComponent;
import org.apache.logging.log4j.util.PropertyKey;

/**
 * Properties used by the JUL support.
 */
public enum ScriptPropertyKey implements PropertyKey {

    SCRIPT_ENABLE_LANGUAGES(PropertyComponent.SCRIPT, ScriptPropertyKey.Constant.ENABLE_LANGUAGES);

    private final PropertyComponent component;
    private final String name;
    private final String key;
    private final String systemKey;

    ScriptPropertyKey(final PropertyComponent component, final String name) {
        this.component = component;
        this.name = name;
        this.key = component.getName() + "." + name;
        this.systemKey = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + key;
    }

    public static PropertyKey findKey(final String component, final String name) {
        for (PropertyKey key : values()) {
            if (key.getComponent().equals(component) && key.getName().equals(name)) {
                return key;
            }
        }
        return null;
    }

    public String getComponent() {
        return component.getName();
    }

    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getSystemKey() {
        return systemKey;
    }

    public String toString() {
        return getKey();
    }

    public static class Constant {
        private static final String DELIM = ".";
        static final String ENABLE_LANGUAGES = "enableLanguages";
        public static final String SCRIPT_ENABLE_LANGUAGES = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX
                + PropertyComponent.Constant.SCRIPT + DELIM + ENABLE_LANGUAGES;
    }

}
