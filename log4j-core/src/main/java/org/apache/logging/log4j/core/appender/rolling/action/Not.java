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
import java.util.Objects;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Wrapper {@code PathCondition} that accepts objects that are rejected by the wrapped component filter.
 */
@Plugin(name = "Not", category = "Core", printObject = true)
public final class Not implements PathCondition {

    private final PathCondition negate;

    private Not(final PathCondition negate) {
        this.negate = Objects.requireNonNull(negate, "filter");
    }

    public PathCondition getWrappedFilter() {
        return negate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.appender.rolling.action.DeleteFilter#accept(java.nio.file.Path,
     * java.nio.file.Path)
     */
    @Override
    public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
        return !negate.accept(baseDir, relativePath, attrs);
    }

    /**
     * Create a Not PathCondition.
     * 
     * @param condition The condition to negate.
     * @return A Not PathCondition.
     */
    @PluginFactory
    public static Not createNotCondition( //
            @PluginElement("PathConditions") final PathCondition condition) {
        return new Not(condition);
    }

    @Override
    public String toString() {
        return "Not(" + negate + ")";
    }
}
