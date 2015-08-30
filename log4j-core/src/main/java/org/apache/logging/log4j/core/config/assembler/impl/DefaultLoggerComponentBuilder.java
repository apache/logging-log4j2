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
package org.apache.logging.log4j.core.config.assembler.impl;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.assembler.api.AppenderRefComponentBuilder;
import org.apache.logging.log4j.core.config.assembler.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.assembler.api.LoggerComponentBuilder;

/**
 *
 */
public class DefaultLoggerComponentBuilder extends DefaultComponentBuilder<LoggerComponentBuilder> implements
        LoggerComponentBuilder {

    /**
     * Configure a logger.
     * @param assembler
     * @param name
     * @param level
     */
    public DefaultLoggerComponentBuilder(DefaultConfigurationBuilder<? extends Configuration> assembler, String name,
            String level) {
        super(assembler, name, "Logger");
        addAttribute("level", level);
    }

    /**
     * Configure a logger.
     * @param assembler
     * @param name
     * @param level
     * @param type
     */
    public DefaultLoggerComponentBuilder(DefaultConfigurationBuilder<? extends Configuration> assembler, String name,
            String level, String type) {
        super(assembler, name, type);
        addAttribute("level", level);
    }

    @Override
    public LoggerComponentBuilder add(AppenderRefComponentBuilder assembler) {
        addComponent(assembler);
        return this;
    }

    @Override
    public LoggerComponentBuilder add(FilterComponentBuilder assembler) {
        addComponent(assembler);
        return this;
    }
}