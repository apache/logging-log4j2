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
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * Key/Value pair configuration item.
 *
 * <p>Compatibility facade delegating storage to {@link org.apache.logging.log4j.common.util.KeyValuePair}.</p>
 *
 * @since 2.1 implements {@link #hashCode()} and {@link #equals(Object)}
 */
@Plugin(name = "KeyValuePair", category = Node.CATEGORY, printObject = true)
public final class KeyValuePair {

    /**
     * The empty array.
     */
    public static final KeyValuePair[] EMPTY_ARRAY = {};

    private final org.apache.logging.log4j.common.util.KeyValuePair delegate;

    /**
     * Constructs a key/value pair. The constructor should only be called from test classes.
     * @param key The key.
     * @param value The value.
     */
    public KeyValuePair(final String key, final String value) {
        this.delegate = new org.apache.logging.log4j.common.util.KeyValuePair(key, value);
    }

    private KeyValuePair(final org.apache.logging.log4j.common.util.KeyValuePair delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns the shared {@link org.apache.logging.log4j.common.util.KeyValuePair} delegate.
     *
     * @return the common key/value pair delegate
     * @since 3.0.0
     */
    public org.apache.logging.log4j.common.util.KeyValuePair asCommonKeyValuePair() {
        return delegate;
    }

    /**
     * Creates a core {@code KeyValuePair} from a {@link org.apache.logging.log4j.common.util.KeyValuePair}.
     *
     * @param keyValuePair the common key/value pair
     * @return a core key/value pair facade
     * @since 3.0.0
     */
    public static KeyValuePair fromCommon(final org.apache.logging.log4j.common.util.KeyValuePair keyValuePair) {
        return new KeyValuePair(Objects.requireNonNull(keyValuePair, "keyValuePair"));
    }

    /**
     * Returns the key.
     * @return the key.
     */
    public String getKey() {
        return delegate.getKey();
    }

    /**
     * Returns the value.
     * @return The value.
     */
    public String getValue() {
        return delegate.getValue();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<KeyValuePair> {

        @PluginBuilderAttribute
        private String key;

        @PluginBuilderAttribute
        private String value;

        public Builder setKey(final String aKey) {
            this.key = aKey;
            return this;
        }

        public Builder setValue(final String aValue) {
            this.value = aValue;
            return this;
        }

        @Override
        public KeyValuePair build() {
            return new KeyValuePair(key, value);
        }
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof KeyValuePair) {
            return delegate.equals(((KeyValuePair) obj).delegate);
        }
        if (obj instanceof org.apache.logging.log4j.common.util.KeyValuePair) {
            return delegate.equals(obj);
        }
        return false;
    }
}
