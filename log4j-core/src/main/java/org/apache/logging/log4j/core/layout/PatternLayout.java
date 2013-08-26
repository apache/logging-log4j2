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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.Booleans;
import org.apache.logging.log4j.core.helpers.Charsets;
import org.apache.logging.log4j.core.helpers.OptionConverter;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.pattern.RegexReplacement;

/**
 * <p>A flexible layout configurable with pattern string. The goal of this class
 * is to {@link org.apache.logging.log4j.core.Layout#toByteArray format} a {@link LogEvent} and return the results.
 * The format of the result depends on the <em>conversion pattern</em>.
 * <p>
 * <p/>
 * <p>The conversion pattern is closely related to the conversion
 * pattern of the printf function in C. A conversion pattern is
 * composed of literal text and format control expressions called
 * <em>conversion specifiers</em>.
 *
 * See the Log4j Manual for details on the supported pattern converters.
 */
@Plugin(name = "PatternLayout", category = "Core", elementType = "layout", printObject = true)
public final class PatternLayout extends AbstractStringLayout {
    /**
     * Default pattern string for log output. Currently set to the
     * string <b>"%m%n"</b> which just prints the application supplied
     * message.
     */
    public static final String DEFAULT_CONVERSION_PATTERN = "%m%n";

    /**
     * A conversion pattern equivalent to the TTCCCLayout.
     * Current value is <b>%r [%t] %p %c %x - %m%n</b>.
     */
    public static final String TTCC_CONVERSION_PATTERN =
        "%r [%t] %p %c %x - %m%n";

    /**
     * A simple pattern.
     * Current value is <b>%d [%t] %p %c - %m%n</b>.
     */
    public static final String SIMPLE_CONVERSION_PATTERN =
        "%d [%t] %p %c - %m%n";

    /** Key to identify pattern converters. */
    public static final String KEY = "Converter";

    /**
     * Initial converter for pattern.
     */
    private List<PatternFormatter> formatters;

    /**
     * Conversion pattern.
     */
    private final String conversionPattern;


    /**
     * The current Configuration.
     */
    private final Configuration config;

    private final RegexReplacement replace;

    private final boolean alwaysWriteExceptions;

    /**
     * Constructs a EnhancedPatternLayout using the supplied conversion pattern.
     *
     * @param config The Configuration.
     * @param replace The regular expression to match.
     * @param pattern conversion pattern.
     * @param charset The character set.
     * @param alwaysWriteExceptions Whether or not exceptions should always be handled in this pattern (if {@code true},
     *                         exceptions will be written even if the pattern does not specify so).
     */
    private PatternLayout(final Configuration config, final RegexReplacement replace, final String pattern,
                          final Charset charset, final boolean alwaysWriteExceptions) {
        super(charset);
        this.replace = replace;
        this.conversionPattern = pattern;
        this.config = config;
        this.alwaysWriteExceptions = alwaysWriteExceptions;
        final PatternParser parser = createPatternParser(config);
        formatters = parser.parse(pattern == null ? DEFAULT_CONVERSION_PATTERN : pattern, this.alwaysWriteExceptions);
    }

    /**
     * Set the <b>ConversionPattern</b> option. This is the string which
     * controls formatting and consists of a mix of literal content and
     * conversion specifiers.
     *
     * @param conversionPattern conversion pattern.
     */
    public void setConversionPattern(final String conversionPattern) {
        final String pattern = OptionConverter.convertSpecialChars(conversionPattern);
        if (pattern == null) {
            return;
        }
        final PatternParser parser = createPatternParser(this.config);
        formatters = parser.parse(pattern, this.alwaysWriteExceptions);
    }

    public String getConversionPattern() {
        return conversionPattern;
    }

    /**
     * PatternLayout's content format is specified by:<p/>
     * Key: "structured" Value: "false"<p/>
     * Key: "formatType" Value: "conversion" (format uses the keywords supported by OptionConverter)<p/>
     * Key: "format" Value: provided "conversionPattern" param
     * @return Map of content format keys supporting PatternLayout
     */
    @Override
    public Map<String, String> getContentFormat()
    {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("structured", "false");
        result.put("formatType", "conversion");
        result.put("format", conversionPattern);
        return result;
    }

    /**
     * Formats a logging event to a writer.
     *
     *
     * @param event logging event to be formatted.
     * @return The event formatted as a String.
     */
    @Override
    public String toSerializable(final LogEvent event) {
        final StringBuilder buf = new StringBuilder();
        for (final PatternFormatter formatter : formatters) {
            formatter.format(event, buf);
        }
        String str = buf.toString();
        if (replace != null) {
            str = replace.format(str);
        }
        return str;
    }

    /**
     * Create a PatternParser.
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
            parser = (PatternParser) config.getComponent(KEY);
        }
        return parser;
    }

    @Override
    public String toString() {
        return conversionPattern;
    }

    /**
     * Create a pattern layout.
     *
     * @param pattern The pattern. If not specified, defaults to DEFAULT_CONVERSION_PATTERN.
     * @param config The Configuration. Some Converters require access to the Interpolator.
     * @param replace A Regex replacement String.
     * @param charsetName The character set.
     * @param always If {@code "true"} (default) exceptions are always written even if the pattern contains no exception
     *               tokens.
     * @return The PatternLayout.
     */
    @PluginFactory
    public static PatternLayout createLayout(
            @PluginAttribute("pattern") final String pattern,
            @PluginConfiguration final Configuration config,
            @PluginElement("Replace") final RegexReplacement replace,
            @PluginAttribute("charset") final String charsetName,
            @PluginAttribute("alwaysWriteExceptions") final String always) {
        final Charset charset = Charsets.getSupportedCharset(charsetName);
        final boolean alwaysWriteExceptions = Booleans.parseBoolean(always, true);
        return new PatternLayout(
                config, replace, pattern == null ? DEFAULT_CONVERSION_PATTERN : pattern, charset, alwaysWriteExceptions
        );
    }
}
