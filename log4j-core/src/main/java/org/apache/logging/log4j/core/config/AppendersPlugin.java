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
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * An Appender container.
 */
@Plugin(name = "appenders", category = Core.CATEGORY_NAME)
public final class AppendersPlugin {

    private AppendersPlugin() {}

    /**
     * Creates a Map of the Appenders.
     * @param appenders An array of Appenders.
     * @return The Appender Map.
     */
    @PluginFactory
    public static ConcurrentMap<String, Appender> createAppenders(
            @PluginElement("Appenders") final Appender[] appenders) {

        final ConcurrentMap<String, Appender> map = new ConcurrentHashMap<>(appenders.length);

        for (final Appender appender : appenders) {
            map.put(appender.getName(), appender);
        }

        return map;
    }
}
