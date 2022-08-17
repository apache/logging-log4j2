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
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

/**
 * An Appender reference.
 */
@Configurable(printObject = true)
@Plugin
@PluginAliases("appender-ref")
public final class AppenderRef {

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
            @PluginAttribute @Required(message = "Appender references must contain a reference") final String ref,
            @PluginAttribute final Level level,
            @PluginElement final Filter filter) {
        return new AppenderRef(ref, level, filter);
    }
}
