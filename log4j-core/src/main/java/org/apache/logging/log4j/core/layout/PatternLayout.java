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
package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.pattern.RegexReplacement;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.Strings;

/**
 * A flexible layout configurable with pattern string.
 * <p>
 * The goal of this class is to {@link Layout#toByteArray format} a {@link LogEvent} and
 * return the results. The format of the result depends on the <em>conversion pattern</em>.
 * </p>
 * <p>
 * The conversion pattern is closely related to the conversion pattern of the printf function in C. A conversion pattern
 * is composed of literal text and format control expressions called <em>conversion specifiers</em>.
 * </p>
 * <p>
 * See the Log4j Manual for details on the supported pattern converters.
 * </p>
 */
@Configurable(elementType = Layout.ELEMENT_TYPE, printObject = true)
@Plugin
public final class PatternLayout extends AbstractStringLayout {

    /**
     * Default pattern string for log output. Currently set to the string <b>"%m%n"</b> which just prints the
     * application supplied message.
     */
    public static final String DEFAULT_CONVERSION_PATTERN = "%m%n";

    /**
     * A conversion pattern equivalent to the TTCCLayout. Current value is <b>%r [%t] %p %c %notEmpty{%x }- %m%n</b>.
     */
    public static final String TTCC_CONVERSION_PATTERN = "%r [%t] %p %c %notEmpty{%x }- %m%n";

    /**
     * A simple pattern. Current value is <b>%d [%t] %p %c - %m%n</b>.
     */
    public static final String SIMPLE_CONVERSION_PATTERN = "%d [%t] %p %c - %m%n";

    /** Key to identify pattern converters. */
    public static final String KEY = "Converter";

    /**
     * Conversion pattern.
     */
    private final String conversionPattern;
    private final PatternSelector patternSelector;
    private final Serializer eventSerializer;

    /**
     * Constructs a PatternLayout using the supplied conversion pattern.
     *
     * @param config The Configuration.
     * @param replace The regular expression to match.
     * @param eventPattern conversion pattern.
     * @param patternSelector The PatternSelector.
     * @param charset The character set.
     * @param alwaysWriteExceptions Whether or not exceptions should always be handled in this pattern (if {@code true},
     *                         exceptions will be written even if the pattern does not specify so).
     * @param disableAnsi
     *            If {@code "true"}, do not output ANSI escape codes
     * @param noConsoleNoAnsi
     *            If {@code "true"} (default) and {@link System#console()} is null, do not output ANSI escape codes
     * @param headerPattern header conversion pattern.
     * @param footerPattern footer conversion pattern.
     */
    private PatternLayout(final Configuration config, final RegexReplacement replace, final String eventPattern,
            final PatternSelector patternSelector, final Charset charset, final boolean alwaysWriteExceptions,
            final boolean disableAnsi, final boolean noConsoleNoAnsi, final String headerPattern,
            final String footerPattern) {
        super(config, charset,
                newSerializerBuilder()
                        .setConfiguration(config)
                        .setReplace(replace)
                        .setPatternSelector(patternSelector)
                        .setAlwaysWriteExceptions(alwaysWriteExceptions)
                        .setDisableAnsi(disableAnsi)
                        .setNoConsoleNoAnsi(noConsoleNoAnsi)
                        .setPattern(headerPattern)
                        .build(),
                newSerializerBuilder()
                        .setConfiguration(config)
                        .setReplace(replace)
                        .setPatternSelector(patternSelector)
                        .setAlwaysWriteExceptions(alwaysWriteExceptions)
                        .setDisableAnsi(disableAnsi)
                        .setNoConsoleNoAnsi(noConsoleNoAnsi)
                        .setPattern(footerPattern)
                        .build());
        this.conversionPattern = eventPattern;
        this.patternSelector = patternSelector;
        this.eventSerializer = newSerializerBuilder()
                .setConfiguration(config)
                .setReplace(replace)
                .setPatternSelector(patternSelector)
                .setAlwaysWriteExceptions(alwaysWriteExceptions)
                .setDisableAnsi(disableAnsi)
                .setNoConsoleNoAnsi(noConsoleNoAnsi)
                .setPattern(eventPattern)
                .setDefaultPattern(DEFAULT_CONVERSION_PATTERN)
                .build();
    }

    public static SerializerBuilder newSerializerBuilder() {
        return new SerializerBuilder();
    }

    @Override
    public boolean requiresLocation() {
        return eventSerializer.requiresLocation();
    }

    /**
     * Gets the conversion pattern.
     *
     * @return the conversion pattern.
     */
    public String getConversionPattern() {
        return conversionPattern;
    }

    /**
     * Gets this PatternLayout's content format. Specified by:
     * <ul>
     * <li>Key: "structured" Value: "false"</li>
     * <li>Key: "formatType" Value: "conversion" (format uses the keywords supported by OptionConverter)</li>
     * <li>Key: "format" Value: provided "conversionPattern" param</li>
     * </ul>
     *
     * @return Map of content format keys supporting PatternLayout
     */
    @Override
    public Map<String, String> getContentFormat() {
        final Map<String, String> result = new HashMap<>();
        result.put("structured", "false");
        result.put("formatType", "conversion");
        result.put("format", conversionPattern);
        return result;
    }

    /**
     * Formats a logging event to a writer.
     *
     * @param event logging event to be formatted.
     * @return The event formatted as a String.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        return eventSerializer.toSerializable(event);
    }

    public void serialize(final LogEvent event, final StringBuilder stringBuilder) {
        eventSerializer.toSerializable(event, stringBuilder);
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final StringBuilder text = toText(eventSerializer, event, getStringBuilder());
        final Encoder<StringBuilder> encoder = getStringBuilderEncoder();
        encoder.encode(text, destination);
        trimToMaxSize(text);
    }

    /**
     * Creates a text representation of the specified log event
     * and writes it into the specified StringBuilder.
     * <p>
     * Implementations are free to return a new StringBuilder if they can
     * detect in advance that the specified StringBuilder is too small.
     */
    private StringBuilder toText(final Serializer2 serializer, final LogEvent event,
            final StringBuilder destination) {
        return serializer.toSerializable(event, destination);
    }

    /**
     * Creates a PatternParser.
     * @param config The Configuration.
     * @return The PatternParser.
     */
    public static PatternParser createPatternParser(final Configuration config) {
        if (config == null) {
            return new PatternParser(config, KEY, LogEventPatternConverter.class);
        }
        PatternParser parser = config.getComponent(KEY);
        if (parser == null) {
            parser = new PatternParser(config, KEY, LogEventPatternConverter.class);
            config.addComponent(KEY, parser);
            parser = config.getComponent(KEY);
        }
        return parser;
    }

    @Override
    public String toString() {
        return patternSelector == null ? conversionPattern : patternSelector.toString();
    }

    private interface PatternSerializer extends Serializer, Serializer2 {}

    private static final class NoFormatPatternSerializer implements PatternSerializer {

        private final LogEventPatternConverter[] converters;

        private NoFormatPatternSerializer(final PatternFormatter[] formatters) {
            this.converters = new LogEventPatternConverter[formatters.length];
            for (int i = 0; i < formatters.length; i++) {
                converters[i] = formatters[i].getConverter();
            }
        }

        @Override
        public String toSerializable(final LogEvent event) {
            final StringBuilder sb = getStringBuilder();
            try {
                return toSerializable(event, sb).toString();
            } finally {
                trimToMaxSize(sb);
            }
        }

        @Override
        public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
            for (LogEventPatternConverter converter : converters) {
                converter.format(event, buffer);
            }
            return buffer;
        }

        @Override
        public boolean requiresLocation() {
            for (LogEventPatternConverter converter : converters) {
                if (converter.requiresLocation()) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return super.toString() + "[converters=" + Arrays.toString(converters) + "]";
        }
    }

    private static final class PatternFormatterPatternSerializer implements PatternSerializer {

        private final PatternFormatter[] formatters;

        private PatternFormatterPatternSerializer(final PatternFormatter[] formatters) {
            this.formatters = formatters;
        }

        @Override
        public String toSerializable(final LogEvent event) {
            final StringBuilder sb = getStringBuilder();
            try {
                return toSerializable(event, sb).toString();
            } finally {
                trimToMaxSize(sb);
            }
        }

        @Override
        public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
            for (PatternFormatter formatter : formatters) {
                formatter.format(event, buffer);
            }
            return buffer;
        }

        @Override
        public String toString() {
            return super.toString() +
                    "[formatters=" +
                    Arrays.toString(formatters) +
                    "]";
        }
    }

    private static final class PatternSerializerWithReplacement implements Serializer, Serializer2 {

        private final PatternSerializer delegate;
        private final RegexReplacement replace;

        private PatternSerializerWithReplacement(final PatternSerializer delegate, final RegexReplacement replace) {
            this.delegate = delegate;
            this.replace = replace;
        }

        @Override
        public String toSerializable(final LogEvent event) {
            final StringBuilder sb = getStringBuilder();
            try {
                return toSerializable(event, sb).toString();
            } finally {
                trimToMaxSize(sb);
            }
        }

        @Override
        public StringBuilder toSerializable(final LogEvent event, final StringBuilder buf) {
            StringBuilder buffer = delegate.toSerializable(event, buf);
            String str = buffer.toString();
            str = replace.format(str);
            buffer.setLength(0);
            buffer.append(str);
            return buffer;
        }



        @Override
        public String toString() {
            return super.toString() +
                    "[delegate=" +
                    delegate +
                    ", replace=" +
                    replace +
                    "]";
        }

        @Override
        public boolean requiresLocation() {
            return delegate.requiresLocation();
        }
    }

    public static class SerializerBuilder implements org.apache.logging.log4j.plugins.util.Builder<Serializer> {

        private Configuration configuration;
        private RegexReplacement replace;
        private String pattern;
        private String defaultPattern;
        private PatternSelector patternSelector;
        private boolean alwaysWriteExceptions;
        private boolean disableAnsi;
        private boolean noConsoleNoAnsi;

        @Override
        public Serializer build() {
            if (Strings.isEmpty(pattern) && Strings.isEmpty(defaultPattern)) {
                return null;
            }
            if (patternSelector == null) {
                try {
                    final PatternParser parser = createPatternParser(configuration);
                    final List<PatternFormatter> list = parser.parse(pattern == null ? defaultPattern : pattern,
                            alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi);
                    final PatternFormatter[] formatters = list.toArray(new PatternFormatter[0]);
                    boolean hasFormattingInfo = false;
                    for (PatternFormatter formatter : formatters) {
                        FormattingInfo info = formatter.getFormattingInfo();
                        if (info != null && info != FormattingInfo.getDefault()) {
                            hasFormattingInfo = true;
                            break;
                        }
                    }
                    PatternSerializer serializer = hasFormattingInfo
                            ? new PatternFormatterPatternSerializer(formatters)
                            : new NoFormatPatternSerializer(formatters);
                    return replace == null ? serializer : new PatternSerializerWithReplacement(serializer, replace);
                } catch (final RuntimeException ex) {
                    throw new IllegalArgumentException("Cannot parse pattern '" + pattern + "'", ex);
                }
            }
            return new PatternSelectorSerializer(patternSelector, replace);
        }

        public SerializerBuilder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public SerializerBuilder setReplace(final RegexReplacement replace) {
            this.replace = replace;
            return this;
        }

        public SerializerBuilder setPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        public SerializerBuilder setDefaultPattern(final String defaultPattern) {
            this.defaultPattern = defaultPattern;
            return this;
        }

        public SerializerBuilder setPatternSelector(final PatternSelector patternSelector) {
            this.patternSelector = patternSelector;
            return this;
        }

        public SerializerBuilder setAlwaysWriteExceptions(final boolean alwaysWriteExceptions) {
            this.alwaysWriteExceptions = alwaysWriteExceptions;
            return this;
        }

        public SerializerBuilder setDisableAnsi(final boolean disableAnsi) {
            this.disableAnsi = disableAnsi;
            return this;
        }

        public SerializerBuilder setNoConsoleNoAnsi(final boolean noConsoleNoAnsi) {
            this.noConsoleNoAnsi = noConsoleNoAnsi;
            return this;
        }

    }

    private static final class PatternSelectorSerializer implements Serializer, Serializer2 {

        private final PatternSelector patternSelector;
        private final RegexReplacement replace;

        private PatternSelectorSerializer(final PatternSelector patternSelector, final RegexReplacement replace) {
            super();
            this.patternSelector = patternSelector;
            this.replace = replace;
        }

        @Override
        public String toSerializable(final LogEvent event) {
            final StringBuilder sb = getStringBuilder();
            try {
                return toSerializable(event, sb).toString();
            } finally {
                trimToMaxSize(sb);
            }
        }

        @Override
        public StringBuilder toSerializable(final LogEvent event, final StringBuilder buffer) {
            for (PatternFormatter formatter : patternSelector.getFormatters(event)) {
                formatter.format(event, buffer);
            }
            if (replace != null) { // creates temporary objects
                String str = buffer.toString();
                str = replace.format(str);
                buffer.setLength(0);
                buffer.append(str);
            }
            return buffer;
        }

        @Override
        public boolean requiresLocation() {
            return patternSelector.requiresLocation();
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append("[patternSelector=");
            builder.append(patternSelector);
            builder.append(", replace=");
            builder.append(replace);
            builder.append("]");
            return builder.toString();
        }
    }

    /**
     * Creates a PatternLayout using the default options. These options include using UTF-8, the default conversion
     * pattern, exceptions being written, and with ANSI escape codes.
     *
     * @return the PatternLayout.
     * @see #DEFAULT_CONVERSION_PATTERN Default conversion pattern
     */
    public static PatternLayout createDefaultLayout() {
        return newBuilder().build();
    }

    /**
     * Creates a PatternLayout using the default options and the given configuration. These options include using UTF-8,
     * the default conversion pattern, exceptions being written, and with ANSI escape codes.
     *
     * @param configuration The Configuration.
     *
     * @return the PatternLayout.
     * @see #DEFAULT_CONVERSION_PATTERN Default conversion pattern
     */
    public static PatternLayout createDefaultLayout(final Configuration configuration) {
        return newBuilder().setConfiguration(configuration).build();
    }

    /**
     * Creates a builder for a custom PatternLayout.
     *
     * @return a PatternLayout builder.
     */
    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Custom PatternLayout builder. Use the {@link PatternLayout#newBuilder() builder factory method} to create this.
     */
    public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<PatternLayout> {

        @PluginBuilderAttribute
        private String pattern = PatternLayout.DEFAULT_CONVERSION_PATTERN;

        @PluginElement("PatternSelector")
        private PatternSelector patternSelector;

        @PluginConfiguration
        private Configuration configuration;

        @PluginElement("Replace")
        private RegexReplacement regexReplacement;

        // LOG4J2-783 use platform default by default
        @PluginBuilderAttribute
        private Charset charset = Charset.defaultCharset();

        @PluginBuilderAttribute
        private boolean alwaysWriteExceptions = true;

        @PluginBuilderAttribute
        private boolean disableAnsi = !useAnsiEscapeCodes();

        @PluginBuilderAttribute
        private boolean noConsoleNoAnsi;

        @PluginBuilderAttribute
        private String header;

        @PluginBuilderAttribute
        private String footer;

        private Builder() {
        }

        private boolean useAnsiEscapeCodes() {
            final PropertyEnvironment properties = PropertiesUtil.getProperties();
            final boolean isPlatformSupportsAnsi = !properties.isOsWindows();
            final boolean isJansiRequested = !properties.getBooleanProperty(Log4jProperties.JANSI_DISABLED, true);
            return isPlatformSupportsAnsi || isJansiRequested;
        }

        /**
         * @param pattern
         *        The pattern. If not specified, defaults to DEFAULT_CONVERSION_PATTERN.
         */
        public Builder setPattern(final String pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * @param patternSelector
         *        Allows different patterns to be used based on some selection criteria.
         */
        public Builder setPatternSelector(final PatternSelector patternSelector) {
            this.patternSelector = patternSelector;
            return this;
        }

        /**
         * @param configuration
         *        The Configuration. Some Converters require access to the Interpolator.
         */
        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * @param regexReplacement
         *        A Regex replacement
         */
        public Builder setRegexReplacement(final RegexReplacement regexReplacement) {
            this.regexReplacement = regexReplacement;
            return this;
        }

        /**
         * @param charset
         *        The character set. The platform default is used if not specified.
         */
        public Builder setCharset(final Charset charset) {
            // LOG4J2-783 if null, use platform default by default
            if (charset != null) {
                this.charset = charset;
            }
            return this;
        }

        /**
         * @param alwaysWriteExceptions
         *        If {@code "true"} (default) exceptions are always written even if the pattern contains no exception tokens.
         */
        public Builder setAlwaysWriteExceptions(final boolean alwaysWriteExceptions) {
            this.alwaysWriteExceptions = alwaysWriteExceptions;
            return this;
        }

        /**
         * @param disableAnsi
         *        If {@code "true"} (default is value of system property `log4j.skipJansi`, or `true` if undefined),
         *        do not output ANSI escape codes
         */
        public Builder setDisableAnsi(final boolean disableAnsi) {
            this.disableAnsi = disableAnsi;
            return this;
        }

        /**
         * @param noConsoleNoAnsi
         *        If {@code "true"} (default is false) and {@link System#console()} is null, do not output ANSI escape codes
         */
        public Builder setNoConsoleNoAnsi(final boolean noConsoleNoAnsi) {
            this.noConsoleNoAnsi = noConsoleNoAnsi;
            return this;
        }

        /**
         * @param header
         *        The footer to place at the top of the document, once.
         */
        public Builder setHeader(final String header) {
            this.header = header;
            return this;
        }

        /**
         * @param footer
         *        The footer to place at the bottom of the document, once.
         */
        public Builder setFooter(final String footer) {
            this.footer = footer;
            return this;
        }

        @Override
        public PatternLayout build() {
            // fall back to DefaultConfiguration
            if (configuration == null) {
                configuration = new DefaultConfiguration();
            }
            return new PatternLayout(configuration, regexReplacement, pattern, patternSelector, charset,
                alwaysWriteExceptions, disableAnsi, noConsoleNoAnsi, header, footer);
        }
    }

    public Serializer getEventSerializer() {
        return eventSerializer;
    }
}
