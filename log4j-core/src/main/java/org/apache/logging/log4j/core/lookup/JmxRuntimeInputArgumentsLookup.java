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
package org.apache.logging.log4j.core.lookup;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.internal.SystemUtils;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Maps JVM input arguments (but not main arguments) using JMX to acquire JVM arguments.
 *
 * @see java.lang.management.RuntimeMXBean#getInputArguments()
 * @since 2.1
 */
@Plugin(name = "jvmrunargs", category = StrLookup.CATEGORY)
public class JmxRuntimeInputArgumentsLookup extends MapLookup {

    private static final Logger LOGGER = StatusLogger.getLogger();

    public static final JmxRuntimeInputArgumentsLookup JMX_SINGLETON = new JmxRuntimeInputArgumentsLookup();

    /**
     * Constructor when used directly as a plugin.
     */
    public JmxRuntimeInputArgumentsLookup() {
        this(getMapFromJmx());
    }

    public JmxRuntimeInputArgumentsLookup(final Map<String, String> map) {
        super(map);
    }

    @Override
    public String lookup(final LogEvent ignored, final String key) {
        if (key == null) {
            return null;
        }
        final Map<String, String> map = getMap();
        return map == null ? null : map.get(key);
    }

    private static Map<String, String> getMapFromJmx() {
        if (!SystemUtils.isOsAndroid()) {
            try {
                return MapLookup.toMap(ManagementFactory.getRuntimeMXBean().getInputArguments());
            } catch (LinkageError e) {
                LOGGER.warn("Failed to get JMX arguments from JVM.", e);
            }
        }
        return Collections.emptyMap();
    }
}
