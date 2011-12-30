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
    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private RootThrowablePatternConverter(final String[] options) {
        super("RootThrowable", "throwable", options);
    }

    /**
     * Gets an instance of the class.
     *
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static RootThrowablePatternConverter newInstance(
        final String[] options) {
        return new RootThrowablePatternConverter(options);
    }

    /**
     * {@inheritDoc}
     */
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        Throwable throwable = event.getThrown();
        if (throwable != null) {
            if (!(throwable instanceof ThrowableProxy)) {
                super.format(event, toAppendTo);
                return;
            }
            ThrowableProxy t = (ThrowableProxy) throwable;
            String trace = t.getRootCauseStackTrace();
            if (lines > 0) {
                StringBuilder sb = new StringBuilder();
                String[] array = trace.split("\n");
                for (int i = 0; i < lines; ++i) {
                    sb.append(array[i]).append("\n");
                }
                toAppendTo.append(sb.toString());

            } else {
                toAppendTo.append(trace);
            }
        }
    }
}
