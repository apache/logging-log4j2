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
import org.apache.logging.log4j.core.util.UuidUtil;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

/**
 * Formats the event sequence number.
 */
@Namespace(PatternConverter.CATEGORY)
@Plugin("UuidPatternConverter")
@ConverterKeys({"u", "uuid"})
public final class UuidPatternConverter extends LogEventPatternConverter {
    private enum UuidType {
        TIME,
        RANDOM,
        HASH
    }

    private final UuidType uuidType;

    /**
     * Private constructor.
     */
    private UuidPatternConverter(final UuidType uuidType) {
        super("u", "uuid");
        this.uuidType = uuidType;
    }

    /**
     * Obtains an instance of UuidPatternConverter.
     *
     * @param options options
     * @return instance of UuidPatternConverter.
     */
    public static UuidPatternConverter newInstance(final String[] options) {
        if (options.length == 0) {
            return new UuidPatternConverter(UuidType.TIME);
        }

        if (options.length == 1) {
            switch (options[0].toUpperCase()) {
                case "TIME":
                    return new UuidPatternConverter(UuidType.TIME);
                case "RANDOM":
                    return new UuidPatternConverter(UuidType.RANDOM);
                case "HASH":
                    return new UuidPatternConverter(UuidType.HASH);
            }
        }

        LOGGER.error(
                "UUID Pattern Converter only accepts a single option with the value \"TIME\" or \"RANDOM\" or \"HASH\"");
        return new UuidPatternConverter(UuidType.TIME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final UUID uuid =
                switch (uuidType) {
                    case TIME -> UuidUtil.getTimeBasedUuid();
                    case RANDOM -> UUID.randomUUID();
                    case HASH -> UuidUtil.getHashBasedUuid(event);
                };
        toAppendTo.append(uuid.toString());
    }
}
