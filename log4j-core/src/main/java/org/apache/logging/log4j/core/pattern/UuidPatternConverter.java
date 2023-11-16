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

import java.util.UUID;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.UuidUtil;

/**
 * Formats the event sequence number.
 */
@Plugin(name = "UuidPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"u", "uuid"})
public final class UuidPatternConverter extends LogEventPatternConverter {

    private final boolean isRandom;

    /**
     * Private constructor.
     */
    private UuidPatternConverter(final boolean isRandom) {
        super("u", "uuid");
        this.isRandom = isRandom;
    }

    /**
     * Obtains an instance of SequencePatternConverter.
     *
     * @param options options, currently ignored, may be null.
     * @return instance of SequencePatternConverter.
     */
    public static UuidPatternConverter newInstance(final String[] options) {
        if (options.length == 0) {
            return new UuidPatternConverter(false);
        }

        if (options.length > 1 || (!options[0].equalsIgnoreCase("RANDOM") && !options[0].equalsIgnoreCase("Time"))) {
            LOGGER.error("UUID Pattern Converter only accepts a single option with the value \"RANDOM\" or \"TIME\"");
        }
        return new UuidPatternConverter(options[0].equalsIgnoreCase("RANDOM"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final UUID uuid = isRandom ? UUID.randomUUID() : UuidUtil.getTimeBasedUuid();
        toAppendTo.append(uuid.toString());
    }
}
