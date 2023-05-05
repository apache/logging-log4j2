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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * An Appender reference.
 */
@Plugin(name = "AppenderRef", category = Node.CATEGORY, printObject = true)
@PluginAliases("appender-ref")
public final class AppenderRef {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String ref;
    private final Level level;
    private final Filter filter;

    private AppenderRef(final String ref, final Level level, final Filter filter) {
        this.ref = ref;
        this.level = level;
        this.filter = filter;
    }

    public String getRef() {
        return ref;
    }

    public Level getLevel() {
        return level;
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    public String toString() {
        return ref;
    }

    /**
     * Create an Appender reference.
     * @param ref The name of the Appender.
     * @param level The Level to filter against.
     * @param filter The filter(s) to use.
     * @return The name of the Appender.
     */
    @PluginFactory
    public static AppenderRef createAppenderRef(
            @PluginAttribute("ref") final String ref,
            @PluginAttribute("level") final Level level,
            @PluginElement("Filter") final Filter filter) {

        if (ref == null) {
            LOGGER.error("Appender references must contain a reference");
            return null;
        }
        return new AppenderRef(ref, level, filter);
    }
}
