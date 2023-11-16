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
package org.apache.logging.log4j.core.pattern;

import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Replace tokens in the LogEvent message.
 */
@Plugin(name = "replace", category = Core.CATEGORY_NAME, printObject = true)
public final class RegexReplacement {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Pattern pattern;

    private final String substitution;

    /**
     * Private constructor.
     *
     * @param pattern The Pattern.
     * @param substitution The substitution String.
     */
    private RegexReplacement(final Pattern pattern, final String substitution) {
        this.pattern = pattern;
        this.substitution = substitution;
    }

    /**
     * Perform the replacement.
     * @param msg The String to match against.
     * @return the replacement String.
     */
    public String format(final String msg) {
        return pattern.matcher(msg).replaceAll(substitution);
    }

    @Override
    public String toString() {
        return "replace(regex=" + pattern.pattern() + ", replacement=" + substitution + ')';
    }

    /**
     * Create a RegexReplacement.
     * @param regex The regular expression to locate.
     * @param replacement The replacement value.
     * @return A RegexReplacement.
     */
    @PluginFactory
    public static RegexReplacement createRegexReplacement(
            @PluginAttribute("regex") final Pattern regex, @PluginAttribute("replacement") final String replacement) {
        if (regex == null) {
            LOGGER.error("A regular expression is required for replacement");
            return null;
        }
        if (replacement == null) {
            LOGGER.error("A replacement string is required to perform replacement");
        }
        // FIXME: should we use Matcher.quoteReplacement() here?
        return new RegexReplacement(regex, replacement);
    }
}
