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

import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Formats a date by delegating to {@link DatePatternConverter}.  The default
 * date pattern for a %d specifier in a file name is different than
 * the %d pattern in pattern layout.
 */
@Namespace("FileConverter")
@Plugin("FileDatePatternConverter")
@ConverterKeys({ "d", "date" })
@PerformanceSensitive("allocation")
public final class FileDatePatternConverter implements ArrayPatternConverter {

    private final DatePatternConverter delegate;

    /**
     * Private constructor.
     */
    private FileDatePatternConverter(final String... options) {
        delegate = DatePatternConverter.newInstance(options);
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param options options, may be null.
     * @return instance of pattern converter.
     */
    public static FileDatePatternConverter newInstance(final String[] options) {
        if (options == null || options.length == 0) {
            return new FileDatePatternConverter("yyyy-MM-dd");
        }

        return new FileDatePatternConverter(options);
    }

    @Override
    public void format(final Object obj, final StringBuilder toAppendTo) {
        delegate.format(obj, toAppendTo);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getStyleClass(final Object e) {
        return delegate.getStyleClass(e);
    }

    @Override
    public void format(final StringBuilder toAppendTo, final Object... objects) {
        delegate.format(toAppendTo, objects);
    }

    public String getPattern() {
        return delegate.getPattern();
    }
}
