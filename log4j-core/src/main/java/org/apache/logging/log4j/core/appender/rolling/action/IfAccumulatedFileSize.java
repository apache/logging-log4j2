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
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.appender.rolling.FileSize;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PathCondition that accepts paths after the accumulated file size threshold is exceeded during the file tree walk.
 */
@Plugin(name = "IfAccumulatedFileSize", category = Core.CATEGORY_NAME, printObject = true)
public final class IfAccumulatedFileSize implements PathCondition {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final long thresholdBytes;
    private long accumulatedSize;
    private final PathCondition[] nestedConditions;

    private IfAccumulatedFileSize(final long thresholdSize, final PathCondition... nestedConditions) {
        if (thresholdSize <= 0) {
            throw new IllegalArgumentException("Count must be a positive integer but was " + thresholdSize);
        }
        this.thresholdBytes = thresholdSize;
        this.nestedConditions = PathCondition.copy(nestedConditions);
    }

    public long getThresholdBytes() {
        return thresholdBytes;
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
        accumulatedSize += attrs.size();
        final boolean result = accumulatedSize > thresholdBytes;
        final String match = result ? ">" : "<=";
        final String accept = result ? "ACCEPTED" : "REJECTED";
        LOGGER.trace(
                "IfAccumulatedFileSize {}: {} accumulated size '{}' {} thresholdBytes '{}'",
                accept,
                relativePath,
                accumulatedSize,
                match,
                thresholdBytes);
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
        accumulatedSize = 0;
        IfAll.beforeFileTreeWalk(nestedConditions);
    }

    /**
     * Create an IfAccumulatedFileSize condition.
     *
     * @param size The threshold accumulated file size from which files will be deleted.
     * @return An IfAccumulatedFileSize condition.
     */
    @PluginFactory
    public static IfAccumulatedFileSize createFileSizeCondition(
            // @formatter:off
            @PluginAttribute("exceeds") final String size,
            @PluginElement("PathConditions") final PathCondition... nestedConditions) {
        // @formatter:on

        if (size == null) {
            LOGGER.error("IfAccumulatedFileSize missing mandatory size threshold.");
        }
        final long threshold = size == null ? Long.MAX_VALUE : FileSize.parse(size, Long.MAX_VALUE);
        return new IfAccumulatedFileSize(threshold, nestedConditions);
    }

    @Override
    public String toString() {
        final String nested = nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(nestedConditions);
        return "IfAccumulatedFileSize(exceeds=" + thresholdBytes + nested + ")";
    }
}
