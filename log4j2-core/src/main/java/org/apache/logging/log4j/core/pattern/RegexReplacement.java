/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.List;
import java.util.regex.Pattern;

/**
 *
 */
@Plugin(name="replace", type="Core", printObject=true)
public final class RegexReplacement {

    private final Pattern pattern;

    private final String substitution;

    private static Logger logger = StatusLogger.getLogger();

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private RegexReplacement(Pattern pattern, String substitution) {
        this.pattern = pattern;
        this.substitution = substitution;
    }

    /**
     * {@inheritDoc}
     */
    public String format(String msg) {
        return pattern.matcher(msg).replaceAll(substitution);
    }

    public String toString() {
        return "replace(regex=" + pattern.pattern() + ", replacement=" + substitution + ")";
    }

    @PluginFactory
    public static RegexReplacement createRegexReplacement(@PluginAttr("regex") String regex,
                                                          @PluginAttr("replacement") String replacement) {
        if (regex == null) {
            logger.error("A regular expression is required for replacement");
            return null;
        }
        if (replacement == null) {
            logger.error("A replacement string is required to perform replacement");
        }
        Pattern p = Pattern.compile(regex);
        return new RegexReplacement(p, replacement);
    }

}
