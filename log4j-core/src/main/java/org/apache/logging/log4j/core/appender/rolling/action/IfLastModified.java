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
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PathCondition that accepts paths that are older than the specified duration.
 * @since 2.5
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

    /**
     * @deprecated since 2.24.0. In 3.0.0 the signature will change.
     */
    @Deprecated
    public org.apache.logging.log4j.core.appender.rolling.action.Duration getAge() {
        return org.apache.logging.log4j.core.appender.rolling.action.Duration.ofMillis(age.toMillis());
    }

    public List<PathCondition> getNestedConditions() {
        return Collections.unmodifiableList(Arrays.asList(nestedConditions));
    }

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

    @Override
    public void beforeFileTreeWalk() {
        IfAll.beforeFileTreeWalk(nestedConditions);
    }

    /**
     * @deprecated since 2.24.0 use {@link #newBuilder()}  instead.
     */
    @Deprecated
    public static IfLastModified createAgeCondition(
            final org.apache.logging.log4j.core.appender.rolling.action.Duration age,
            final PathCondition... pathConditions) {
        return newBuilder()
                .setAge(Duration.ofMillis(age.toMillis()))
                .setNestedConditions(pathConditions)
                .build();
    }

    @Override
    public String toString() {
        final String nested = nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(nestedConditions);
        return "IfLastModified(age=" + age + nested + ")";
    }

    /**
     * @since 2.24.0
     */
    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * @since 2.24.0
     */
    public static final class Builder implements org.apache.logging.log4j.core.util.Builder<IfLastModified> {
        @PluginBuilderAttribute
        @Required(message = "No age provided for IfLastModified")
        private org.apache.logging.log4j.core.appender.rolling.action.Duration age;

        @PluginElement("nestedConditions")
        private PathCondition[] nestedConditions;

        public Builder setAge(final Duration age) {
            this.age = org.apache.logging.log4j.core.appender.rolling.action.Duration.ofMillis(age.toMillis());
            return this;
        }

        public Builder setNestedConditions(final PathCondition... nestedConditions) {
            this.nestedConditions = nestedConditions;
            return this;
        }

        @Override
        public IfLastModified build() {
            return isValid() ? new IfLastModified(Duration.ofMillis(age.toMillis()), nestedConditions) : null;
        }
    }
}
