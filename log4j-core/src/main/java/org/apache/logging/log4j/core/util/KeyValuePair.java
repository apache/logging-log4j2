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

package org.apache.logging.log4j.core.util;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * Key/Value pair configuration item.
 *
 * @since 2.1 implements {@link Serializable}
 * @since 2.1 implements {@link #hashCode()} and {@link #equals(Object)}
 */
@Plugin(name = "KeyValuePair", category = Node.CATEGORY, printObject = true)
public final class KeyValuePair implements Serializable {

    private static final long serialVersionUID = 4331228262821046866L;

    private final String key;
    private final String value;

    /**
     * Constructs a key/value pair. The constructor should only be called from test classes.
     * @param key The key.
     * @param value The value.
     */
    public KeyValuePair(final String key, final String value) {
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

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    protected Object writeReplace() throws ObjectStreamException {
        return newBuilder().setKey(this.key).setValue(this.value);
    }

    private void readObject(final ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Builder proxy required");
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<KeyValuePair>, Serializable {

        private static final long serialVersionUID = 1L;

        @PluginBuilderAttribute
        private String key;

        @PluginBuilderAttribute
        private String value;

        public Builder setKey(final String key) {
            this.key = key;
            return this;
        }

        public Builder setValue(final String value) {
            this.value = value;
            return this;
        }

        @Override
        public KeyValuePair build() {
            return new KeyValuePair(key, value);
        }

        protected Object readResolve() throws ObjectStreamException {
            return new KeyValuePair(key, value);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final KeyValuePair other = (KeyValuePair) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
}
