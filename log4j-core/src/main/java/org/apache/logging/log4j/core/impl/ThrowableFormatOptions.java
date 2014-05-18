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
package org.apache.logging.log4j.core.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Patterns;

/**
 * Contains options which control how a {@link Throwable} pattern is formatted.
 */
public final class ThrowableFormatOptions {

    private static final int DEFAULT_LINES = Integer.MAX_VALUE;

    /**
     * Default instance of {@code ThrowableFormatOptions}.
     */
    protected static final ThrowableFormatOptions DEFAULT = new ThrowableFormatOptions();

    /**
     * Format the whole stack trace.
     */
    private static final String FULL = "full";

    /**
     * Do not format the exception.
     */
    private static final String NONE = "none";

    /**
     * Format only the first line of the throwable.
     */
    private static final String SHORT = "short";

    /**
     * The number of lines to write.
     */
    private final int lines;

    /**
     * The stack trace separator.
     */
    private final String separator;

    /**
     * The list of packages to filter.
     */
    private final List<String> packages;

    public static final String CLASS_NAME = "short.className";
    public static final String METHOD_NAME = "short.methodName";
    public static final String LINE_NUMBER = "short.lineNumber";
    public static final String FILE_NAME = "short.fileName";
    public static final String MESSAGE = "short.message";
    public static final String LOCALIZED_MESSAGE = "short.localizedMessage";

    /**
     * Construct the options for printing stack trace.
     * @param lines The number of lines.
     * @param separator The stack trace separator.
     * @param packages The packages to filter.
     */
    protected ThrowableFormatOptions(final int lines, final String separator, final List<String> packages) {
        this.lines = lines;
        this.separator = separator == null ? Constants.LINE_SEPARATOR : separator;
        this.packages = packages;
    }

    /**
     * Construct the options for printing stack trace.
     * @param packages The packages to filter.
     */
    protected ThrowableFormatOptions(final List<String> packages) {
        this(DEFAULT_LINES, null, packages);
    }

    /**
     * Construct the options for printing stack trace.
     */
    protected ThrowableFormatOptions() {
        this(DEFAULT_LINES, null, null);
    }

    /**
     * Returns the number of lines to write.
     * @return The number of lines to write.
     */
    public int getLines() {
        return this.lines;
    }

    /**
     * Returns the stack trace separator.
     * @return The stack trace separator.
     */
    public String getSeparator() {
        return this.separator;
    }

    /**
     * Returns the list of packages to filter.
     * @return The list of packages to filter.
     */
    public List<String> getPackages() {
        return this.packages;
    }

    /**
     * Determines if all lines should be printed.
     * @return true for all lines, false otherwise.
     */
    public boolean allLines() {
        return this.lines == DEFAULT_LINES;
    }

    /**
     * Determines if any lines should be printed.
     * @return true for any lines, false otherwise.
     */
    public boolean anyLines() {
        return this.lines > 0;
    }

    /**
     * Returns the minimum between the lines and the max lines.
     * @param maxLines The maximum number of lines.
     * @return The number of lines to print.
     */
    public int minLines(final int maxLines) {
        return this.lines > maxLines ? maxLines : this.lines;
    }

    /**
     * Determines if there are any packages to filter.
     * @return true if there are packages, false otherwise.
     */
    public boolean hasPackages() {
        return this.packages != null && !this.packages.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder();
        s.append('{').append(allLines() ? FULL : this.lines == 2 ? SHORT : anyLines() ? String.valueOf(this.lines) : NONE).append('}');
        s.append("{separator(").append(this.separator).append(")}");
        if (hasPackages()) {
            s.append("{filters(");
            for (final String p : this.packages) {
                s.append(p).append(',');
            }
            s.deleteCharAt(s.length() - 1);
            s.append(")}");
        }
        return s.toString();
    }

    /**
     * Create a new instance based on the array of options.
     * @param options The array of options.
     */
    public static ThrowableFormatOptions newInstance(String[] options) {
        if (options == null || options.length == 0) {
            return DEFAULT;
        }
        // NOTE: The following code is present for backward compatibility
        // and was copied from Extended/RootThrowablePatternConverter.
        // This supports a single option with the format:
        //     %xEx{["none"|"short"|"full"|depth],[filters(packages)}
        // However, the convention for multiple options should be:
        //     %xEx{["none"|"short"|"full"|depth]}[{filters(packages)}]
        if (options.length == 1 && options[0] != null && options[0].length() > 0) {
            final String[] opts = options[0].split(Patterns.COMMA_SEPARATOR, 2);
            final String first = opts[0].trim();
            final Scanner scanner = new Scanner(first);
            if (opts.length > 1 && (first.equalsIgnoreCase(FULL) || first.equalsIgnoreCase(SHORT) || first.equalsIgnoreCase(NONE) || scanner.hasNextInt())) {
                options = new String[]{first, opts[1].trim()};
            }
            scanner.close();
        }

        int lines = DEFAULT.lines;
        String separator = DEFAULT.separator;
        List<String> packages = DEFAULT.packages;
        for (String rawOption : options) {
            if (rawOption != null) {
                final String option = rawOption.trim();
                if (option.isEmpty()) {
                    // continue;
                } else if (option.startsWith("separator(") && option.endsWith(")")) {
                    separator = option.substring("separator(".length(), option.length() - 1);
                } else if (option.startsWith("filters(") && option.endsWith(")")) {
                    final String filterStr = option.substring("filters(".length(), option.length() - 1);
                    if (filterStr.length() > 0) {
                        final String[] array = filterStr.split(Patterns.COMMA_SEPARATOR);
                        if (array.length > 0) {
                            packages = new ArrayList<String>(array.length);
                            for (String token : array) {
                                token = token.trim();
                                if (token.length() > 0) {
                                    packages.add(token);
                                }
                            }
                        }
                    }
                } else if (option.equalsIgnoreCase(NONE)) {
                    lines = 0;
                } else if (option.equalsIgnoreCase(SHORT) || option.equalsIgnoreCase(CLASS_NAME) ||
                        option.equalsIgnoreCase(METHOD_NAME) || option.equalsIgnoreCase(LINE_NUMBER) ||
                        option.equalsIgnoreCase(FILE_NAME) || option.equalsIgnoreCase(MESSAGE) ||
                        option.equalsIgnoreCase(LOCALIZED_MESSAGE)) {
                    lines = 2;
                } else if (!option.equalsIgnoreCase(FULL)) {
                    lines = Integer.parseInt(option);
                }
            }
        }
        return new ThrowableFormatOptions(lines, separator, packages);
    }
}
