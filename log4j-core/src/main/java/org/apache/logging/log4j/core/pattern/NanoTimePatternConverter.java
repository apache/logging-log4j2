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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Converts and formats the event's nanoTime in a StringBuilder.
 */
@Plugin(name = "NanoTimePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"N", "nano"})
@PerformanceSensitive("allocation")
public final class NanoTimePatternConverter extends LogEventPatternConverter {

    /**
     * Private constructor.
     *
     * @param options
     *            options, may be null.
     */
    private NanoTimePatternConverter(final String[] options) {
        super("Nanotime", "nanotime");
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options
     *            options, may be null.
     * @return instance of pattern converter.
     */
    public static NanoTimePatternConverter newInstance(final String[] options) {
        return new NanoTimePatternConverter(options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder output) {
        output.append(event.getNanoTime());
    }
}
