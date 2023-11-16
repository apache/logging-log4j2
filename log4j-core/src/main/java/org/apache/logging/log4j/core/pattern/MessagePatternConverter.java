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

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.MultiFormatStringBuilderFormattable;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilderFormattable;
import org.apache.logging.log4j.util.Strings;

/**
 * Returns the event's rendered message in a StringBuilder.
 */
@Plugin(name = "MessagePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({"m", "msg", "message"})
@PerformanceSensitive("allocation")
public class MessagePatternConverter extends LogEventPatternConverter {

    private static final String LOOKUPS = "lookups";
    private static final String NOLOOKUPS = "nolookups";

    private MessagePatternConverter() {
        super("Message", "message");
    }

    private static TextRenderer loadMessageRenderer(final String[] options) {
        if (options != null) {
            for (final String option : options) {
                switch (toRootUpperCase(option)) {
                    case "ANSI":
                        if (Loader.isJansiAvailable()) {
                            return new JAnsiTextRenderer(options, JAnsiTextRenderer.DefaultMessageStyleMap);
                        }
                        StatusLogger.getLogger()
                                .warn("You requested ANSI message rendering but JANSI is not on the classpath.");
                        return null;
                    case "HTML":
                        return new HtmlTextRenderer(options);
                }
            }
        }
        return null;
    }

    /**
     * Obtains an instance of pattern converter.
     *
     * @param config
     *            The Configuration.
     * @param options
     *            options, may be null.
     * @return instance of pattern converter.
     */
    public static MessagePatternConverter newInstance(final Configuration config, final String[] options) {
        final String[] formats = withoutLookupOptions(options);
        final TextRenderer textRenderer = loadMessageRenderer(formats);
        MessagePatternConverter result = formats == null || formats.length == 0
                ? SimpleMessagePatternConverter.INSTANCE
                : new FormattedMessagePatternConverter(formats);
        if (textRenderer != null) {
            result = new RenderingPatternConverter(result, textRenderer);
        }
        return result;
    }

    private static String[] withoutLookupOptions(final String[] options) {
        if (options == null || options.length == 0) {
            return options;
        }
        final List<String> results = new ArrayList<>(options.length);
        for (String option : options) {
            if (LOOKUPS.equalsIgnoreCase(option) || NOLOOKUPS.equalsIgnoreCase(option)) {
                LOGGER.info("The {} option will be ignored. Message Lookups are no longer supported.", option);
            } else {
                results.add(option);
            }
        }
        return results.toArray(Strings.EMPTY_ARRAY);
    }

    @Override
    public void format(final LogEvent event, final StringBuilder toAppendTo) {
        throw new UnsupportedOperationException();
    }

    private static final class SimpleMessagePatternConverter extends MessagePatternConverter {
        private static final MessagePatternConverter INSTANCE = new SimpleMessagePatternConverter();

        /**
         * {@inheritDoc}
         */
        @Override
        public void format(final LogEvent event, final StringBuilder toAppendTo) {
            final Message msg = event.getMessage();
            if (msg instanceof StringBuilderFormattable) {
                ((StringBuilderFormattable) msg).formatTo(toAppendTo);
            } else if (msg != null) {
                toAppendTo.append(msg.getFormattedMessage());
            }
        }
    }

    private static final class FormattedMessagePatternConverter extends MessagePatternConverter {

        private final String[] formats;

        FormattedMessagePatternConverter(final String[] formats) {
            this.formats = formats;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void format(final LogEvent event, final StringBuilder toAppendTo) {
            final Message msg = event.getMessage();
            if (msg instanceof StringBuilderFormattable) {
                if (msg instanceof MultiFormatStringBuilderFormattable) {
                    ((MultiFormatStringBuilderFormattable) msg).formatTo(formats, toAppendTo);
                } else {
                    ((StringBuilderFormattable) msg).formatTo(toAppendTo);
                }
            } else if (msg != null) {
                toAppendTo.append(
                        msg instanceof MultiformatMessage
                                ? ((MultiformatMessage) msg).getFormattedMessage(formats)
                                : msg.getFormattedMessage());
            }
        }
    }

    private static final class RenderingPatternConverter extends MessagePatternConverter {

        private final MessagePatternConverter delegate;
        private final TextRenderer textRenderer;

        RenderingPatternConverter(final MessagePatternConverter delegate, final TextRenderer textRenderer) {
            this.delegate = delegate;
            this.textRenderer = textRenderer;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void format(final LogEvent event, final StringBuilder toAppendTo) {
            final StringBuilder workingBuilder = new StringBuilder(80);
            delegate.format(event, workingBuilder);
            textRenderer.render(workingBuilder, toAppendTo);
        }
    }
}
