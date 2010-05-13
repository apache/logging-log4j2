/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
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

package org.apache.logging.log4j.core.layout.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * Outputs the ThrowableInformation portion of the LoggingiEvent as a full stacktrace
 * unless this converter's option is 'short', where it just outputs the first line of the trace.
 */
@Plugin(name = "ThrowablePatternConverter", type = "Converter")
@ConverterKeys({"ex", "throwable"})
public class ThrowablePatternConverter extends LogEventPatternConverter {
    /**
     * If "short", only first line of throwable report will be formatted.
     */
    private final String option;

    /**
     * Private constructor.
     *
     * @param options options, may be null.
     */
    private ThrowablePatternConverter(final String[] options) {
        super("Throwable", "throwable");

        if ((options != null) && (options.length > 0)) {
            option = options[0];
        } else {
            option = null;
        }
    }

    /**
     * Gets an instance of the class.
     *
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static ThrowablePatternConverter newInstance(
        final String[] options) {
        return new ThrowablePatternConverter(options);
    }

    /**
     * {@inheritDoc}
     */
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        Throwable t = event.getThrown();

        if (t != null) {
            if (option == null || option.equals("full")) {
                StringWriter w = new StringWriter();
                t.printStackTrace(new PrintWriter(w));
                toAppendTo.append(w.toString());
            } else {
                StackTraceElement[] e = t.getStackTrace();
                toAppendTo.append(t.toString()).append(" at ").append(e[0].toString());
            }
        }
    }

    /**
     * This converter obviously handles throwables.
     *
     * @return true.
     */
    public boolean handlesThrowable() {
        return true;
    }
}
