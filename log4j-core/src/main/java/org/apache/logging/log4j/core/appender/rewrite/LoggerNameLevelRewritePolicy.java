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
     *        The logger name prefix for events to rewrite; an event will be rewritten if its logger name is
     *        exactly this string, or a true descendant of it in the usual Log4j logger-name hierarchy sense
     *        (this string followed by {@code '.'} and more). An empty string matches every logger name, and a
     *        prefix that already ends with {@code '.'} is matched as a plain string prefix, since the
     *        trailing separator already supplies the hierarchy boundary.
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
        if (!matchesLoggerNameOrDescendant(event.getLoggerName())) {
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

    /**
     * Matches {@code eventLoggerName} against the configured {@link #loggerName} the same way the rest of
     * Log4j treats logger-name hierarchy: either an exact match, or a true descendant separated by a '.'.
     * A plain {@code startsWith} check would also match an unrelated sibling package that merely shares the
     * same character prefix, e.g. a policy for {@code "com.foo"} would incorrectly also match
     * {@code "com.foobar.Baz"}.
     * <p>
     * An empty {@link #loggerName} (matches every logger, by design) or one that already ends with the
     * separator (the separator itself already supplies the hierarchy boundary) is exempt from the extra
     * boundary check, since a plain prefix match is already unambiguous in both cases.
     */
    private boolean matchesLoggerNameOrDescendant(final String eventLoggerName) {
        if (eventLoggerName == null || !eventLoggerName.startsWith(loggerName)) {
            return false;
        }
        return loggerName.isEmpty()
                || loggerName.endsWith(".")
                || eventLoggerName.length() == loggerName.length()
                || eventLoggerName.charAt(loggerName.length()) == '.';
    }
}
