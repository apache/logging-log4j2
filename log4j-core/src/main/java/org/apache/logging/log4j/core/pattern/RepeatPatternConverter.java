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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.Strings;

/**
 * Equals pattern converter.
 */
@Plugin(name = "repeat", category = PatternConverter.CATEGORY)
@ConverterKeys({":|", "repeat"})
@PerformanceSensitive("allocation")
public final class RepeatPatternConverter extends LogEventPatternConverter {

    private final String result;

    /**
     * Gets an instance of the class.
     *
     * @param config  The current Configuration.
     * @param options pattern options, an array of two elements: repeatString and count.
     * @return instance of class.
     */
    public static RepeatPatternConverter newInstance(final Configuration config, final String[] options) {
        if (options.length != 2) {
            LOGGER.error("Incorrect number of options on repeat. Expected 2 received " + options.length);
            return null;
        }
        if (options[0] == null) {
            LOGGER.error("No string supplied on repeat");
            return null;
        }
        if (options[1] == null) {
            LOGGER.error("No repeat count supplied on repeat");
            return null;
        }
        int count = 0;
        String result = options[0];
        try {
            count = Integers.parseInt(options[1]);
            result = Strings.repeat(options[0], count);
        } catch (Exception ex) {
            LOGGER.error("The repeat count is not an integer: {}", options[1].trim());
        }

        return new RepeatPatternConverter(result);
    }

    /**
     * Construct the converter.
     *
     * @param result  The repeated String
     *
     */
    private RepeatPatternConverter(final String result) {
        super("repeat", "repeat");
        this.result = result;
    }

    /**
     * Adds the repeated String to the buffer.
     *
     * @param obj      event to format, may not be null.
     * @param toAppendTo string buffer to which the formatted event will be appended.  May not be null.
     */
    public void format(final Object obj, final StringBuilder toAppendTo) {
        format(toAppendTo);
    }

    /**
     * Adds the repeated String to the buffer.
     *
     * @param event      event to format, may not be null.
     * @param toAppendTo string buffer to which the formatted event will be appended.  May not be null.
     */
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        format(toAppendTo);
    }

    private void format(final StringBuilder toAppendTo) {
        if (result != null) {
            toAppendTo.append(result);
        }
    }
}
