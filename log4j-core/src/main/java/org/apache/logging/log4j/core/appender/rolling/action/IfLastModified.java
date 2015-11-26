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
import java.nio.file.attribute.FileTime;
import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PathCondition that accepts paths that are older than the specified duration.
 */
@Plugin(name = "IfLastModified", category = "Core", printObject = true)
public final class IfLastModified implements PathCondition {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final Clock CLOCK = ClockFactory.getClock();

    private final Duration age;

    private IfLastModified(final Duration age) {
        this.age = Objects.requireNonNull(age, "age");
    }

    public Duration getAge() {
        return age;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#accept(java.nio.file.Path, java.nio.file.Path, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
        final FileTime fileTime = attrs.lastModifiedTime();
        final long millis = fileTime.toMillis();
        final long ageMillis = CLOCK.currentTimeMillis() - millis;
        final boolean result = ageMillis >= age.toMillis();
        final String match = result ? ">=" : "<";
        LOGGER.trace("IfLastModified: {} ageMillis '{}' {} '{}'", relativePath, ageMillis, match, age);
        return result;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathCondition#beforeFileTreeWalk()
     */
    @Override
    public void beforeFileTreeWalk() {
    }

    /**
     * Create an IfLastModified condition.
     * 
     * @param age The path age that is accepted by this condition. Must be a valid Duration.
     * @return An IfLastModified condition.
     */
    @PluginFactory
    public static IfLastModified createAgeCondition( //
            @PluginAttribute("age") final Duration age) {
        return new IfLastModified(age);
    }

    @Override
    public String toString() {
        return "IfLastModified(age=" + age + ")";
    }
}
