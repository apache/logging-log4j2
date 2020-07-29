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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.util.Strings;

import java.util.List;


/**
 * Returns the event's NDC in a StringBuilder.
 */
@Plugin(name = "Log4j1NdcPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "ndc" })
public final class Log4j1NdcPatternConverter extends LogEventPatternConverter {
    /**
     * Singleton.
     */
    private static final Log4j1NdcPatternConverter INSTANCE =
        new Log4j1NdcPatternConverter();

    /**
     * Private constructor.
     */
    private Log4j1NdcPatternConverter() {
        super("Log4j1NDC", "ndc");
    }

    /**
     * Obtains an instance of NdcPatternConverter.
     *
     * @param options options, may be null.
     * @return instance of NdcPatternConverter.
     */
    public static Log4j1NdcPatternConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final List<String> ndc = event.getContextStack().asList();
        toAppendTo.append(Strings.join(ndc, ' '));
    }
}
