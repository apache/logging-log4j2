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

import java.util.List;

import org.apache.logging.log4j.core.LogEvent;

/**
 * Style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
 */
public abstract class AbstractStyleNameConverter extends LogEventPatternConverter {

    private final List<PatternFormatter> formatters;

    private final String style;
    
    /**
     * Constructs the converter.
     * 
     * @param formatters
     *            The PatternFormatters to generate the text to manipulate.
     * @param styling
     *            The styling that should encapsulate the pattern.
     */
    protected AbstractStyleNameConverter(String name, List<PatternFormatter> formatters, String styling) {
        super(name, "style");
        this.formatters = formatters;
        this.style = styling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        StringBuilder buf = new StringBuilder();
        for (PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        if (buf.length() > 0) {
            toAppendTo.append(style).append(buf.toString()).append(AnsiEscape.getDefaultStyle());
        }
    }
}
