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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * The array of failover Appenders.
 */
@Plugin(name = "failovers", category = Core.CATEGORY_NAME)
public final class FailoversPlugin {

    private static final Logger LOGGER = StatusLogger.getLogger();

    /**
     * Prevent instantiation.
     */
    private FailoversPlugin() {}

    /**
     * Returns the appender references.
     * @param refs The references to return.
     * @return The appender references.
     */
    @PluginFactory
    public static String[] createFailovers(@PluginElement("AppenderRef") final AppenderRef... refs) {

        if (refs == null) {
            LOGGER.error("failovers must contain an appender reference");
            return null;
        }
        final String[] arr = new String[refs.length];
        for (int i = 0; i < refs.length; ++i) {
            arr[i] = refs[i].getRef();
        }
        return arr;
    }
}
