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

import java.util.EventObject;

import org.apache.logging.log4j.core.LoggerContext;

/**
 * Event emitted when a {@link LoggerContext} is replacing an old {@link Configuration} with a new one.
 *
 * @since 3.0.0
 */
public class ConfigurationChangeEvent extends EventObject {
    private final Configuration oldConfiguration;
    private final Configuration newConfiguration;

    /**
     * Constructs a new ConfigurationChangeEvent.
     *
     * @param source           the LoggerContext in which the configuration change event initially occurred
     * @param oldConfiguration the old Configuration instance
     * @param newConfiguration the new Configuration instance
     * @throws IllegalArgumentException if source is null
     */
    public ConfigurationChangeEvent(final LoggerContext source, final Configuration oldConfiguration, final Configuration newConfiguration) {
        super(source);
        this.oldConfiguration = oldConfiguration;
        this.newConfiguration = newConfiguration;
    }

    public LoggerContext getLoggerContext() {
        return (LoggerContext) getSource();
    }

    public Configuration getOldConfiguration() {
        return oldConfiguration;
    }

    public Configuration getNewConfiguration() {
        return newConfiguration;
    }

    @Override
    public String toString() {
        return "ConfigurationChangeEvent{" +
                "oldConfiguration=" + oldConfiguration +
                ", newConfiguration=" + newConfiguration +
                ", source=" + source +
                '}';
    }
}
