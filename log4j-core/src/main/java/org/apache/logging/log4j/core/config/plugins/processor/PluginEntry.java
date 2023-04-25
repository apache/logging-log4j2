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
package org.apache.logging.log4j.core.config.plugins.processor;

import java.io.Serializable;

/**
 * Memento object for storing a plugin entry to a cache file.
 */
public class PluginEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String key;
    private String className;
    private String name;
    private boolean printable;
    private boolean defer;
    private transient String category;

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(final String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isPrintable() {
        return printable;
    }

    public void setPrintable(final boolean printable) {
        this.printable = printable;
    }

    public boolean isDefer() {
        return defer;
    }

    public void setDefer(final boolean defer) {
        this.defer = defer;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return "PluginEntry [key=" + key + ", className=" + className + ", name=" + name + ", printable=" + printable
                + ", defer=" + defer + ", category=" + category + "]";
    }
}
