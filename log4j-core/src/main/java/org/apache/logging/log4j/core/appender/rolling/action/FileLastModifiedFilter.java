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

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Clock;
import org.apache.logging.log4j.core.util.ClockFactory;

/**
 * PathFilter that accepts paths that are older than the specified duration.
 */
@Plugin(name = "LastModified", category = "Core", printObject = true)
public final class FileLastModifiedFilter implements PathFilter {
    private static final Clock CLOCK = ClockFactory.getClock();

    private final Duration duration;

    private FileLastModifiedFilter(final Duration duration) {
        this.duration = Objects.requireNonNull(duration, "duration");
    }

    public Duration getDuration() {
        return duration;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.appender.rolling.action.PathFilter#accept(java.nio.file.Path,
     * java.nio.file.Path)
     */
    @Override
    public boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs) {
        final FileTime fileTime = attrs.lastModifiedTime();
        final long millis = fileTime.toMillis();
        final long ageMillis = CLOCK.currentTimeMillis() - millis;
        return ageMillis >= duration.toMillis();
    }

    /**
     * Create a FileLastModifiedFilter filter.
     * 
     * @param duration The path age that is accepted by this filter. Must be a valid Duration.
     * @return A FileLastModifiedFilter filter.
     */
    @PluginFactory
    public static FileLastModifiedFilter createAgeFilter( //
            @PluginAttribute("duration") final Duration duration) {
        return new FileLastModifiedFilter(duration);
    }

    @Override
    public String toString() {
        return "FileLastModifiedFilter(age=" + duration + ")";
    }
}
