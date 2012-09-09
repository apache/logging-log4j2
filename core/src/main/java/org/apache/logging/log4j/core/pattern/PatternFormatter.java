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

import org.apache.logging.log4j.core.LogEvent;


/**
 *
 */
public class PatternFormatter {
    private LogEventPatternConverter converter;
    private FormattingInfo field;

    public PatternFormatter(LogEventPatternConverter converter, FormattingInfo field) {
        this.converter = converter;
        this.field = field;
    }

    public void format(LogEvent event, StringBuilder buf) {
        int startField = buf.length();
        converter.format(event, buf);
        field.format(startField, buf);
    }

    public LogEventPatternConverter getConverter() {
        return converter;
    }

    public FormattingInfo getFormattingInfo() {
        return field;
    }
}
