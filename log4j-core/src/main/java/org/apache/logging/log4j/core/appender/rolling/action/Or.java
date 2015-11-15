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
package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Composite {@code PathCondition} that accepts objects that are accepted by <em>any</em> component filters.
 */
@Plugin(name = "Or", category = "Core", printObject = true)
public final class Or implements PathCondition {

    private final PathCondition[] components;

    private Or(final PathCondition... filters) {
        this.components = Objects.requireNonNull(filters, "filters");
    }

    public PathCondition[] getDeleteFilters() {
        return components;
    }

    /* (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.rolling.action.DeleteFilter#accept(java.nio.file.Path,
     * java.nio.file.Path)
     */
    @Override
    public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
        for (final PathCondition component : components) {
            if (component.accept(baseDir, relativePath, attrs)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a Composite PathCondition: accepts if any of the nested conditions accepts.
     * 
     * @param components The component conditions.
     * @return A Composite PathCondition.
     */
    @PluginFactory
    public static Or createOrCondition( //
            @PluginElement("PathConditions") final PathCondition... components) {
        return new Or(components);
    }

    @Override
    public String toString() {
        return "Or" + Arrays.toString(components);
    }
}
