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
package org.apache.logging.log4j.core.layout;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

/**
 * PatternMatch configuration item.
 *
 * @since 2.4.1 implements {@link Serializable}
 */
@Plugin(name = "PatternMatch", category = Node.CATEGORY, printObject = true)
public final class PatternMatch {

    private final String key;
    private final String pattern;

    /**
     * Constructs a key/value pair. The constructor should only be called from test classes.
     * @param key The key.
     * @param pattern The value.
     */
    public PatternMatch(final String key, final String pattern) {
        this.key = key;
        this.pattern = pattern;
    }

    /**
     * Returns the key.
     * @return the key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the pattern.
     * @return The pattern.
     */
    public String getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return key + '=' + pattern;
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<PatternMatch>, Serializable {

        private static final long serialVersionUID = 1L;

        @PluginBuilderAttribute
        private String key;

        @PluginBuilderAttribute
        private String pattern;

        public Builder setKey(final String key) {
            this.key = key;
            return this;
        }

        public Builder setPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        @Override
        public PatternMatch build() {
            return new PatternMatch(key, pattern);
        }

        protected Object readResolve() throws ObjectStreamException {
            return new PatternMatch(key, pattern);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, pattern);
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
        final PatternMatch other = (PatternMatch) obj;
        if (!Objects.equals(key, other.key)) {
            return false;
        }
        if (!Objects.equals(pattern, other.pattern)) {
            return false;
        }
        return true;
    }
}
