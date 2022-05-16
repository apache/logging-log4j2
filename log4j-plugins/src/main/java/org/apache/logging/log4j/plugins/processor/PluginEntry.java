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

package org.apache.logging.log4j.plugins.processor;

import java.util.function.Supplier;

/**
 * Descriptor for {@link org.apache.logging.log4j.plugins.Plugin} metadata.
 */
public class PluginEntry {
    private final String key;
    private final String className;
    private final String name;
    private final String elementName;
    private final boolean printable;
    private final boolean defer;
    private final String category;
    private final Class<?>[] interfaces;

    public PluginEntry(
            String key, String className, String name, String elementName, boolean printable, boolean defer, String category) {
        this.key = key;
        this.className = className;
        this.name = name;
        this.elementName = elementName;
        this.printable = printable;
        this.defer = defer;
        this.category = category;
        this.interfaces = null;
    }

    public PluginEntry(
            final String key, final String className, final String name, final String elementName, final boolean printable,
            final boolean defer, final String category, final Class<?>... interfaces) {
        this.key = key;
        this.className = className;
        this.name = name;
        this.elementName = elementName;
        this.printable = printable;
        this.defer = defer;
        this.category = category;
        this.interfaces = interfaces;
    }

    private PluginEntry(final Builder builder) {
        key = builder.getKey();
        className = builder.getClassName();
        name = builder.getName();
        elementName = builder.getElementName();
        printable = builder.isPrintable();
        defer = builder.isDefer();
        category = builder.getCategory();
        final Class<?>[] classes = builder.getInterfaces();
        interfaces = classes != null ? classes.clone() : null;
    }

    public String getKey() {
        return key;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String getElementName() {
        return elementName;
    }

    public boolean isPrintable() {
        return printable;
    }

    public boolean isDefer() {
        return defer;
    }

    public String getCategory() {
        return category;
    }

    public Class<?>[] getInterfaces() {
        return interfaces;
    }

    @Override
    public String toString() {
        return "PluginEntry [key=" + key + ", className=" + className + ", name=" + name + ", printable=" + printable
                + ", defer=" + defer + ", category=" + category + "]";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder implements Supplier<PluginEntry> {
        private String key;
        private String className;
        private String name;
        private String elementName;
        private boolean printable;
        private boolean defer;
        private String category;
        private Class<?>[] interfaces;

        public String getKey() {
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

        public String getElementName() {
            return elementName;
        }

        public Builder setElementName(final String elementName) {
            this.elementName = elementName;
            return this;
        }

        public boolean isPrintable() {
            return printable;
        }

        public Builder setPrintable(final boolean printable) {
            this.printable = printable;
            return this;
        }

        public boolean isDefer() {
            return defer;
        }

        public Builder setDefer(final boolean defer) {
            this.defer = defer;
            return this;
        }

        public String getCategory() {
            return category;
        }

        public Builder setCategory(final String category) {
            this.category = category;
            return this;
        }

        public Class<?>[] getInterfaces() {
            return interfaces;
        }

        public Builder setInterfaces(final Class<?>... interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        @Override
        public PluginEntry get() {
            return new PluginEntry(this);
        }
    }
}
