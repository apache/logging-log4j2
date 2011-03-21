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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;


/**
 * Return the event's level in a StringBuffer.
 */
@Plugin(name = "LevelPatternConverter", type = "Converter")
@ConverterKeys({"p", "level"})
public final class LevelPatternConverter extends LogEventPatternConverter {
    /**
     * Singleton.
     */
    private static final LevelPatternConverter INSTANCE =
        new LevelPatternConverter();

    /**
     * Private constructor.
     */
    private LevelPatternConverter() {
        super("Level", "level");
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static LevelPatternConverter newInstance(
        final String[] options) {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    public void format(final LogEvent event, final StringBuilder output) {
        output.append(event.getLevel().toString());
    }

    /**
     * {@inheritDoc}
     */
    public String getStyleClass(Object e) {
        if (e instanceof LogEvent) {
            Level level = ((LogEvent) e).getLevel();

            switch (level) {
                case TRACE:
                    return "level trace";

                case DEBUG:
                    return "level debug";

                case INFO:
                    return "level info";

                case WARN:
                    return "level warn";

                case ERROR:
                    return "level error";

                case FATAL:
                    return "level fatal";

                default:
                    return "level " + ((LogEvent) e).getLevel().toString();
            }
        }

        return "level";
    }
}
