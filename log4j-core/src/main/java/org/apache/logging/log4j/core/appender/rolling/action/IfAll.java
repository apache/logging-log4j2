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
package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Objects;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * Composite {@code PathCondition} that only accepts objects that are accepted by <em>all</em> component conditions.
 * Corresponds to logical "AND".
 */
@Plugin(name = "IfAll", category = Core.CATEGORY_NAME, printObject = true)
public final class IfAll implements PathCondition {

    private final PathCondition[] components;

    private IfAll(final PathCondition... filters) {
        this.components = Objects.requireNonNull(filters, "filters");
    }

    public PathCondition[] getDeleteFilters() {
        return components;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#accept(java.nio.file.Path,
     * java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
        if (components == null || components.length == 0) {
            return false; // unconditional delete not supported
        }
        return accept(components, baseDir, relativePath, attrs);
    }

    /**
     * Returns {@code true} if all the specified conditions accept the specified path, {@code false} otherwise.
     *
     * @param list the array of conditions to evaluate
     * @param baseDir the directory from where to start scanning for deletion candidate files
     * @param relativePath the candidate for deletion. This path is relative to the baseDir.
     * @param attrs attributes of the candidate path
     * @return {@code true} if all the specified conditions accept the specified path, {@code false} otherwise
     * @throws NullPointerException if any of the parameters is {@code null}
     */
    public static boolean accept(
            final PathCondition[] list, final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
        for (final PathCondition component : list) {
            if (!component.accept(baseDir, relativePath, attrs)) {
                return false;
            }
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#beforeFileTreeWalk()
     */
    @Override
    public void beforeFileTreeWalk() {
        beforeFileTreeWalk(components);
    }

    /**
     * Calls {@link #beforeFileTreeWalk()} on all of the specified nested conditions.
     *
     * @param nestedConditions the conditions to call {@link #beforeFileTreeWalk()} on
     */
    public static void beforeFileTreeWalk(final PathCondition[] nestedConditions) {
        for (final PathCondition condition : nestedConditions) {
            condition.beforeFileTreeWalk();
        }
    }

    /**
     * Create a Composite PathCondition whose components all need to accept before this condition accepts.
     *
     * @param components The component filters.
     * @return A Composite PathCondition.
     */
    @PluginFactory
    public static IfAll createAndCondition(
            @PluginElement("PathConditions") @Required(message = "No components provided for IfAll")
                    final PathCondition... components) {
        return new IfAll(components);
    }

    @Override
    public String toString() {
        return "IfAll" + Arrays.toString(components);
    }
}
