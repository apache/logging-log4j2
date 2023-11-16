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
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PathCondition that accepts paths that are older than the specified duration.
 */
@Plugin(name = "IfLastModified", category = Core.CATEGORY_NAME, printObject = true)
public final class IfLastModified implements PathCondition {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Clock CLOCK = ClockFactory.getClock();

    private final Duration age;
    private final PathCondition[] nestedConditions;

    private IfLastModified(final Duration age, final PathCondition[] nestedConditions) {
        this.age = Objects.requireNonNull(age, "age");
        this.nestedConditions = PathCondition.copy(nestedConditions);
    }

    public Duration getAge() {
        return age;
    }

    public List<PathCondition> getNestedConditions() {
        return Collections.unmodifiableList(Arrays.asList(nestedConditions));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#accept(java.nio.file.Path,
     * java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public boolean accept(final Path basePath, final Path relativePath, final BasicFileAttributes attrs) {
        final FileTime fileTime = attrs.lastModifiedTime();
        final long millis = fileTime.toMillis();
        final long ageMillis = CLOCK.currentTimeMillis() - millis;
        final boolean result = ageMillis >= age.toMillis();
        final String match = result ? ">=" : "<";
        final String accept = result ? "ACCEPTED" : "REJECTED";
        LOGGER.trace("IfLastModified {}: {} ageMillis '{}' {} '{}'", accept, relativePath, ageMillis, match, age);
        if (result) {
            return IfAll.accept(nestedConditions, basePath, relativePath, attrs);
        }
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#beforeFileTreeWalk()
     */
    @Override
    public void beforeFileTreeWalk() {
        IfAll.beforeFileTreeWalk(nestedConditions);
    }

    /**
     * Create an IfLastModified condition.
     *
     * @param age The path age that is accepted by this condition. Must be a valid Duration.
     * @param nestedConditions nested conditions to evaluate if this condition accepts a path
     * @return An IfLastModified condition.
     */
    @PluginFactory
    public static IfLastModified createAgeCondition(
            // @formatter:off
            @PluginAttribute("age") @Required(message = "No age provided for IfLastModified") final Duration age,
            @PluginElement("PathConditions") final PathCondition... nestedConditions) {
        // @formatter:on
        return new IfLastModified(age, nestedConditions);
    }

    @Override
    public String toString() {
        final String nested = nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(nestedConditions);
        return "IfLastModified(age=" + age + nested + ")";
    }
}
