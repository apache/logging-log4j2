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
package org.apache.logging.log4j.core.config.plugins.util;

import org.apache.logging.log4j.core.config.plugins.processor.PluginEntry;

/**
 * Plugin Descriptor. This is a memento object for Plugin annotations paired to their annotated classes.
 *
 * @param <T> The plug-in class, which can be any kind of class.
 * @see org.apache.logging.log4j.core.config.plugins.Plugin
 */
public class PluginType<T> {

    private final PluginEntry pluginEntry;
    private final Class<T> pluginClass;
    private final String elementName;

    /**
     * @since 2.1
     */
    public PluginType(final PluginEntry pluginEntry, final Class<T> pluginClass, final String elementName) {
        this.pluginEntry = pluginEntry;
        this.pluginClass = pluginClass;
        this.elementName = elementName;
    }

    public Class<T> getPluginClass() {
        return this.pluginClass;
    }

    public String getElementName() {
        return this.elementName;
    }

    /**
     * @since 2.1
     */
    public String getKey() {
        return this.pluginEntry.getKey();
    }

    public boolean isObjectPrintable() {
        return this.pluginEntry.isPrintable();
    }

    public boolean isDeferChildren() {
        return this.pluginEntry.isDefer();
    }

    /**
     * @since 2.1
     */
    public String getCategory() {
        return this.pluginEntry.getCategory();
    }

    @Override
    public String toString() {
        return "PluginType [pluginClass=" + pluginClass + ", key="
                + pluginEntry.getKey() + ", elementName="
                + pluginEntry.getName() + ", isObjectPrintable="
                + pluginEntry.isPrintable() + ", isDeferChildren=="
                + pluginEntry.isDefer() + ", category="
                + pluginEntry.getCategory() + "]";
    }
}
