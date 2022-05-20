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
package org.apache.log4j.pattern;

import org.apache.log4j.helpers.OptionConverter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

/**
 * Outputs the Log4j 1.x level name.
 */
@Namespace(PatternConverter.CATEGORY)
@Plugin("Log4j1LevelPatternConverter")
@ConverterKeys({ "v1Level" })
public class Log4j1LevelPatternConverter extends LogEventPatternConverter {

    private static final Log4j1LevelPatternConverter INSTANCE = new Log4j1LevelPatternConverter();

    public static Log4j1LevelPatternConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    private Log4j1LevelPatternConverter() {
        super("Log4j1Level", "v1Level");
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        toAppendTo.append(OptionConverter.convertLevel(event.getLevel()).toString());
    }

}
