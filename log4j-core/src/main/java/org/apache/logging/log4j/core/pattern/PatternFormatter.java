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
import org.apache.logging.log4j.core.impl.LocationAware;

/**
 *
 */
public class PatternFormatter {

    /**
     * The empty array.
     */
    public static final PatternFormatter[] EMPTY_ARRAY = {};

    private final LogEventPatternConverter converter;
    private final FormattingInfo field;
    private final boolean skipFormattingInfo;

    public PatternFormatter(final LogEventPatternConverter converter, final FormattingInfo field) {
        this.converter = converter;
        this.field = field;
        this.skipFormattingInfo = field == FormattingInfo.getDefault();
    }

    public void format(final LogEvent event, final StringBuilder buf) {
        if (skipFormattingInfo) {
            converter.format(event, buf);
        } else {
            formatWithInfo(event, buf);
        }
    }

    private void formatWithInfo(final LogEvent event, final StringBuilder buf) {
        final int startField = buf.length();
        converter.format(event, buf);
        field.format(startField, buf);
    }

    public LogEventPatternConverter getConverter() {
        return converter;
    }

    public FormattingInfo getFormattingInfo() {
        return field;
    }

    /**
     * Normally pattern formatters are not meant to handle Exceptions although few pattern formatters might.
     * <p>
     * By examining the return values for this method, the containing layout will determine whether it handles
     * throwables or not.
     * </p>
     *
     * @return true if this PatternConverter handles throwables
     */
    public boolean handlesThrowable() {
        return converter.handlesThrowable();
    }

    /**
     * Most pattern formatters do not use location information. When they do they should return true here
     * so that the logging system can efficiently capture it.
     * @return true if location information is required.
     */
    public boolean requiresLocation() {
        return converter instanceof LocationAware && ((LocationAware) converter).requiresLocation();
    }

    /**
     * Returns a String suitable for debugging.
     *
     * @return a String suitable for debugging.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[converter=");
        sb.append(converter);
        sb.append(", field=");
        sb.append(field);
        sb.append(']');
        return sb.toString();
    }
}
