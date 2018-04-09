package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.pattern.PatternParser;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.StringBuilderFormattable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Key/value based layout configuration. Basically a list of PatternLayouts associated toa  key
 * <p>
 * The goal of this class is to {@link org.apache.logging.log4j.core.Layout#toByteArray format} a {@link LogEvent} and
 * return the results. The format of the result depends on the configured key/values, plus a "message" key/value. This
 * will be a single key/value in the case of most log messages, or multiple in the case of MapMessages
 * </p>
 * <p>
 * See the Log4j Manual section on PatternLayout for details on the supported pattern converters.
 * </p>
 */
@Plugin(name = "KeyValueLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE, printObject = true)
public class KeyValueLayout extends AbstractStringLayout {

    /**
     * Key to identify pattern converters.
     */
    public static final String KEY = "Converter";
    private Serializer serializer;

    protected KeyValueLayout(
        Configuration configuration,
        Charset charset,
        String messageKey,
        Map<String, List<PatternFormatter>> additionalFields,
        List<PatternFormatter> endOfLinePattern,
        boolean encloseInQuotes,
        boolean escapeQuotes,
        char escapeChar
    ) {
        super(configuration, charset, null, null);
        serializer = new KeyValueSerializer(additionalFields, messageKey, encloseInQuotes, escapeQuotes, escapeChar, endOfLinePattern);
    }

    /**
     * Creates a PatternParser.
     *
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

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public String toSerializable(LogEvent event) {

        return serializer.toSerializable(event);
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<KeyValueLayout> {

        @PluginElement("AdditionalField")
        private List<KeyValuePair> additionalFields = new ArrayList<KeyValuePair>();

        @PluginBuilderAttribute
        private String messageKey = "message";

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private String endOfLinePattern = "%n";

        @PluginBuilderAttribute
        private boolean encloseInQuotes = true;

        @PluginBuilderAttribute
        private boolean escapeQuotes = true;

        @PluginBuilderAttribute
        private String elementSeparator = " ";

        @PluginBuilderAttribute
        private char escapeChar = '\\';

        public Builder withMessageKey(final String messageKey) {
            this.messageKey = messageKey;
            return this;
        }

        public Builder withElementSeparator(final String elementSeparator) {
            this.elementSeparator = elementSeparator;
            return this;
        }

        public Builder withConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder withKeyValueFields(final KeyValuePair[] additionalFields) {
            this.additionalFields = Arrays.asList(additionalFields);
            for (KeyValuePair additionalField : additionalFields) {
                this.additionalFields.add(additionalField);
            }
            return this;
        }

        public Builder withKeyValue(KeyValuePair keyValue) {
            this.additionalFields.add(keyValue);
            return this;

        }

        public Builder withEncloseInQuotes(boolean encloseInQuotes) {
            this.encloseInQuotes = encloseInQuotes;
            return this;
        }

        public Builder withEscapeQuotes(boolean escapeQuotes) {
            this.escapeQuotes = escapeQuotes;
            return this;
        }

        public Builder withEscapeChar(char escapeChar) {
            this.escapeChar = escapeChar;
            return this;
        }

        public Builder setEndOfLinePattern(String endOfLinePattern) {
            this.endOfLinePattern = endOfLinePattern;
            return this;
        }

        @Override
        public KeyValueLayout build() {

            if (configuration == null) {
                configuration = new DefaultConfiguration();
            }

            PatternParser patternParser = createPatternParser(configuration);

            Map<String, List<PatternFormatter>> formatters = new HashMap<>();

            for (int i = 0; i < additionalFields.size(); i++) {
                KeyValuePair pair = additionalFields.get(i);
                formatters.put(
                    pair.getKey(),
                    patternParser.parse(pair.getValue(), false, false)
                );
            }

            return new KeyValueLayout(
                configuration,
                StandardCharsets.UTF_8,
                messageKey,
                formatters,
                patternParser.parse(endOfLinePattern, false, false),
                encloseInQuotes,
                escapeQuotes,
                escapeChar
            );
        }

    }

    private static class KeyValueSerializer implements Serializer, Serializer2 {

        private final Map<String, List<PatternFormatter>> formatters;
        private final String messageKey;
        private final boolean encloseInQuotes;
        private final boolean escapeQuotes;
        private final char quoteEscapeChar;
        private final List<PatternFormatter> endOfLinePattern;

        // We use a global mutable StringBuilder and just reset the size to 0 to avoid unnecessary allocations
        private final StringBuilder patternBuilder = new StringBuilder();

        private KeyValueSerializer(
            final Map<String, List<PatternFormatter>> formatters,
            final String messageKey,
            boolean encloseInQuotes, boolean escapeQuotes,
            char quoteEscapeChar,
            List<PatternFormatter> endOfLinePattern
        ) {
            super();
            this.formatters = formatters;
            this.messageKey = messageKey;
            this.encloseInQuotes = encloseInQuotes;
            this.escapeQuotes = escapeQuotes;
            this.quoteEscapeChar = quoteEscapeChar;
            this.endOfLinePattern = endOfLinePattern;
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

            for (Map.Entry<String, List<PatternFormatter>> entry : formatters.entrySet()) {
                buffer.append(entry.getKey());
                buffer.append("=");
                if (encloseInQuotes) {
                    buffer.append("\"");
                }
                patternBuilder.setLength(0);
                for (PatternFormatter formatter : entry.getValue()) {
                    formatter.format(event, patternBuilder);
                }
                if (escapeQuotes) {
                    // Iterate backwards because we're screwing with the size of the buffer
                    int i = patternBuilder.length() - 1;
                    while (i >= 0) {
                        if (patternBuilder.charAt(i) == '"') {
                            patternBuilder.insert(i, quoteEscapeChar);
                        }
                        i--;
                    }
                }
                buffer.append(patternBuilder);
                if (encloseInQuotes) {
                    buffer.append("\"");
                }
                buffer.append(" ");
            }

            Message message = event.getMessage();
            if (message instanceof MapMessage) {
                IndexedReadOnlyStringMap data = ((MapMessage) message).getIndexedReadOnlyStringMap();
                boolean first = true;
                for (int i = 0; i < data.size(); i++) {
                    // There's already a space added previously from the loop above (or there's no space needed)
                    if (!first) buffer.append(" ");
                    first = false;
                    String mapKey = data.getKeyAt(i);
                    String mapValue = data.getValueAt(i);

                    buffer.append(mapKey);
                    buffer.append("=");
                    if (encloseInQuotes) {
                        buffer.append("\"");
                    }
                    buffer.append(mapValue.replace("\"", String.valueOf(quoteEscapeChar) + "\""));
                    if (encloseInQuotes) {
                        buffer.append("\"");
                    }

                }

            } else {
                buffer.append(this.messageKey);
                buffer.append("=");
                if (encloseInQuotes) {
                    buffer.append("\"");
                }
                if (message instanceof StringBuilderFormattable) {

                    int currentBuilderSize = buffer.length();
                    ((StringBuilderFormattable) message).formatTo(buffer);

                    if (escapeQuotes) {
                        // Iterate backwards because we're screwing with the size of the buffer
                        // We iterate from the end of the buffer, but only covering the newly
                        // added characters. This avoids some intermediary allocations of Strings
                        // and StringBuilders
                        int i = buffer.length() - 1;
                        while (i >= currentBuilderSize) {
                            if (buffer.charAt(i) == '"') {
                                patternBuilder.insert(i, quoteEscapeChar);
                            }
                            i--;
                        }
                    }
                } else {
                    buffer.append(message.getFormattedMessage().replace("\"", String.valueOf(quoteEscapeChar) + "\""));

                }

                if (encloseInQuotes) {
                    buffer.append("\"");
                }
            }
            for (PatternFormatter eol : endOfLinePattern) {
                eol.format(event, buffer);
            }
            return buffer;
        }

        @Override
        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append(super.toString());
            builder.append("[formatters=");
            builder.append(formatters);
            builder.append("]");
            return builder.toString();
        }
    }
}
