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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * An Appender container.
 */
@Configurable
@Plugin("appenders")
public final class AppendersPlugin {

    private AppendersPlugin() {
    }

    /**
     * Creates a Map of the Appenders.
     * @param appenders An array of Appenders.
     * @return The Appender Map.
     */
    @PluginFactory
    public static ConcurrentMap<String, Appender> createAppenders(
                @PluginElement("Appenders") final Appender[] appenders) {

        final ConcurrentMap<String, Appender> map =  new ConcurrentHashMap<>(appenders.length);

        for (final Appender appender : appenders) {
            map.put(appender.getName(), appender);
        }

        return map;
    }
}
