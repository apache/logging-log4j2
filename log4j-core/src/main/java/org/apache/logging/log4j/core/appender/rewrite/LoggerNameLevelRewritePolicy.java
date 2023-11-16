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
package org.apache.logging.log4j.core.appender.rewrite;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;

/**
 * Rewrites log event levels for a given logger name.
 *
 * @since 2.4
 */
@Plugin(
        name = "LoggerNameLevelRewritePolicy",
        category = Core.CATEGORY_NAME,
        elementType = "rewritePolicy",
        printObject = true)
public class LoggerNameLevelRewritePolicy implements RewritePolicy {

    /**
     * Creates a policy to rewrite levels for a given logger name.
     *
     * @param loggerNamePrefix
     *        The logger name prefix for events to rewrite; all event logger names that start with this string will be
     *        rewritten.
     * @param levelPairs
     *        The levels to rewrite, the key is the source level, the value the target level.
     * @return a new LoggerNameLevelRewritePolicy
     */
    @PluginFactory
    public static LoggerNameLevelRewritePolicy createPolicy(
            // @formatter:off
            @PluginAttribute("logger") final String loggerNamePrefix,
            @PluginElement("KeyValuePair") final KeyValuePair[] levelPairs) {
        // @formatter:on
        final Map<Level, Level> newMap = new HashMap<>(levelPairs.length);
        for (final KeyValuePair keyValuePair : levelPairs) {
            newMap.put(getLevel(keyValuePair.getKey()), getLevel(keyValuePair.getValue()));
        }
        return new LoggerNameLevelRewritePolicy(loggerNamePrefix, newMap);
    }

    private static Level getLevel(final String name) {
        return Level.getLevel(toRootUpperCase(name));
    }

    private final String loggerName;

    private final Map<Level, Level> map;

    private LoggerNameLevelRewritePolicy(final String loggerName, final Map<Level, Level> map) {
        this.loggerName = loggerName;
        this.map = map;
    }

    @Override
    public LogEvent rewrite(final LogEvent event) {
        if (event.getLoggerName() == null || !event.getLoggerName().startsWith(loggerName)) {
            return event;
        }
        final Level sourceLevel = event.getLevel();
        final Level newLevel = map.get(sourceLevel);
        if (newLevel == null || newLevel == sourceLevel) {
            return event;
        }
        final LogEvent result =
                new Log4jLogEvent.Builder(event).setLevel(newLevel).build();
        return result;
    }
}
