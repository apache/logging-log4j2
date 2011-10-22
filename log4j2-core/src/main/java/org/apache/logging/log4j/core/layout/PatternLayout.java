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

package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.helpers.OptionConverter;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.pattern.RegexReplacement;

import java.nio.charset.Charset;
import java.util.List;

/**
 * <p>A flexible layout configurable with pattern string. The goal of this class
 * is to {@link #format format} a {@link LogEvent} and return the results.
 * The format of the result depends on the <em>conversion pattern</em>.
 * <p>
 * <p/>
 * <p>The conversion pattern is closely related to the conversion
 * pattern of the printf function in C. A conversion pattern is
 * composed of literal text and format control expressions called
 * <em>conversion specifiers</em>.
 * <p/>
 * <p><i>Note that you are free to insert any literal text within the
 * conversion pattern.</i>
 * </p>
 * <p/>
 * <p>Each conversion specifier starts with a percent sign (%) and is
 * followed by optional <em>format modifiers</em> and a <em>conversion
 * character</em>. The conversion character specifies the type of
 * data, e.g. category, priority, date, thread name. The format
 * modifiers control such things as field width, padding, left and
 * right justification. The following is a simple example.
 * <p/>
 * <p>Let the conversion pattern be <b>"%-5p [%t]: %m%n"</b> and assume
 * that the log4j environment was set to use a PatternLayout. Then the
 * statements
 * <pre>
 * Logger logger = LoggerFactory().getLogger("MyLogger");
 * logger.debug("Message 1");
 * logger.warn("Message 2");
 * </pre>
 * would yield the output
 * <pre>
 * DEBUG [main]: Message 1
 * WARN  [main]: Message 2
 * </pre>
 * <p/>
 * <p>Note that there is no explicit separator between text and
 * conversion specifiers. The pattern parser knows when it has reached
 * the end of a conversion specifier when it reads a conversion
 * character. In the example above the conversion specifier
 * <b>%-5p</b> means the priority of the logging event should be left
 * justified to a width of five characters.
 * <p/>
 * The recognized conversion characters are
 * <p/>
 * <p>
 * <table border="1" CELLPADDING="8">
 * <th>Conversion Character</th>
 * <th>Effect</th>
 * <p/>
 * <tr>
 * <td align=center><b>c</b></td>
 * <p/>
 * <td>Used to output the category of the logging event. The
 * category conversion specifier can be optionally followed by
 * <em>precision specifier</em>, that is a decimal constant in
 * brackets.
 * <p/>
 * <p>If a precision specifier is given, then only the corresponding
 * number of right most components of the category name will be
 * printed. By default the category name is printed in full.
 * <p/>
 * <p>For example, for the category name "a.b.c" the pattern
 * <b>%c{2}</b> will output "b.c".
 * <p/>
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>C</b></td>
 * <p/>
 * <td>Used to output the fully qualified class name of the caller
 * issuing the logging request. This conversion specifier
 * can be optionally followed by <em>precision specifier</em>, that
 * is a decimal constant in brackets.
 * <p/>
 * <p>If a precision specifier is given, then only the corresponding
 * number of right most components of the class name will be
 * printed. By default the class name is output in fully qualified form.
 * <p/>
 * <p>For example, for the class name "org.apache.xyz.SomeClass", the
 * pattern <b>%C{1}</b> will output "SomeClass".
 * <p/>
 * <p><b>WARNING</b> Generating the caller class information is
 * slow. Thus, it's use should be avoided unless execution speed is
 * not an issue.
 * <p/>
 * </td>
 * </tr>
 * <p/>
 * <tr> <td align=center><b>d</b></td> <td>Used to output the date of
 * the logging event. The date conversion specifier may be
 * followed by a set of braces containing a
 * date and time pattern strings {@link java.text.SimpleDateFormat},
 * <em>ABSOLUTE</em>, <em>DATE</em> or <em>ISO8601</em>
 * and a set of braces containing a time zone id per
 * {@link java.util.TimeZone#getTimeZone(String)}.
 * For example, <b>%d{HH:mm:ss,SSS}</b>,
 * <b>%d{dd&nbsp;MMM&nbsp;yyyy&nbsp;HH:mm:ss,SSS}</b>,
 * <b>%d{DATE}</b> or <b>%d{HH:mm:ss}{GMT+0}</b>. If no date format specifier is given then
 * ISO8601 format is assumed.
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>F</b></td>
 * <p/>
 * <td>Used to output the file name where the logging request was
 * issued.
 * <p/>
 * <p><b>WARNING</b> Generating caller location information is
 * extremely slow. Its use should be avoided unless execution speed
 * is not an issue.
 * <p/>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>l</b></td>
 * <p/>
 * <td>Used to output location information of the caller which generated
 * the logging event.
 * <p/>
 * <p>The location information depends on the JVM implementation but
 * usually consists of the fully qualified name of the calling
 * method followed by the callers source the file name and line
 * number between parentheses.
 * <p/>
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>L</b></td>
 * <p/>
 * <td>Used to output the line number from where the logging request
 * was issued.
 * <p/>
 * </tr>
 * <p/>
 * <p/>
 * <tr>
 * <td align=center><b>m</b></td>
 * <td>Used to output the application supplied message associated with
 * the logging event.</td>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>M</b></td>
 * <p/>
 * <td>Used to output the method name where the logging request was
 * issued.
 * <p/>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>n</b></td>
 * <p/>
 * <td>Outputs the platform dependent line separator character or
 * characters.
 * <p/>
 * <p>This conversion character offers practically the same
 * performance as using non-portable line separator strings such as
 * "\n", or "\r\n". Thus, it is the preferred way of specifying a
 * line separator.
 * <p/>
 * <p/>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>p</b></td>
 * <td>Used to output the level of the logging event.</td>
 * </tr>
 * <p/>
 * <tr>
 * <p/>
 * <td align=center><b>r</b></td>
 * <p/>
 * <td>Used to output the number of milliseconds elapsed since the construction
 * of the layout until the creation of the logging event.</td>
 * </tr>
 * <p/>
 * <p/>
 * <tr>
 * <td align=center><b>t</b></td>
 * <p/>
 * <td>Used to output the name of the thread that generated the logging event.</td>
 * <p/>
 * </tr>
 * <p/>
 * <tr>
 * <p/>
 * <td align=center><b>x</b></td>
 * <p/>
 * <td>Used to output the NDC (nested diagnostic context) associated
 * with the thread that generated the logging event.
 * </td>
 * </tr>
 * <p/>
 * <p/>
 * <tr>
 * <td align=center><b>X</b></td>
 * <p/>
 * <td>
 * <p/>
 * <p>Used to output the MDC (mapped diagnostic context) associated
 * with the thread that generated the logging event. The <b>X</b>
 * conversion character can be followed by the key for the
 * map placed between braces, as in <b>%X{clientNumber}</b> where
 * <code>clientNumber</code> is the key. The value in the MDC
 * corresponding to the key will be output. If no additional sub-option
 * is specified, then the entire contents of the MDC key value pair set
 * is output using a format {{key1,val1},{key2,val2}}</p>
 * <p/>
 * <p>See {@link ThreadContext} class for more details.
 * </p>
 * <p/>
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>properties</b></td>
 * <p/>
 * <td>
 * <p>Used to output the Properties associated with the logging event. The <b>properties</b>
 * conversion word can be followed by the key for the
 * map placed between braces, as in <b>%properties{application}</b> where
 * <code>application</code> is the key. The value in the Properties bundle
 * corresponding to the key will be output. If no additional sub-option
 * is specified, then the entire contents of the Properties key value pair set
 * is output using a format {{key1,val1},{key2,val2}}</p>
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <td align=center><b>throwable</b></td>
 * <p/>
 * <td>
 * <p>Used to output the Throwable trace that has been bound to the LoggingEvent, by
 * default this will output the full trace as one would normally find by a call to Throwable.printStackTrace().
 * The throwable conversion word can be followed by an option in the form <b>%throwable{short}</b>
 * which will only output the first line of the Throwable.</p>
 * </td>
 * </tr>
 * <p/>
 * <tr>
 * <p/>
 * <td align=center><b>%</b></td>
 * <p/>
 * <td>The sequence %% outputs a single percent sign.
 * </td>
 * </tr>
 * <p/>
 * </table>
 * <p/>
 * <p>By default the relevant information is output as is. However,
 * with the aid of format modifiers it is possible to change the
 * minimum field width, the maximum field width and justification.
 * <p/>
 * <p>The optional format modifier is placed between the percent sign
 * and the conversion character.
 * <p/>
 * <p>The first optional format modifier is the <em>left justification
 * flag</em> which is just the minus (-) character. Then comes the
 * optional <em>minimum field width</em> modifier. This is a decimal
 * constant that represents the minimum number of characters to
 * output. If the data item requires fewer characters, it is padded on
 * either the left or the right until the minimum width is
 * reached. The default is to pad on the left (right justify) but you
 * can specify right padding with the left justification flag. The
 * padding character is space. If the data item is larger than the
 * minimum field width, the field is expanded to accommodate the
 * data. The value is never truncated.
 * <p/>
 * <p>This behavior can be changed using the <em>maximum field
 * width</em> modifier which is designated by a period followed by a
 * decimal constant. If the data item is longer than the maximum
 * field, then the extra characters are removed from the
 * <em>beginning</em> of the data item and not from the end. For
 * example, it the maximum field width is eight and the data item is
 * ten characters long, then the first two characters of the data item
 * are dropped. This behavior deviates from the printf function in C
 * where truncation is done from the end.
 * <p/>
 * <p>Below are various format modifier examples for the category
 * conversion specifier.
 * <p/>
 * <p/>
 * <TABLE BORDER=1 CELLPADDING=8>
 * <th>Format modifier
 * <th>left justify
 * <th>minimum width
 * <th>maximum width
 * <th>comment
 * <p/>
 * <tr>
 * <td align=center>%20c</td>
 * <td align=center>false</td>
 * <td align=center>20</td>
 * <td align=center>none</td>
 * <p/>
 * <td>Left pad with spaces if the category name is less than 20
 * characters long.
 * <p/>
 * <tr> <td align=center>%-20c</td> <td align=center>true</td> <td
 * align=center>20</td> <td align=center>none</td> <td>Right pad with
 * spaces if the category name is less than 20 characters long.
 * <p/>
 * <tr>
 * <td align=center>%.30c</td>
 * <td align=center>NA</td>
 * <td align=center>none</td>
 * <td align=center>30</td>
 * <p/>
 * <td>Truncate from the beginning if the category name is longer than 30
 * characters.
 * <p/>
 * <tr>
 * <td align=center>%20.30c</td>
 * <td align=center>false</td>
 * <td align=center>20</td>
 * <td align=center>30</td>
 * <p/>
 * <td>Left pad with spaces if the category name is shorter than 20
 * characters. However, if category name is longer than 30 characters,
 * then truncate from the beginning.
 * <p/>
 * <tr>
 * <td align=center>%-20.30c</td>
 * <td align=center>true</td>
 * <td align=center>20</td>
 * <td align=center>30</td>
 * <p/>
 * <td>Right pad with spaces if the category name is shorter than 20
 * characters. However, if category name is longer than 30 characters,
 * then truncate from the beginning.
 * <p/>
 * </table>
 * <p/>
 * <p>Below are some examples of conversion patterns.
 * <p/>
 * <dl>
 * <p/>
 * <p><dt><b>%r [%t] %-5p %c %x - %m%n</b>
 * <p><dd>This is essentially the TTCC layout.
 * <p/>
 * <p><dt><b>%-6r [%15.15t] %-5p %30.30c %x - %m%n</b>
 * <p/>
 * <p><dd>Similar to the TTCC layout except that the relative time is
 * right padded if less than 6 digits, thread name is right padded if
 * less than 15 characters and truncated if longer and the category
 * name is left padded if shorter than 30 characters and truncated if
 * longer.
 * <p/>
 * </dl>
 * <p/>
 * <p>The above text is largely inspired from Peter A. Darnell and
 * Philip E. Margolis' highly recommended book "C -- a Software
 * Engineering Approach", ISBN 0-387-97389-3.
 */
@Plugin(name="PatternLayout",type="Core",elementType="layout",printObject=true)
public class PatternLayout extends AbstractStringLayout {
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

    /**
     * Initial converter for pattern.
     */
    private List<PatternConverter> converters;

    public static final String KEY = "Converter";

    /**
     * Conversion pattern.
     */
    private String conversionPattern;

    /**
     * True if any element in pattern formats information from exceptions.
     */
    private boolean handlesExceptions;

    /**
     * The current Configuration.
     */
    private final Configuration config;

    private final RegexReplacement replace;

    /**
     * Constructs a EnhancedPatternLayout using the DEFAULT_LAYOUT_PATTERN.
     * <p/>
     * The default pattern just produces the application supplied message.
     */
    public PatternLayout() {
        this(null, null, DEFAULT_CONVERSION_PATTERN, Charset.defaultCharset());
    }

    /**
     * Constructs a EnhancedPatternLayout using the DEFAULT_LAYOUT_PATTERN.
     * <p/>
     * The default pattern just produces the application supplied message.
     */
    public PatternLayout(final String pattern) {
        this(null, null, pattern, Charset.defaultCharset());
    }

   /**
     * Constructs a EnhancedPatternLayout using the DEFAULT_LAYOUT_PATTERN.
     * <p/>
     * The default pattern just produces the application supplied message.
     */
    public PatternLayout(Configuration config, final String pattern) {
        this(config, null, pattern, Charset.defaultCharset());
    }

    /**
     * Constructs a EnhancedPatternLayout using the supplied conversion pattern.
     *
     * @param pattern conversion pattern.
     */
    public PatternLayout(Configuration config, final RegexReplacement replace, final String pattern,
                         final Charset charset) {
        super(charset);
        this.replace = replace;
        this.conversionPattern = pattern;
        this.config = config;
        PatternParser parser = createPatternParser(config);
        converters = parser.parse((pattern == null) ? DEFAULT_CONVERSION_PATTERN : pattern);
        handlesExceptions = parser.handlesExceptions();

    }

    /**
     * Set the <b>ConversionPattern</b> option. This is the string which
     * controls formatting and consists of a mix of literal content and
     * conversion specifiers.
     *
     * @param conversionPattern conversion pattern.
     */
    public void setConversionPattern(final String conversionPattern) {
        String pattern = OptionConverter.convertSpecialChars(conversionPattern);
        if (pattern == null) {
            return;
        }
        PatternParser parser = createPatternParser(this.config);
        converters = parser.parse(pattern);
        handlesExceptions = parser.handlesExceptions();
    }

    /**
     * Formats a logging event to a writer.
     *
     * @param event logging event to be formatted.
     */
    public String formatAs(final LogEvent event) {
        StringBuilder buf = new StringBuilder();
        for (PatternConverter c : converters) {
            c.format(event, buf);
        }
        String str = buf.toString();
        if (replace != null) {
            str = replace.format(str);
        }
        return config == null ? str : config.getSubst().replace(event, str);
    }

    public static PatternParser createPatternParser(Configuration config) {
        if (config == null) {
            return new PatternParser(config, KEY, LogEventPatternConverter.class);
        }
        PatternParser parser = (PatternParser) config.getComponent(KEY);
        if (parser == null) {
            parser = new PatternParser(config, KEY, LogEventPatternConverter.class);
            config.addComponent(KEY, parser);
            parser = (PatternParser) config.getComponent(KEY);
        }
        return parser;
    }

    public String toString() {
        return "PatternLayout(" + conversionPattern + ")";
    }

    @PluginFactory
    public static PatternLayout createLayout(@PluginAttr("pattern") String pattern,
                                             @PluginConfiguration Configuration config,
                                             @PluginElement("replace") RegexReplacement replace,
                                             @PluginAttr("charset") String charset) {
        Charset c = Charset.isSupported("UTF-8") ? Charset.forName("UTF-8") : Charset.defaultCharset();
        if (charset != null) {
            if (Charset.isSupported(charset)) {
                c = Charset.forName(charset);
            } else {
                logger.error("Charset " + charset + " is not supported for layout, using " + c.displayName());
            }
        }
        if (pattern != null) {
            return new PatternLayout(config, replace, pattern, c);
        }
        logger.error("No pattern specified for PatternLayout");
        return null;
    }
}
