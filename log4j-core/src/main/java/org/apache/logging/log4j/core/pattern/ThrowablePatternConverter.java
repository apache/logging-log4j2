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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.StringBuilderWriter;
import org.apache.logging.log4j.util.Strings;

/**
 * Outputs the Throwable portion of the LoggingEvent as a full stack trace
 * unless this converter's option is 'short', where it just outputs the first line of the trace, or if
 * the number of lines to print is explicitly specified.
 */
@Plugin(name = "ThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"ex", "throwable", "exception"})
public class ThrowablePatternConverter extends LogEventPatternConverter {

    /**
     * Returns the list of formatters used to render the suffix.
     *
     * @deprecated Kept for binary backward compatibility.
     */
    @Deprecated
    protected final List<PatternFormatter> formatters;

    private final Function<LogEvent, String> effectiveLineSeparatorProvider;

    private String rawOption;

    private final boolean subShortOption;

    private ThrowableRenderer<ThrowableRenderer.Context> renderer;

    protected final ThrowableFormatOptions options;

    /**
     * Constructor.
     * @param name Name of converter.
     * @param style CSS style for output.
     * @param options options, may be null.
     * @deprecated Use ThrowablePatternConverter(String name, String stule, String[] options, Configuration config)
     */
    @Deprecated
    protected ThrowablePatternConverter(final String name, final String style, final String[] options) {
        this(name, style, options, null);
    }

    /**
     * Constructor.
     * @param name name of converter
     * @param style CSS style for output
     * @param options options, may be null.
     * @param config the Configuration or {@code null}
     */
    protected ThrowablePatternConverter(
            final String name, final String style, final String[] options, final Configuration config) {
        super(name, style);
        this.options = ThrowableFormatOptions.newInstance(options);
        if (options != null && options.length > 0) {
            rawOption = options[0];
        }
        final List<PatternFormatter> suffixFormatters = new ArrayList<>();
        this.effectiveLineSeparatorProvider = createEffectiveLineSeparator(
                this.options.getSeparator(), this.options.getSuffix(), config, suffixFormatters);
        this.formatters = Collections.unmodifiableList(suffixFormatters);
        subShortOption = ThrowableFormatOptions.MESSAGE.equalsIgnoreCase(rawOption)
                || ThrowableFormatOptions.LOCALIZED_MESSAGE.equalsIgnoreCase(rawOption)
                || ThrowableFormatOptions.FILE_NAME.equalsIgnoreCase(rawOption)
                || ThrowableFormatOptions.LINE_NUMBER.equalsIgnoreCase(rawOption)
                || ThrowableFormatOptions.METHOD_NAME.equalsIgnoreCase(rawOption)
                || ThrowableFormatOptions.CLASS_NAME.equalsIgnoreCase(rawOption);
        createRenderer(this.options);
    }

    /**
     * Gets an instance of the class.
     *
     * @param config The Configuration or {@code null}.
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static ThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
        return new ThrowablePatternConverter("Throwable", "throwable", options, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder buffer) {
        final Throwable throwable = event.getThrown();
        final String effectiveLineSeparator = effectiveLineSeparator(event);
        if (subShortOption) {
            formatSubShortOption(throwable, effectiveLineSeparator, buffer);
        } else if (throwable != null && options.anyLines()) {
            formatOption(throwable, effectiveLineSeparator, buffer);
        }
    }

    private void formatSubShortOption(final Throwable t, final String lineSeparator, final StringBuilder buffer) {
        StackTraceElement[] trace;
        StackTraceElement throwingMethod = null;
        int len;

        if (t != null) {
            trace = t.getStackTrace();
            if (trace != null && trace.length > 0) {
                throwingMethod = trace[0];
            }
        }

        if (t != null && throwingMethod != null) {
            String toAppend = Strings.EMPTY;

            if (ThrowableFormatOptions.CLASS_NAME.equalsIgnoreCase(rawOption)) {
                toAppend = throwingMethod.getClassName();
            } else if (ThrowableFormatOptions.METHOD_NAME.equalsIgnoreCase(rawOption)) {
                toAppend = throwingMethod.getMethodName();
            } else if (ThrowableFormatOptions.LINE_NUMBER.equalsIgnoreCase(rawOption)) {
                toAppend = String.valueOf(throwingMethod.getLineNumber());
            } else if (ThrowableFormatOptions.MESSAGE.equalsIgnoreCase(rawOption)) {
                toAppend = t.getMessage();
            } else if (ThrowableFormatOptions.LOCALIZED_MESSAGE.equalsIgnoreCase(rawOption)) {
                toAppend = t.getLocalizedMessage();
            } else if (ThrowableFormatOptions.FILE_NAME.equalsIgnoreCase(rawOption)) {
                toAppend = throwingMethod.getFileName();
            }

            len = buffer.length();
            if (len > 0 && !Character.isWhitespace(buffer.charAt(len - 1))) {
                buffer.append(' ');
            }
            buffer.append(toAppend);

            buffer.append(lineSeparator);
        }
    }

    @SuppressFBWarnings(
            value = "INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE",
            justification = "Formatting a throwable is the main purpose of this class.")
    private void formatOption(
            final Throwable throwable, final String effectiveLineSeparator, final StringBuilder buffer) {
        final int bufferLength = buffer.length();
        if (bufferLength > 0 && !Character.isWhitespace(buffer.charAt(bufferLength - 1))) {
            buffer.append(' ');
        }
        final boolean customRenderingRequired = isCustomRenderingRequired(effectiveLineSeparator);
        if (customRenderingRequired) {
            renderer.renderThrowable(buffer, throwable, effectiveLineSeparator);
        } else {
            throwable.printStackTrace(new PrintWriter(new StringBuilderWriter(buffer)));
        }
    }

    /**
     * This converter obviously handles throwables.
     *
     * @return true.
     */
    @Override
    public boolean handlesThrowable() {
        return true;
    }

    public ThrowableFormatOptions getOptions() {
        return options;
    }

    private boolean isCustomRenderingRequired(final String effectiveLineSeparator) {
        return !options.allLines() || !System.lineSeparator().equals(effectiveLineSeparator) || options.hasPackages();
    }

    /**
     * Creates a lambda that returns the <em>effective</em> line separator by concatenating the formatted {@code suffix} with the {@code separator}.
     * <p>
     * At the beginning, there was only {@code separator} used as a terminator at the end of every rendered line.
     * Its content was rendered literally without any processing.
     * </p>
     * <p>
     * Later on, {@code suffix} was added in <a href="https://github.com/apache/logging-log4j2/pull/61">#61</a>.
     * {@code suffix} is functionally identical to {@code separator} with the exception that it contains a Pattern Layout conversion pattern.
     * In an ideal world, {@code separator} should have been extended to accept patterns.
     * But without giving it a second of thought, just like almost any other Log4j feature, we cheerfully accepted the feature.
     * </p>
     * <p>
     * Given two overlapping features, how do we determine the <em>effective</em> line separator?
     * </p>
     * <pre>{@code
     * String effectiveLineSeparator(String separator, String suffix, LogEvent event) {
     *     String formattedSuffix = format(suffix, event);
     *     return isNotBlank(formattedSuffix)
     *            ? (' ' + formattedSuffix + lineSeparator)
     *            : lineSeparator;
     * }
     * }</pre>
     *
     * @param separator the user-provided {@code separator} option
     * @param suffix the user-provided {@code suffix} option containing a Pattern Layout conversion pattern
     * @param config the configuration to create the Pattern Layout conversion pattern parser
     * @param suffixFormatters the list of pattern formatters to format the suffix
     * @return a lambda that returns the <em>effective</em> line separator by concatenating the formatted {@code suffix} with the {@code separator}
     */
    private static Function<LogEvent, String> createEffectiveLineSeparator(
            final String separator,
            final String suffix,
            final Configuration config,
            final List<PatternFormatter> suffixFormatters) {
        if (suffix != null) {

            // Suffix is allowed to be a Pattern Layout conversion pattern, hence we need to parse it
            final PatternParser parser = PatternLayout.createPatternParser(config);
            final List<PatternFormatter> parsedSuffixFormatters = parser.parse(suffix);

            // Collect formatters excluding ones handling throwables
            for (final PatternFormatter suffixFormatter : parsedSuffixFormatters) {
                if (!suffixFormatter.handlesThrowable()) {
                    suffixFormatters.add(suffixFormatter);
                }
            }

            // Create the lambda accepting a `LogEvent` to invoke collected formatters
            return logEvent -> {
                final StringBuilder buffer = new StringBuilder();
                buffer.append(' ');
                for (PatternFormatter suffixFormatter : suffixFormatters) {
                    suffixFormatter.format(logEvent, buffer);
                }
                final boolean blankSuffix = buffer.length() == 1;
                if (blankSuffix) {
                    return separator;
                } else {
                    buffer.append(separator);
                    return buffer.toString();
                }
            };

        } else {
            return logEvent -> separator;
        }
    }

    void createRenderer(final ThrowableFormatOptions options) {
        this.renderer = new ThrowableRenderer<>(options.getIgnorePackages(), options.getLines());
    }

    /**
     * Returns the <em>effective</em> line separator by concatenating the formatted {@code suffix} with the {@code separator}.
     *
     * @param logEvent the log event to use while formatting the suffix pattern
     * @return the concatenation of the formatted {@code suffix} with the {@code separator}
     * @see #createEffectiveLineSeparator(String, String, Configuration, List)
     */
    String effectiveLineSeparator(final LogEvent logEvent) {
        return effectiveLineSeparatorProvider.apply(logEvent);
    }
}
