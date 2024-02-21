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
package org.apache.logging.log4j.kit.env.internal;

import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertySource;
import org.jspecify.annotations.Nullable;

/**
 * PropertySource backed by the current system properties.
 * <p>
 *     Should have a slightly lower priority than global system properties.
 * </p>
 */
public class ContextPropertiesPropertySource implements PropertySource {

    private static final int DEFAULT_PRIORITY = 0;

    private final String prefix;
    private final int priority;

    public ContextPropertiesPropertySource(final String contextName, final int priorityOffset) {
        this.prefix = "log4j2." + contextName + ".";
        this.priority = DEFAULT_PRIORITY + priorityOffset;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public @Nullable String getProperty(final String key) {
        final String actualKey = prefix + key;
        try {
            return System.getProperty(actualKey);
        } catch (final SecurityException e) {
            StatusLogger.getLogger()
                    .warn(
                            "{} lacks permissions to access system property {}.",
                            getClass().getName(),
                            actualKey,
                            e);
        }
        return null;
    }

    @Override
    public boolean containsProperty(final String key) {
        return getProperty(key) != null;
    }
}
