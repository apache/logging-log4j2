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
package org.apache.logging.log4j.core.util;

import java.util.Objects;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;

/**
 * Key/Value pair configuration item.
 *
 * @since 2.1 implements {@link #hashCode()} and {@link #equals(Object)}
 */
@Configurable(printObject = true)
@Plugin
public final class KeyValuePair {

    private final String key;
    private final String value;

    /**
     * Constructs a key/value pair.
     * @param key The key.
     * @param value The value.
     */
    @Inject
    public KeyValuePair(@PluginAttribute final String key, @PluginAttribute final String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the key.
     * @return the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the value.
     * @return The value.
     */
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return key + '=' + value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final KeyValuePair that = (KeyValuePair) o;
        return Objects.equals(key, that.key) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
