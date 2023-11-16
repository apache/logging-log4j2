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

/**
 * LoggingEventPatternConverter is a base class for pattern converters
 * that can format information from instances of LoggingEvent.
 */
public abstract class LogEventPatternConverter extends AbstractPatternConverter {

    /**
     * Constructs an instance of LoggingEventPatternConverter.
     *
     * @param name  name of converter.
     * @param style CSS style for output.
     */
    protected LogEventPatternConverter(final String name, final String style) {
        super(name, style);
    }

    /**
     * Formats an event into a string buffer.
     *
     * @param event      event to format, may not be null.
     * @param toAppendTo string buffer to which the formatted event will be appended.  May not be null.
     */
    public abstract void format(final LogEvent event, final StringBuilder toAppendTo);

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final Object obj, final StringBuilder output) {
        if (obj instanceof LogEvent) {
            format((LogEvent) obj, output);
        }
    }

    /**
     * Normally pattern converters are not meant to handle Exceptions although few pattern converters might.
     * <p>
     * By examining the return values for this method, the containing layout will determine whether it handles
     * throwables or not.
     * </p>
     *
     * @return true if this PatternConverter handles throwables
     */
    public boolean handlesThrowable() {
        return false;
    }

    public boolean isVariable() {
        return true;
    }

    /**
     * @return the formatted output when the input variable (MDC, NDC, etc.) is empty
     */
    public String emptyVariableOutput() {
        return "";
    }
}
