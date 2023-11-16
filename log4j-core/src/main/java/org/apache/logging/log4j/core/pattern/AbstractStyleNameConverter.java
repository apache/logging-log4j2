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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
 */
public abstract class AbstractStyleNameConverter extends LogEventPatternConverter /*TODO: implements AnsiConverter*/ {

    private final List<PatternFormatter> formatters;

    private final String style;

    /**
     * Constructs the converter.
     *
     * @param formatters The PatternFormatters to generate the text to manipulate.
     * @param styling The styling that should encapsulate the pattern.
     */
    protected AbstractStyleNameConverter(
            final String name, final List<PatternFormatter> formatters, final String styling) {
        super(name, "style");
        this.formatters = formatters;
        this.style = styling;
    }

    /**
     * Black style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = Black.NAME, category = "Converter")
    @ConverterKeys(Black.NAME)
    public static final class Black extends AbstractStyleNameConverter {

        /** Black */
        protected static final String NAME = "black";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public Black(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *            throwable will be formatted.
         * @return new instance of class or null
         */
        public static Black newInstance(final Configuration config, final String[] options) {
            return newInstance(Black.class, NAME, config, options);
        }
    }

    /**
     * Blue style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = Blue.NAME, category = "Converter")
    @ConverterKeys(Blue.NAME)
    public static final class Blue extends AbstractStyleNameConverter {

        /** Blue */
        protected static final String NAME = "blue";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public Blue(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *                throwable will be formatted.
         * @return new instance of class or null
         */
        public static Blue newInstance(final Configuration config, final String[] options) {
            return newInstance(Blue.class, NAME, config, options);
        }
    }

    /**
     * Cyan style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = Cyan.NAME, category = "Converter")
    @ConverterKeys(Cyan.NAME)
    public static final class Cyan extends AbstractStyleNameConverter {

        /** Cyan */
        protected static final String NAME = "cyan";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public Cyan(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *                throwable will be formatted.
         * @return new instance of class or null
         */
        public static Cyan newInstance(final Configuration config, final String[] options) {
            return newInstance(Cyan.class, NAME, config, options);
        }
    }

    /**
     * Green style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = Green.NAME, category = "Converter")
    @ConverterKeys(Green.NAME)
    public static final class Green extends AbstractStyleNameConverter {

        /** Green */
        protected static final String NAME = "green";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public Green(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *                throwable will be formatted.
         * @return new instance of class or null
         */
        public static Green newInstance(final Configuration config, final String[] options) {
            return newInstance(Green.class, NAME, config, options);
        }
    }

    /**
     * Magenta style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = Magenta.NAME, category = "Converter")
    @ConverterKeys(Magenta.NAME)
    public static final class Magenta extends AbstractStyleNameConverter {

        /** Magenta */
        protected static final String NAME = "magenta";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public Magenta(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *                throwable will be formatted.
         * @return new instance of class or null
         */
        public static Magenta newInstance(final Configuration config, final String[] options) {
            return newInstance(Magenta.class, NAME, config, options);
        }
    }

    /**
     * Red style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = Red.NAME, category = "Converter")
    @ConverterKeys(Red.NAME)
    public static final class Red extends AbstractStyleNameConverter {

        /** Red */
        protected static final String NAME = "red";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public Red(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *                throwable will be formatted.
         * @return new instance of class or null
         */
        public static Red newInstance(final Configuration config, final String[] options) {
            return newInstance(Red.class, NAME, config, options);
        }
    }

    /**
     * White style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = White.NAME, category = "Converter")
    @ConverterKeys(White.NAME)
    public static final class White extends AbstractStyleNameConverter {

        /** White */
        protected static final String NAME = "white";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public White(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *                throwable will be formatted.
         * @return new instance of class or null
         */
        public static White newInstance(final Configuration config, final String[] options) {
            return newInstance(White.class, NAME, config, options);
        }
    }

    /**
     * Yellow style pattern converter. Adds ANSI color styling to the result of the enclosed pattern.
     */
    @Plugin(name = Yellow.NAME, category = "Converter")
    @ConverterKeys(Yellow.NAME)
    public static final class Yellow extends AbstractStyleNameConverter {

        /** Yellow */
        protected static final String NAME = "yellow";

        /**
         * Constructs the converter. This constructor must be public.
         *
         * @param formatters The PatternFormatters to generate the text to manipulate.
         * @param styling The styling that should encapsulate the pattern.
         */
        public Yellow(final List<PatternFormatter> formatters, final String styling) {
            super(NAME, formatters, styling);
        }

        /**
         * Gets an instance of the class (called via reflection).
         *
         * @param config The current Configuration.
         * @param options The pattern options, may be null. If the first element is "short", only the first line of the
         *                throwable will be formatted.
         * @return new instance of class or null
         */
        public static Yellow newInstance(final Configuration config, final String[] options) {
            return newInstance(Yellow.class, NAME, config, options);
        }
    }

    /**
     * Gets an instance of the class (called via reflection).
     *
     * @param config The current Configuration.
     * @param options The pattern options, may be null. If the first element is "short", only the first line of the
     *                throwable will be formatted.
     * @return new instance of class or null
     */
    protected static <T extends AbstractStyleNameConverter> T newInstance(
            final Class<T> asnConverterClass, final String name, final Configuration config, final String[] options) {
        final List<PatternFormatter> formatters = toPatternFormatterList(config, options);
        if (formatters == null) {
            return null;
        }
        try {
            final Constructor<T> constructor = asnConverterClass.getConstructor(List.class, String.class);
            return constructor.newInstance(formatters, AnsiEscape.createSequence(name));
        } catch (final SecurityException
                | NoSuchMethodException
                | IllegalArgumentException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException e) {
            LOGGER.error(e.toString(), e);
        }
        return null;
    }

    /**
     * Creates a list of PatternFormatter from the given configuration and options or null if no pattern is supplied.
     *
     * @param config A configuration.
     * @param options pattern options.
     * @return a list of PatternFormatter from the given configuration and options or null if no pattern is supplied.
     */
    private static List<PatternFormatter> toPatternFormatterList(final Configuration config, final String[] options) {
        if (options.length == 0 || options[0] == null) {
            LOGGER.error("No pattern supplied on style for config=" + config);
            return null;
        }
        final PatternParser parser = PatternLayout.createPatternParser(config);
        if (parser == null) {
            LOGGER.error("No PatternParser created for config=" + config + ", options=" + Arrays.toString(options));
            return null;
        }
        return parser.parse(options[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @PerformanceSensitive("allocation")
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        final int start = toAppendTo.length();
        for (int i = 0; i < formatters.size(); i++) {
            final PatternFormatter formatter = formatters.get(i);
            formatter.format(event, toAppendTo);
        }
        if (toAppendTo.length() > start) {
            toAppendTo.insert(start, style);
            toAppendTo.append(AnsiEscape.getDefaultStyle());
        }
    }
}
