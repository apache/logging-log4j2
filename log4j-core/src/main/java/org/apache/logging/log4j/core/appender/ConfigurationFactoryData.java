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
package org.apache.logging.log4j.core.appender;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;

/**
 * Factory Data that carries a configuration.
 */
public class ConfigurationFactoryData {

    /**
     * This field is public to follow the style of existing FactoryData classes.
     */
    public final Configuration configuration;

    public ConfigurationFactoryData(final Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Gets the LoggerContext from the Configuration or null.
     *
     * @return the LoggerContext from the Configuration or null.
     */
    public LoggerContext getLoggerContext() {
        return configuration != null ? configuration.getLoggerContext() : null;
    }
}
