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
package org.apache.logging.log4j.core.config.builder.impl;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;

/**
 * @since 2.4
 */
class DefaultLoggerComponentBuilder extends DefaultComponentAndConfigurationBuilder<LoggerComponentBuilder>
        implements LoggerComponentBuilder {

    /**
     * Configure a logger.
     * @param builder
     * @param name
     * @param level
     */
    public DefaultLoggerComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder, final String name, final String level) {
        super(builder, name, "Logger");
        if (level != null) {
            addAttribute("level", level);
        }
    }

    /**
     * Configure a logger.
     * @param builder
     * @param name
     * @param level
     * @param includeLocation
     */
    public DefaultLoggerComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder,
            final String name,
            final String level,
            final boolean includeLocation) {
        super(builder, name, "Logger");
        if (level != null) {
            addAttribute("level", level);
        }
        addAttribute("includeLocation", includeLocation);
    }

    /**
     * Configure a logger.
     * @param builder
     * @param name
     * @param level
     * @param type
     */
    public DefaultLoggerComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder,
            final String name,
            final String level,
            final String type) {
        super(builder, name, type);
        if (level != null) {
            addAttribute("level", level);
        }
    }

    /**
     * Configure a logger.
     * @param builder
     * @param name
     * @param level
     * @param type
     * @param includeLocation
     */
    public DefaultLoggerComponentBuilder(
            final DefaultConfigurationBuilder<? extends Configuration> builder,
            final String name,
            final String level,
            final String type,
            final boolean includeLocation) {
        super(builder, name, type);
        if (level != null) {
            addAttribute("level", level);
        }
        addAttribute("includeLocation", includeLocation);
    }

    @Override
    public LoggerComponentBuilder add(final AppenderRefComponentBuilder builder) {
        return addComponent(builder);
    }

    @Override
    public LoggerComponentBuilder add(final FilterComponentBuilder builder) {
        return addComponent(builder);
    }
}
