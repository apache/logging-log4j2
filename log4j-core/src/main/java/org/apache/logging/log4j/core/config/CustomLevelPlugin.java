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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Custom Level object that is created via configuration.
 */
@Plugin(name = "CustomLevel", category = "Core") // TBD: category="Level" does not work... Why?
public final class CustomLevelPlugin {

    private CustomLevelPlugin() {
    }

    /**
     * Creates a custom Level object.
     * 
     * @param levelName name of the custom level.
     * @param intLevel the intLevel that determines where this level resides relative to the built-in levels
     * @return A Level object.
     */
    @PluginFactory
    public static Level createLevel(
// @formatter:off
            @PluginAttribute("name") final String levelName,
            @PluginAttribute("intLevel") final int intLevel) {
        // @formatter:on
        Level result = Level.forName(levelName, intLevel);
        return result;
    }
}
