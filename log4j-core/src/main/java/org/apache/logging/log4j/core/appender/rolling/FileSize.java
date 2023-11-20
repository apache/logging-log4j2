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
package org.apache.logging.log4j.core.appender.rolling;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * FileSize utility class.
 */
public final class FileSize {

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final long KB = 1024;
    private static final long MB = KB * KB;
    private static final long GB = KB * MB;
    private static final long TB = KB * GB;

    /**
     * Pattern for string parsing.
     */
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("([0-9]+([.,][0-9]+)?)\\s*(|K|M|G|T)B?", Pattern.CASE_INSENSITIVE);

    private FileSize() {}

    /**
     * Converts a string to a number of bytes. Strings consist of a floating point value followed by
     * K, M, or G for kilobytes, megabytes, gigabytes, respectively. The
     * abbreviations KB, MB, and GB are also accepted. Matching is case insensitive.
     *
     * @param string The string to convert
     * @param defaultValue The default value if a problem is detected parsing.
     * @return The Bytes value for the string
     */
    public static long parse(final String string, final long defaultValue) {
        final Matcher matcher = VALUE_PATTERN.matcher(string);

        // Valid input?
        if (matcher.matches()) {
            try {

                // Read the quantity.
                final String quantityString = matcher.group(1);
                final double quantity = NumberFormat.getNumberInstance(Locale.ROOT)
                        .parse(quantityString)
                        .doubleValue();

                // Read the unit.
                final String unit = matcher.group(3);

                // Calculate the number of bytes.
                if (unit == null || unit.isEmpty()) {
                    return (long) quantity;
                } else if (unit.equalsIgnoreCase("K")) {
                    return (long) (quantity * KB);
                } else if (unit.equalsIgnoreCase("M")) {
                    return (long) (quantity * MB);
                } else if (unit.equalsIgnoreCase("G")) {
                    return (long) (quantity * GB);
                } else if (unit.equalsIgnoreCase("T")) {
                    return (long) (quantity * TB);
                } else {
                    LOGGER.error("FileSize units not recognized: " + string);
                    return defaultValue;
                }

            } catch (final ParseException error) {
                LOGGER.error("FileSize unable to parse numeric part: " + string, error);
                return defaultValue;
            }
        }

        // Invalid input, bail out.
        LOGGER.error("FileSize unable to parse bytes: " + string);
        return defaultValue;
    }
}
