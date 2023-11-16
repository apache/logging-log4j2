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
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.util.Strings;

/**
 * Outputs the Throwable portion of the LoggingEvent as a full stack trace
 * unless this converter's option is 'short', where it just outputs the first line of the trace, or if
 * the number of lines to print is explicitly specified.
 * <p>
 * The extended stack trace will also include the location of where the class was loaded from and the
 * version of the jar if available.
 */
@Plugin(name = "RootThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"rEx", "rThrowable", "rException"})
public final class RootThrowablePatternConverter extends ThrowablePatternConverter {

    /**
     * Private constructor.
     *
     * @param config the Configuration or {@code null}
     * @param options Options, may be null.
     */
    private RootThrowablePatternConverter(final Configuration config, final String[] options) {
        super("RootThrowable", "throwable", options, config);
    }

    /**
     * Gets an instance of the class.
     *
     * @param config The Configuration or {@code null}.
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static RootThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
        return new RootThrowablePatternConverter(config, options);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final ThrowableProxy proxy = event.getThrownProxy();
        final Throwable throwable = event.getThrown();
        if (throwable != null && options.anyLines()) {
            if (proxy == null) {
                super.format(event, toAppendTo);
                return;
            }
            final String trace = proxy.getCauseStackTraceAsString(
                    options.getIgnorePackages(), options.getTextRenderer(), getSuffix(event), options.getSeparator());
            final int len = toAppendTo.length();
            if (len > 0 && !Character.isWhitespace(toAppendTo.charAt(len - 1))) {
                toAppendTo.append(' ');
            }
            if (!options.allLines() || !Strings.LINE_SEPARATOR.equals(options.getSeparator())) {
                final StringBuilder sb = new StringBuilder();
                final String[] array = trace.split(Strings.LINE_SEPARATOR);
                final int limit = options.minLines(array.length) - 1;
                for (int i = 0; i <= limit; ++i) {
                    sb.append(array[i]);
                    if (i < limit) {
                        sb.append(options.getSeparator());
                    }
                }
                toAppendTo.append(sb.toString());

            } else {
                toAppendTo.append(trace);
            }
        }
    }
}
