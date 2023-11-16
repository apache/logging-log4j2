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
import java.util.Objects;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * Wrapper {@code PathCondition} that accepts objects that are rejected by the wrapped component filter.
 */
@Plugin(name = "IfNot", category = Core.CATEGORY_NAME, printObject = true)
public final class IfNot implements PathCondition {

    private final PathCondition negate;

    private IfNot(final PathCondition negate) {
        this.negate = Objects.requireNonNull(negate, "filter");
    }

    public PathCondition getWrappedFilter() {
        return negate;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#accept(java.nio.file.Path, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
        return !negate.accept(baseDir, relativePath, attrs);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#beforeFileTreeWalk()
     */
    @Override
    public void beforeFileTreeWalk() {
        negate.beforeFileTreeWalk();
    }

    /**
     * Create an IfNot PathCondition.
     *
     * @param condition The condition to negate.
     * @return An IfNot PathCondition.
     */
    @PluginFactory
    public static IfNot createNotCondition(
            @PluginElement("PathConditions") @Required(message = "No condition provided for IfNot")
                    final PathCondition condition) {
        return new IfNot(condition);
    }

    @Override
    public String toString() {
        return "IfNot(" + negate + ")";
    }
}
