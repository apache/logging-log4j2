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
package org.apache.logging.log4j.core.pattern;

import java.lang.management.ManagementFactory;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Returns the relative time in milliseconds since JVM Startup.
 */
@Plugin(name = "RelativeTimePatternConverter", category = "Converter")
@ConverterKeys({"r", "relative" })
public class RelativeTimePatternConverter extends LogEventPatternConverter {
    /**
     * Cached formatted timestamp.
     */
    private long lastTimestamp = Long.MIN_VALUE;
    private final long startTime = ManagementFactory.getRuntimeMXBean().getStartTime();
    private String relative;

    /**
     * Private constructor.
     */
    public RelativeTimePatternConverter() {
        super("Time", "time");
    }

    /**
     * Obtains an instance of RelativeTimePatternConverter.
     *
     * @param options options, currently ignored, may be null.
     * @return instance of RelativeTimePatternConverter.
     */
    public static RelativeTimePatternConverter newInstance(
        final String[] options) {
        return new RelativeTimePatternConverter();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final long timestamp = event.getTimeMillis();

        synchronized (this) {
            if (timestamp != lastTimestamp) {
                lastTimestamp = timestamp;
                relative = Long.toString(timestamp - startTime);
            }
        }
        toAppendTo.append(relative);
    }
}
