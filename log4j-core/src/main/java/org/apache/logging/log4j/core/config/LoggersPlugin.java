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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Container of Logger objects.
 */
@Plugin(name = "loggers", category = Node.CATEGORY)
public final class LoggersPlugin {

    private LoggersPlugin() {}

    /**
     * Create a Loggers object to contain all the Loggers.
     * @param loggers An array of Loggers.
     * @return A Loggers object.
     */
    @PluginFactory
    public static Loggers createLoggers(@PluginElement("Loggers") final LoggerConfig[] loggers) {
        final ConcurrentMap<String, LoggerConfig> loggerMap = new ConcurrentHashMap<>();
        LoggerConfig root = null;

        for (final LoggerConfig logger : loggers) {
            if (logger != null) {
                if (logger.getName().isEmpty()) {
                    if (root != null) {
                        throw new IllegalStateException(
                                "Configuration has multiple root loggers. There can be only one.");
                    }
                    root = logger;
                }
                loggerMap.put(logger.getName(), logger);
            }
        }

        return new Loggers(loggerMap, root);
    }
}
