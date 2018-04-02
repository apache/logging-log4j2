package org.apache.logging.log4j.core;

import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.util.StringBuilders;

/**
 * {@link TestPatternConverters} provides {@link LogEventPatternConverter} implementations that may be
 * useful in tests.
 */
public final class TestPatternConverters {
    private TestPatternConverters() {
    }

    @Plugin(name = "TestParametersPatternConverter", category = "Converter")
    @ConverterKeys("testparameters")
    public static final class TestParametersPatternConverter extends LogEventPatternConverter {

        private TestParametersPatternConverter() {
            super("Parameters", "testparameters");
        }

        public static TestParametersPatternConverter newInstance(final String[] options) {
            return new TestParametersPatternConverter();
        }

        @Override
        public void format(final LogEvent event, final StringBuilder toAppendTo) {
            toAppendTo.append('[');
            Object[] parameters = event.getMessage().getParameters();
            if (parameters != null) {
                for (int i = 0; i < parameters.length; i++) {
                    StringBuilders.appendValue(toAppendTo, parameters[i]);
                    if (i != parameters.length - 1) {
                        toAppendTo.append(',');
                    }
                }
            }
            toAppendTo.append(']');
        }
    }

    @Plugin(name = "TestFormatPatternConverter", category = "Converter")
    @ConverterKeys("testformat")
    public static final class TestFormatPatternConverter extends LogEventPatternConverter {

        private TestFormatPatternConverter() {
            super("Format", "testformat");
        }

        public static TestFormatPatternConverter newInstance(final String[] options) {
            return new TestFormatPatternConverter();
        }

        @Override
        public void format(final LogEvent event, final StringBuilder toAppendTo) {
            toAppendTo.append(event.getMessage().getFormat());
        }
    }

}
