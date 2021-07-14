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

package org.apache.logging.log4j.core.appender.rolling;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse a file size expression into a number of bytes (long value).
 */
public final class FileSize {
    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * MB;

    /**
     * Pattern for string parsing.
     */
    private static final Pattern VALUE_PATTERN =
        Pattern.compile("([0-9]+([\\.,][0-9]+)?)\\s*(|K|M|G)B?", Pattern.CASE_INSENSITIVE);

    private FileSize() {
    }

    /**
     * Converts a string to a number of bytes.<br>
     * Strings consist of a floating-point value followed by K, M, or G
     * for kilobytes, megabytes, gigabytes, respectively.<br>
     * The abbreviations KB, MB, and GB are also accepted. Matching is case-insensitive.
     *
     * @param expr the string to convert
     * @return the file size as number of bytes parsed from the input string
     */
    public static long parse(final String expr) {
        Objects.requireNonNull(expr, "File size expression required");

        // treat comma as decimal separator unless comma and period appear in the expression
        final String expression = expr.contains(",") && !expr.contains(".") ? expr.replace(",", ".") : expr;

        final Matcher matcher = VALUE_PATTERN.matcher(expression);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported file size expression '" + expression + "'");
        }

        double dval;
        try {
            // Get double precision value
            dval = NumberFormat.getNumberInstance(Locale.ROOT).parse(matcher.group(1)).doubleValue();
        } catch (final Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
            throw new IllegalArgumentException("Failed to parse file size expression '" + expression + "'", ex);
        }

        // Get units specified
        final String units = matcher.group(3).toUpperCase();

        if (units.equals("K")) {
            dval *= KB;
        } else if (units.equals("M")) {
            dval *= MB;
        } else if (units.equals("G")) {
            dval *= GB;
        }
        final long lval = (long) dval;
        if (lval < 1) {
            throw new IllegalArgumentException("File size must be > 0");
        }
        return lval;
    }

}
