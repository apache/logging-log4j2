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
package org.apache.log4j;

import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;

/**
 *
 */
public class PatternLayout extends Layout {

    /**
     * Default pattern string for log output. Currently set to the string <b>{@value #DEFAULT_CONVERSION_PATTERN}</b> which
     * just prints the application supplied message.
     */
    public static final String DEFAULT_CONVERSION_PATTERN = "%m%n";

    /**
     * A conversion pattern equivalent to the TTCCCLayout. Current value is <b>{@value #TTCC_CONVERSION_PATTERN}</b>
     */
    public static final String TTCC_CONVERSION_PATTERN = "%r [%t] %p %c %x - %m%n";

    protected final int BUF_SIZE = 256;

    protected final int MAX_CAPACITY = 1024;

    // output buffer appended to when format() is invoked
    private StringBuffer sbuf = new StringBuffer(BUF_SIZE);

    private String pattern;

    private PatternConverter head;

    /**
     * Constructs a PatternLayout using the DEFAULT_LAYOUT_PATTERN.
     *
     * The default pattern just produces the application supplied message.
     */
    public PatternLayout() {
        this(DEFAULT_CONVERSION_PATTERN);
    }

    /**
     * Constructs a PatternLayout using the supplied conversion pattern.
     */
    public PatternLayout(final String pattern) {
        this.pattern = pattern;
        head = createPatternParser((pattern == null) ? DEFAULT_CONVERSION_PATTERN : pattern)
                .parse();
    }

    /**
     * Does not do anything as options become effective
     */
    public void activateOptions() {
        // nothing to do.
    }

    /**
     * Returns PatternParser used to parse the conversion string. Subclasses may override this to return a subclass of
     * PatternParser which recognize custom conversion characters.
     *
     * @since 0.9.0
     */
    protected PatternParser createPatternParser(final String pattern) {
        return new PatternParser(pattern);
    }

    /**
     * Produces a formatted string as specified by the conversion pattern.
     */
    @Override
    public String format(final LoggingEvent event) {
        // Reset working stringbuffer
        if (sbuf.capacity() > MAX_CAPACITY) {
            sbuf = new StringBuffer(BUF_SIZE);
        } else {
            sbuf.setLength(0);
        }

        PatternConverter c = head;

        while (c != null) {
            c.format(sbuf, event);
            c = c.next;
        }
        return sbuf.toString();
    }

    /**
     * Returns the value of the <b>ConversionPattern</b> option.
     */
    public String getConversionPattern() {
        return pattern;
    }

    /**
     * The PatternLayout does not handle the throwable contained within {@link LoggingEvent LoggingEvents}. Thus, it returns
     * <code>true</code>.
     *
     * @since 0.8.4
     */
    @Override
    public boolean ignoresThrowable() {
        return true;
    }

    /**
     * Set the <b>ConversionPattern</b> option. This is the string which controls formatting and consists of a mix of
     * literal content and conversion specifiers.
     */
    public void setConversionPattern(final String conversionPattern) {
        pattern = conversionPattern;
        head = createPatternParser(conversionPattern).parse();
    }
}
