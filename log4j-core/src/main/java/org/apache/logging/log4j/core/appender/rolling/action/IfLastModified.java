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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * PathCondition that accepts paths that are older than the specified duration.
 */
@Configurable(printObject = true)
@Plugin
public final class IfLastModified implements PathCondition {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final Clock clock;

    private final Duration age;
    private final PathCondition[] nestedConditions;

    private IfLastModified(final Duration age, final PathCondition[] nestedConditions, final Clock clock) {
        this.age = Objects.requireNonNull(age, "age");
        this.nestedConditions = PathCondition.copy(nestedConditions);
        this.clock = clock;
    }

    public Duration getAge() {
        return age;
    }

    public List<PathCondition> getNestedConditions() {
        return List.of(nestedConditions);
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
        final long ageMillis = clock.currentTimeMillis() - millis;
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

    @Override
    public String toString() {
        final String nested = nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(nestedConditions);
        return "IfLastModified(age=" + age + nested + ")";
    }

    /**
     * Create an IfLastModified condition.
     *
     * @param age The path age that is accepted by this condition. Must be a valid Duration.
     * @param nestedConditions nested conditions to evaluate if this condition accepts a path
     * @return An IfLastModified condition.
     */
    @Deprecated(since = "3.0.0", forRemoval = true)
    public static IfLastModified createAgeCondition(final Duration age, final PathCondition... nestedConditions) {
        return newBuilder().setAge(age).setNestedConditions(nestedConditions).get();
    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Supplier<IfLastModified> {
        private Duration age;
        private PathCondition[] nestedConditions;
        private Clock clock;

        public Builder setAge(@PluginAttribute final Duration age) {
            this.age = age;
            return this;
        }

        public Builder setNestedConditions(@PluginElement final PathCondition... nestedConditions) {
            this.nestedConditions = nestedConditions;
            return this;
        }

        @Inject
        public Builder setClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        @Override
        public IfLastModified get() {
            if (clock == null) {
                clock = ClockFactory.getClock();
            }
            return new IfLastModified(age, nestedConditions, clock);
        }
    }
}
