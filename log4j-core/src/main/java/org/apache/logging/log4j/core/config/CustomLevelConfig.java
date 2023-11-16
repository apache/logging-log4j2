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
package org.apache.logging.log4j.core.config;

import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Descriptor of a custom Level object that is created via configuration.
 */
@Plugin(name = "CustomLevel", category = Core.CATEGORY_NAME, printObject = true)
public final class CustomLevelConfig {

    /**
     * The empty array.
     */
    static final CustomLevelConfig[] EMPTY_ARRAY = {};

    private final String levelName;
    private final int intLevel;

    private CustomLevelConfig(final String levelName, final int intLevel) {
        this.levelName = Objects.requireNonNull(levelName, "levelName is null");
        this.intLevel = intLevel;
    }

    /**
     * Creates a CustomLevelConfig object. This also defines the Level object with a call to
     * {@link Level#forName(String, int)}.
     *
     * @param levelName name of the custom level.
     * @param intLevel the intLevel that determines where this level resides relative to the built-in levels
     * @return A CustomLevelConfig object.
     */
    @PluginFactory
    public static CustomLevelConfig createLevel( // @formatter:off
            @PluginAttribute("name") final String levelName, @PluginAttribute("intLevel") final int intLevel) {
        // @formatter:on

        StatusLogger.getLogger().debug("Creating CustomLevel(name='{}', intValue={})", levelName, intLevel);
        Level.forName(levelName, intLevel);
        return new CustomLevelConfig(levelName, intLevel);
    }

    /**
     * Returns the custom level name.
     *
     * @return the custom level name
     */
    public String getLevelName() {
        return levelName;
    }

    /**
     * Returns the custom level intLevel that determines the strength of the custom level relative to the built-in
     * levels.
     *
     * @return the custom level intLevel
     */
    public int getIntLevel() {
        return intLevel;
    }

    @Override
    public int hashCode() {
        return intLevel ^ levelName.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof CustomLevelConfig)) {
            return false;
        }
        final CustomLevelConfig other = (CustomLevelConfig) object;
        return this.intLevel == other.intLevel && this.levelName.equals(other.levelName);
    }

    @Override
    public String toString() {
        return "CustomLevel[name=" + levelName + ", intLevel=" + intLevel + "]";
    }
}
