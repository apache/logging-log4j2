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
 * Formats a UUID.
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
     * Creates an instance of {@link UuidPatternConverter}.
     * <p>
     * The {@code RANDOM} option generates a Type 4 (pseudo-randomly generated) UUID.
     * The UUID is generated using a cryptographically strong pseudo-random number generator.
     * <p>
     * The {@code TIME} option generates a Type 1 (date and time based) UUID using the local network interface's MAC address.
     * To ensure uniqueness across multiple JVMs and/or class loaders on the same host, a random number between 0 and 16384 will be associated with each instance of the UUID generator class, and included in each time-based UUID generated.
     * See {@link UuidUtil#UUID_SEQUENCE} how to seed the UUID generation with an integer value.
     * Because time-based UUIDs contain the MAC address and timestamp, they should be used with care.
     *
     * @param options An array containing either {@code RANDOM} or {@code TIME}.
     * If empty, {@code TIME} will be used.
     * @return a new {@link UuidPatternConverter} instance
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
