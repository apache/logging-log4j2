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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * PathCondition that accepts paths after some count threshold is exceeded during the file tree walk.
 */
@Plugin(name = "IfAccumulatedFileCount", category = Core.CATEGORY_NAME, printObject = true)
public final class IfAccumulatedFileCount implements PathCondition {
    private static final Logger LOGGER = StatusLogger.getLogger();
    private final int threshold;
    private int count;
    private final PathCondition[] nestedConditions;

    private IfAccumulatedFileCount(final int thresholdParam, final PathCondition... nestedConditions) {
        if (thresholdParam <= 0) {
            throw new IllegalArgumentException("Count must be a positive integer but was " + thresholdParam);
        }
        this.threshold = thresholdParam;
        this.nestedConditions = PathCondition.copy(nestedConditions);
    }

    public int getThresholdCount() {
        return threshold;
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
        final boolean result = ++count > threshold;
        final String match = result ? ">" : "<=";
        final String accept = result ? "ACCEPTED" : "REJECTED";
        LOGGER.trace(
                "IfAccumulatedFileCount {}: {} count '{}' {} threshold '{}'",
                accept,
                relativePath,
                count,
                match,
                threshold);
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
        count = 0;
        IfAll.beforeFileTreeWalk(nestedConditions);
    }

    /**
     * Create an IfAccumulatedFileCount condition.
     *
     * @param threshold The threshold count from which files will be deleted.
     * @return An IfAccumulatedFileCount condition.
     */
    @PluginFactory
    public static IfAccumulatedFileCount createFileCountCondition(
            // @formatter:off
            @PluginAttribute(value = "exceeds", defaultInt = Integer.MAX_VALUE) final int threshold,
            @PluginElement("PathConditions") final PathCondition... nestedConditions) {
        // @formatter:on

        if (threshold == Integer.MAX_VALUE) {
            LOGGER.error("IfAccumulatedFileCount invalid or missing threshold value.");
        }
        return new IfAccumulatedFileCount(threshold, nestedConditions);
    }

    @Override
    public String toString() {
        final String nested = nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(nestedConditions);
        return "IfAccumulatedFileCount(exceeds=" + threshold + nested + ")";
    }
}
