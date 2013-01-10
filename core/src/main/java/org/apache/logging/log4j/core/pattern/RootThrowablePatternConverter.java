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
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


/**
 * Outputs the Throwable portion of the LoggingEvent as a full stacktrace
 * unless this converter's option is 'short', where it just outputs the first line of the trace, or if
 * the number of lines to print is explicitly specified.
 * <p>
 * The extended stack trace will also include the location of where the class was loaded from and the
 * version of the jar if available.
 */
@Plugin(name = "RootThrowablePatternConverter", type = "Converter")
@ConverterKeys({"rEx", "rThrowable", "rException" })
public final class RootThrowablePatternConverter extends ThrowablePatternConverter {

    private static final String FILTERS = "filters(";

    private final List<String> packages;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private RootThrowablePatternConverter(final String[] options) {
        super("RootThrowable", "throwable", options);
        List<String> tempPackages = null;
        if (options != null && options.length > 1) {
            if (options[1].startsWith(FILTERS) && options[1].endsWith(")")) {
                final String filterStr = options[1].substring(FILTERS.length(), options[1].length() - 1);
                final String[] array = filterStr.split(",");
                if (array.length > 0) {
                    tempPackages = new ArrayList<String>(array.length);
                    for (final String token : array) {
                        tempPackages.add(token.trim());
                    }
                }
            }
        }
        packages = tempPackages;
    }

    /**
     * Gets an instance of the class.
     *
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static RootThrowablePatternConverter newInstance(final String[] options) {
        String type = null;
        String[] array = options;
        if (options != null && options.length == 1 && options[0].length() > 0) {
            final String[] opts = options[0].split(",", 2);
            final String first = opts[0].trim();
            String filter;
            final Scanner scanner = new Scanner(first);
            if (first.equalsIgnoreCase(FULL) || first.equalsIgnoreCase(SHORT) || scanner.hasNextInt()) {
                type = first;
                filter = opts[1].trim();
            } else {
                filter = options[0].trim();
            }
            array = new String[] {type, filter};
        }

        return new RootThrowablePatternConverter(array);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final Throwable throwable = event.getThrown();
        if (throwable != null && lines > 0) {
            if (!(throwable instanceof ThrowableProxy)) {
                super.format(event, toAppendTo);
                return;
            }
            final ThrowableProxy t = (ThrowableProxy) throwable;
            final String trace = t.getRootCauseStackTrace(packages);
            final int len = toAppendTo.length();
            if (len > 0 && !Character.isWhitespace(toAppendTo.charAt(len - 1))) {
                toAppendTo.append(" ");
            }
            if (lines != Integer.MAX_VALUE) {
                final StringBuilder sb = new StringBuilder();
                final String[] array = trace.split("\n");
                final int limit = lines > array.length ? array.length : lines;
                for (int i = 0; i < limit; ++i) {
                    sb.append(array[i]).append("\n");
                }
                toAppendTo.append(sb.toString());

            } else {
                toAppendTo.append(trace);
            }
        }
    }
}
