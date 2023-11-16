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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.LocationAware;

/**
 * Returns the event's line location information in a StringBuilder.
 */
@Plugin(name = "MethodLocationPatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"M", "method"})
public final class MethodLocationPatternConverter extends LogEventPatternConverter implements LocationAware {
    /**
     * Singleton.
     */
    private static final MethodLocationPatternConverter INSTANCE = new MethodLocationPatternConverter();

    /**
     * Private constructor.
     */
    private MethodLocationPatternConverter() {
        super("Method", "method");
    }

    /**
     * Obtains an instance of MethodLocationPatternConverter.
     *
     * @param options options, may be null.
     * @return instance of MethodLocationPatternConverter.
     */
    public static MethodLocationPatternConverter newInstance(final String[] options) {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final StackTraceElement element = event.getSource();

        if (element != null) {
            toAppendTo.append(element.getMethodName());
        }
    }

    @Override
    public boolean requiresLocation() {
        return true;
    }
}
