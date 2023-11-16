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

import java.util.Objects;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.MapMessage.MapFormat;

/**
 * Able to handle the contents of the LogEvent's MapMessage and either
 * output the entire contents of the properties in a similar format to the
 * java.util.Hashtable.toString(), or to output the value of a specific key
 * within the Map.
 */
@Plugin(name = "MapPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"K", "map", "MAP"})
public final class MapPatternConverter extends LogEventPatternConverter {

    private static final String JAVA_UNQUOTED = MapFormat.JAVA_UNQUOTED.name();

    /**
     * Name of property to output.
     */
    private final String key;

    /**
     * Format to use when no key is provided.
     *
     * @see MapFormat
     * @since 2.11.2
     */
    private final String[] format;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private MapPatternConverter(final String[] options, final String... format) {
        super(options != null && options.length > 0 ? "MAP{" + options[0] + '}' : "MAP", "map");
        key = options != null && options.length > 0 ? options[0] : null;
        this.format = format;
    }

    /**
     * Obtains an instance of {@link MapPatternConverter}.
     *
     * @param options options, may be null or first element contains name of property to format.
     * @return instance of {@link MapPatternConverter}.
     */
    public static MapPatternConverter newInstance(final String[] options) {
        return new MapPatternConverter(options, JAVA_UNQUOTED);
    }

    /**
     * Obtain an instance of {@link MapPatternConverter}.
     *
     * @param options options, may be null or first element contains name of property to format.
     * @param format the format to use if no options are given (i.e., options is null). Ignored if options is non-null.
     * @return instance of {@link MapPatternConverter}.
     * @since 2.11.2
     */
    public static MapPatternConverter newInstance(final String[] options, final MapFormat format) {
        return new MapPatternConverter(options, Objects.toString(format, JAVA_UNQUOTED));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        MapMessage msg;
        if (event.getMessage() instanceof MapMessage) {
            msg = (MapMessage) event.getMessage();
        } else {
            return;
        }
        // if there is no additional options, we output every single
        // Key/Value pair for the Map in a similar format to Hashtable.toString()
        if (key == null) {
            msg.formatTo(format, toAppendTo);
        } else {
            // otherwise they just want a single key output
            final String val = msg.get(key);

            if (val != null) {
                toAppendTo.append(val);
            }
        }
    }
}
