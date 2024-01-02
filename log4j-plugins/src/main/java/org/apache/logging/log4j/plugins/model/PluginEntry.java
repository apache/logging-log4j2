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
package org.apache.logging.log4j.plugins.model;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.logging.log4j.util.Strings;

/**
 * Descriptor for {@link org.apache.logging.log4j.plugins.Plugin} metadata.
 */
public record PluginEntry(
        String key,
        String className,
        String name,
        String elementType,
        boolean printable,
        boolean deferChildren,
        String namespace)
        implements Comparable<PluginEntry> {

    @Override
    public int compareTo(final PluginEntry o) {
        final int namespaceComparison = namespace.compareToIgnoreCase(o.namespace);
        if (namespaceComparison != 0) {
            return namespaceComparison;
        }
        return name.compareToIgnoreCase(o.name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements Supplier<PluginEntry> {
        private String key;
        private String className;
        private String name;
        private String elementType = Strings.EMPTY;
        private boolean printable;
        private boolean deferChildren;
        private String namespace = Strings.EMPTY;

        public String getKey() {
            if (key == null) {
                var name = this.name;
                if (name != null) {
                    key = name.toLowerCase(Locale.ROOT);
                }
            }
            return key;
        }

        public Builder setKey(final String key) {
            this.key = key;
            return this;
        }

        public String getClassName() {
            return className;
        }

        public Builder setClassName(final String className) {
            this.className = className;
            return this;
        }

        public String getName() {
            return name;
        }

        public Builder setName(final String name) {
            this.name = name;
            return this;
        }

        public String getElementType() {
            if (elementType == null) {
                elementType = Strings.EMPTY;
            }
            return elementType;
        }

        public Builder setElementType(final String elementType) {
            this.elementType = elementType;
            return this;
        }

        public boolean isPrintable() {
            return printable;
        }

        public Builder setPrintable(final boolean printable) {
            this.printable = printable;
            return this;
        }

        public boolean isDeferChildren() {
            return deferChildren;
        }

        public Builder setDeferChildren(final boolean deferChildren) {
            this.deferChildren = deferChildren;
            return this;
        }

        public String getNamespace() {
            if (namespace == null) {
                namespace = Strings.EMPTY;
            }
            return namespace;
        }

        public Builder setNamespace(final String namespace) {
            this.namespace = namespace;
            return this;
        }

        @Override
        public PluginEntry get() {
            final var key = Objects.requireNonNull(getKey(), () -> "key is null from " + this);
            final var className = Objects.requireNonNull(getClassName(), () -> "className is null from " + this);
            final var name = Objects.requireNonNull(getName(), () -> "name is null from " + this);
            final var elementType = getElementType();
            final var printable = isPrintable();
            final var deferChildren = isDeferChildren();
            final var namespace = getNamespace();
            return new PluginEntry(key, className, name, elementType, printable, deferChildren, namespace);
        }

        @Override
        public String toString() {
            return "PluginEntry.Builder{" + "key='"
                    + key + '\'' + ", className='"
                    + className + '\'' + ", name='"
                    + name + '\'' + ", elementType='"
                    + elementType + '\'' + ", printable="
                    + printable + ", deferChildren="
                    + deferChildren + ", namespace='"
                    + namespace + '\'' + '}';
        }
    }
}
