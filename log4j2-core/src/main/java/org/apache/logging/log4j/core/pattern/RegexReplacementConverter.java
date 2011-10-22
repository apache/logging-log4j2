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
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Replacement pattern converter.
 */
@Plugin(name="replace", type="Converter")
@ConverterKeys({"replace"})
public final class RegexReplacementConverter extends LogEventPatternConverter {

    private final Pattern pattern;

    private final String substitution;

    private static Logger logger = StatusLogger.getLogger();

    private List<PatternConverter> converters;

    /**
     * Construct the converter.
     * @param converters The PatternConverters to generate the text to manipulate.
     * @param pattern The regular expression Pattern.
     * @param substitution The substitution string.
     */
    private RegexReplacementConverter(List<PatternConverter> converters,
                                      Pattern pattern, String substitution) {
        super("replace", "replace");
        this.pattern = pattern;
        this.substitution = substitution;
        this.converters = converters;
    }

    /**
     * Gets an instance of the class.
     *
     * @param config The current Configuration.
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static RegexReplacementConverter newInstance(Configuration config, final String[] options) {
        if (options.length != 3) {
            logger.error("Incorrect number of options on replace. Expected 3 received " + options.length);
            return null;
        }
        if (options[0] == null) {
            logger.error("No pattern supplied on replace");
            return null;
        }
        if (options[1] == null) {
            logger.error("No regular expression supplied on replace");
            return null;
        }
        if (options[2] == null) {
            logger.error("No substitution supplied on replace");
            return null;
        }
        Pattern p = Pattern.compile(options[1]);
        PatternParser parser = PatternLayout.createPatternParser(config);
        List<PatternConverter> converters = parser.parse(options[0]);
        return new RegexReplacementConverter(converters, p, options[2]);
    }


    /**
     * {@inheritDoc}
     */
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        StringBuilder buf = new StringBuilder();
        for (PatternConverter c : converters) {
            c.format(event, buf);
        }
        toAppendTo.append(pattern.matcher(buf.toString()).replaceAll(substitution));
    }
}
