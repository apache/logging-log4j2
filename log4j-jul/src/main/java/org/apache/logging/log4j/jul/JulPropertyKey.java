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
package org.apache.logging.log4j.jul;

import org.apache.logging.log4j.spi.LoggingSystemProperty;
import org.apache.logging.log4j.spi.PropertyComponent;
import org.apache.logging.log4j.util.PropertyKey;

/**
 * Properties used by the JUL support.
 */
public enum JulPropertyKey implements PropertyKey {

    LEVEL_CONVERTER(PropertyComponent.JUL, "levelConverter"),
    LOGGER_ADAPTER(PropertyComponent.JUL, "loggerAdpater");

    private final PropertyComponent component;
    private final String name;
    private final String key;
    private final String systemKey;

    JulPropertyKey(final PropertyComponent component, String name) {
        this.component = component;
        this.name = name;
        this.key = component.getName() + "." + name;
        this.systemKey = LoggingSystemProperty.SYSTEM_PROPERTY_PREFIX + key;
    }

    public static PropertyKey findKey(String component, String name) {
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



}
