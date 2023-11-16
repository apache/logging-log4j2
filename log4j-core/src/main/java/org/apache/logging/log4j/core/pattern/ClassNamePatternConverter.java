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
 * Formats the class name of the site of the logging request.
 */
@Plugin(name = "ClassNamePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"C", "class"})
public final class ClassNamePatternConverter extends NamePatternConverter implements LocationAware {

    private static final String NA = "?";

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private ClassNamePatternConverter(final String[] options) {
        super("Class Name", "class name", options);
    }

    /**
     * Gets an instance of ClassNamePatternConverter.
     *
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static ClassNamePatternConverter newInstance(final String[] options) {
        return new ClassNamePatternConverter(options);
    }

    /**
     * Format a logging event.
     *
     * @param event      event to format.
     * @param toAppendTo string buffer to which class name will be appended.
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final StackTraceElement element = event.getSource();
        if (element == null) {
            toAppendTo.append(NA);
        } else {
            abbreviate(element.getClassName(), toAppendTo);
        }
    }

    @Override
    public boolean requiresLocation() {
        return true;
    }
}
