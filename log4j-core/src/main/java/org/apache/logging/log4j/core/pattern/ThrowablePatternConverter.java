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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableFormatOptions;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Outputs certain information extracted from the {@link Throwable} associated with a {@link LogEvent}.
 */
@NullMarked
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

    protected final ThrowableFormatOptions options;

    private final ThrowableRenderer renderer;

    /**
     * @deprecated Use {@link #ThrowablePatternConverter(String, String, String[], Configuration, ThrowablePropertyRendererFactory, ThrowableStackTraceRendererFactory)} instead.
     */
    @Deprecated
    protected ThrowablePatternConverter(final String name, final String style, @Nullable final String[] options) {
        this(name, style, options, null, null, null);
    }

    /**
     * @deprecated Use {@link #ThrowablePatternConverter(String, String, String[], Configuration, ThrowablePropertyRendererFactory, ThrowableStackTraceRendererFactory)} instead.
     */
    @Deprecated
    protected ThrowablePatternConverter(
            final String name,
            final String style,
            @Nullable final String[] options,
            @Nullable final Configuration config) {
        this(name, style, options, config, null, null);
    }

    /**
     * The canonical constructor.
     *
     * @param name name of the converter
     * @param style CSS style for output
     * @param options array of options
     * @param config a configuration
     * @param stackTraceRendererFactory a renderer factory
     * @since 2.25.0
     */
    ThrowablePatternConverter(
            final String name,
            final String style,
            @Nullable final String[] options,
            @Nullable final Configuration config,
            @Nullable final ThrowablePropertyRendererFactory propertyRendererFactory,
            @Nullable final ThrowableStackTraceRendererFactory stackTraceRendererFactory) {

        // Process `name`, `style`, and `options`
        super(name, style);
        this.options = ThrowableFormatOptions.newInstance(options);

        // Determine the effective line separator
        final List<PatternFormatter> suffixFormatters = new ArrayList<>();
        this.effectiveLineSeparatorProvider = createEffectiveLineSeparator(
                this.options.getSeparator(), this.options.getSuffix(), config, suffixFormatters);
        this.formatters = Collections.unmodifiableList(suffixFormatters);

        // Create the effective renderer
        this.renderer =
                createEffectiveRenderer(options, this.options, propertyRendererFactory, stackTraceRendererFactory);
    }

    /**
     * Creates an instance of the class.
     *
     * @param config a configuration
     * @param options the pattern options
     * @return a new instance
     */
    public static ThrowablePatternConverter newInstance(
            @Nullable final Configuration config, @Nullable final String[] options) {
        return new ThrowablePatternConverter("Throwable", "throwable", options, config, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(final LogEvent event, final StringBuilder buffer) {
        requireNonNull(event, "event");
        requireNonNull(buffer, "buffer");
        final Throwable throwable = event.getThrown();
        if (throwable != null) {
            final String lineSeparator = effectiveLineSeparatorProvider.apply(event);
            renderer.renderThrowable(buffer, throwable, lineSeparator);
        }
    }

    /**
     * Indicates this converter handles {@link Throwable}s.
     *
     * @return {@code true}
     */
    @Override
    public boolean handlesThrowable() {
        return true;
    }

    public ThrowableFormatOptions getOptions() {
        return options;
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
     * @param suffixFormatters the list of pattern formatters employed to format the suffix
     * @return a lambda that returns the <em>effective</em> line separator by concatenating the formatted {@code suffix} with the {@code separator}
     */
    private static Function<LogEvent, String> createEffectiveLineSeparator(
            final String separator,
            @Nullable final String suffix,
            @Nullable final Configuration config,
            final List<PatternFormatter> suffixFormatters) {
        requireNonNull(separator, "separator");
        requireNonNull(suffixFormatters, "suffixFormatters");
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

    private static ThrowableRenderer createEffectiveRenderer(
            final String[] rawOptions,
            final ThrowableFormatOptions options,
            @Nullable final ThrowablePropertyRendererFactory propertyRendererFactory,
            @Nullable final ThrowableStackTraceRendererFactory stackTraceRendererFactory) {

        // Try to create a property renderer first
        final ThrowablePropertyRendererFactory effectivePropertyRendererFactory =
                propertyRendererFactory != null ? propertyRendererFactory : ThrowablePropertyRendererFactory.INSTANCE;
        final ThrowableRenderer propertyRenderer = effectivePropertyRendererFactory.createPropertyRenderer(rawOptions);
        if (propertyRenderer != null) {
            return propertyRenderer;
        }

        // Create a stack trace renderer
        final ThrowableStackTraceRendererFactory effectiveStackTraceRendererFactory = stackTraceRendererFactory != null
                ? stackTraceRendererFactory
                : ThrowableStackTraceRendererFactory.INSTANCE;
        return effectiveStackTraceRendererFactory.createStackTraceRenderer(options);
    }

    /**
     * Returns the formatted suffix pattern.
     *
     * @param logEvent the log event to use while formatting the suffix pattern
     * @return the formatted suffix
     * @deprecated Planned to be removed without a replacement
     */
    @Deprecated
    protected String getSuffix(final LogEvent logEvent) {
        requireNonNull(logEvent, "logEvent");
        final String effectiveLineSeparator = effectiveLineSeparatorProvider.apply(logEvent);
        if (options.getSeparator().equals(effectiveLineSeparator)) {
            return "";
        }
        return effectiveLineSeparator.substring(
                // Skip whitespace prefix:
                1,
                // Remove the separator:
                effectiveLineSeparator.length() - options.getSeparator().length());
    }
}
