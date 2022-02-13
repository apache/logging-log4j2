package co.elastic.logging.log4j2;


import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.pattern.PatternFormatter;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.MultiformatMessage;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.logging.log4j.util.StringBuilderFormattable;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Plugin(name = "LcsLayout", category = Node.CATEGORY, elementType = Layout.ELEMENT_TYPE)
public class LcsLayout extends AbstractStringLayout {

    public static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final ObjectMessageJacksonSerializer JACKSON_SERIALIZER = ObjectMessageJacksonSerializer.Resolver.resolve();
    private static final MdcSerializer MDC_SERIALIZER = MdcSerializer.Resolver.resolve();
    private static final MultiFormatHandler MULTI_FORMAT_HANDLER = MultiFormatHandler.Resolver.resolve();

    private final KeyValuePair[] additionalFields;
    private final PatternFormatter[][] fieldValuePatternFormatter;
    private final boolean stackTraceAsArray;
    private final String serviceName;
    private final String eventDataset;
    private final boolean includeMarkers;
    private final boolean includeOrigin;
    private final ConcurrentMap<Class<? extends MultiformatMessage>, Boolean> supportsJson = new ConcurrentHashMap<Class<? extends MultiformatMessage>, Boolean>();

    private LcsLayout(Configuration config, String serviceName, String eventDataset, boolean includeMarkers, KeyValuePair[] additionalFields, boolean includeOrigin, boolean stackTraceAsArray) {
        super(config, UTF_8, null, null);
        this.serviceName = serviceName;
        this.eventDataset = eventDataset;
        this.includeMarkers = includeMarkers;
        this.includeOrigin = includeOrigin;
        this.stackTraceAsArray = stackTraceAsArray;
        this.additionalFields = additionalFields;
        fieldValuePatternFormatter = new PatternFormatter[additionalFields.length][];
        for (int i = 0; i < additionalFields.length; i++) {
            KeyValuePair additionalField = additionalFields[i];
            if (additionalField.getValue().contains("%")) {
                fieldValuePatternFormatter[i] = PatternLayout.createPatternParser(config)
                        .parse(additionalField.getValue())
                        .toArray(new PatternFormatter[0]);
            }
        }
    }

    @PluginBuilderFactory
    public static LcsLayout.Builder newBuilder() {
        return new LcsLayout.Builder();
    }

    private static boolean valueNeedsLookup(final String value) {
        return value != null && value.contains("${");
    }

    @Override
    public String toSerializable(LogEvent event) {
        final StringBuilder text = toText(event, getStringBuilder(), false);
        return text.toString();
    }

    @Override
    public void encode(LogEvent event, ByteBufferDestination destination) {
        final StringBuilder text = toText(event, getStringBuilder(), true);
        final Encoder<StringBuilder> helper = getStringBuilderEncoder();
        helper.encode(text, destination);
    }

    @Override
    public String getContentType() {
        return "application/json";
    }

    private StringBuilder toText(LogEvent event, StringBuilder builder, boolean gcFree) {
        builder.append('{');//EcsJsonSerializer.serializeObjectStart(builder, event.getTimeMillis());
//        EcsJsonSerializer.serializeLogLevel(builder, event.getLevel().toString());
//        serializeMessage(builder, gcFree, event.getMessage(), event.getThrown());
//        EcsJsonSerializer.serializeEcsVersion(builder);
//        EcsJsonSerializer.serializeServiceName(builder, serviceName);
//        EcsJsonSerializer.serializeEventDataset(builder, eventDataset);
//        EcsJsonSerializer.serializeThreadName(builder, event.getThreadName());
//        EcsJsonSerializer.serializeLoggerName(builder, event.getLoggerName());
//        serializeAdditionalFieldsAndMDC(event, builder);
//        serializeTags(event, builder);
//        if (includeOrigin) {
//            EcsJsonSerializer.serializeOrigin(builder, event.getSource());
//        }
//        EcsJsonSerializer.serializeException(builder, event.getThrown(), stackTraceAsArray);
        EcsJsonSerializer.serializeObjectEnd(builder);
        return builder;
    }

    private void serializeAdditionalFieldsAndMDC(LogEvent event, StringBuilder builder) {
        final int length = additionalFields.length;
        if (length > 0) {
            final StrSubstitutor strSubstitutor = getConfiguration().getStrSubstitutor();
            for (int i = 0; i < length; i++) {
                KeyValuePair additionalField = additionalFields[i];
                PatternFormatter[] formatters = fieldValuePatternFormatter[i];
                CharSequence value = null;
                if (formatters != null) {
                    StringBuilder buffer = EcsJsonSerializer.getMessageStringBuilder();
                    formatPattern(event, formatters, buffer);
                    if (buffer.length() > 0) {
                        value = buffer;
                    }
                } else if (valueNeedsLookup(additionalField.getValue())) {
                    StringBuilder lookupValue = EcsJsonSerializer.getMessageStringBuilder();
                    lookupValue.append(additionalField.getValue());
                    if (strSubstitutor.replaceIn(event, lookupValue)) {
                        value = lookupValue;
                    }
                } else {
                    value = additionalField.getValue();
                }

                if (value != null) {
                    builder.append('\"');
                    JsonUtils.quoteAsString(additionalField.getKey(), builder);
                    builder.append("\":\"");
                    JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(value), builder);
                    builder.append("\",");
                }
            }
        }
        MDC_SERIALIZER.serializeMdc(event, builder);
    }

    private static void formatPattern(LogEvent event, PatternFormatter[] formatters, StringBuilder buffer) {
        final int len = formatters.length;
        for (int i = 0; i < len; i++) {
            formatters[i].format(event, buffer);
        }
    }

    private void serializeTags(LogEvent event, StringBuilder builder) {
        ThreadContext.ContextStack stack = event.getContextStack();
        List<String> contextStack;
        if (stack == null) {
            contextStack = Collections.emptyList();
        } else {
            contextStack = stack.asList();
        }
        Marker marker = event.getMarker();
        boolean hasTags = !contextStack.isEmpty() || (includeMarkers && marker != null);
        if (hasTags) {
            EcsJsonSerializer.serializeTagStart(builder);
        }

        if (!contextStack.isEmpty()) {
            final int len = contextStack.size();
            for (int i = 0; i < len; i++) {
                builder.append('\"');
                JsonUtils.quoteAsString(contextStack.get(i), builder);
                builder.append("\",");
            }
        }

        if (includeMarkers && marker != null) {
            serializeMarker(builder, marker);
        }

        if (hasTags) {
            EcsJsonSerializer.serializeTagEnd(builder);
        }
    }

    private void serializeMarker(StringBuilder builder, Marker marker) {
        EcsJsonSerializer.serializeSingleTag(builder, marker.getName());
        if (marker.hasParents()) {
            Marker[] parents = marker.getParents();
            for (int i = 0; i < parents.length; i++) {
                serializeMarker(builder, parents[i]);
            }
        }
    }

    private void serializeMessage(StringBuilder builder, boolean gcFree, Message message, Throwable thrown) {
        if (message instanceof MultiformatMessage) {
            MultiformatMessage multiformatMessage = (MultiformatMessage) message;
            if (supportsJson(multiformatMessage)) {
                serializeJsonMessage(builder, multiformatMessage);
            } else {
                serializeSimpleMessage(builder, gcFree, message, thrown);
            }
        } else if (JACKSON_SERIALIZER != null && message instanceof ObjectMessage) {
            final StringBuilder jsonBuffer = EcsJsonSerializer.getMessageStringBuilder();
            JACKSON_SERIALIZER.formatTo(jsonBuffer, (ObjectMessage) message);
            addJson(builder, jsonBuffer);
        } else {
            serializeSimpleMessage(builder, gcFree, message, thrown);
        }
    }

    private static void serializeJsonMessage(StringBuilder builder, MultiformatMessage message) {
        final StringBuilder messageBuffer = EcsJsonSerializer.getMessageStringBuilder();
        MULTI_FORMAT_HANDLER.formatJsonTo(message, messageBuffer);
        addJson(builder, messageBuffer);
    }

    private static void addJson(StringBuilder buffer, StringBuilder jsonBuffer) {
        if (isObject(jsonBuffer)) {
            moveToRoot(jsonBuffer);
            buffer.append(jsonBuffer);
            buffer.append(", ");
        } else {
            buffer.append("\"message\":");
            if (isString(jsonBuffer)) {
                buffer.append(jsonBuffer);
            } else {
                // message always has to be a string to avoid mapping conflicts
                buffer.append('"');
                JsonUtils.quoteAsString(jsonBuffer, buffer);
                buffer.append('"');
            }
            buffer.append(", ");
        }
    }

    private void serializeSimpleMessage(StringBuilder builder, boolean gcFree, Message message, Throwable thrown) {
        builder.append("\"message\":\"");
        if (message instanceof CharSequence) {
            JsonUtils.quoteAsString(((CharSequence) message), builder);
        } else if (gcFree && message instanceof StringBuilderFormattable) {
            final StringBuilder messageBuffer = EcsJsonSerializer.getMessageStringBuilder();
            try {
                ((StringBuilderFormattable) message).formatTo(messageBuffer);
                JsonUtils.quoteAsString(messageBuffer, builder);
            } finally {
                trimToMaxSizeCopy(messageBuffer);
            }
        } else {
            JsonUtils.quoteAsString(EcsJsonSerializer.toNullSafeString(message.getFormattedMessage()), builder);
        }
        builder.append("\", ");
    }

    static void trimToMaxSizeCopy(final StringBuilder stringBuilder) {
        if (stringBuilder.length() > MAX_STRING_BUILDER_SIZE) {
            stringBuilder.setLength(MAX_STRING_BUILDER_SIZE);
            stringBuilder.trimToSize();
        }
    }

    private static boolean isObject(StringBuilder messageBuffer) {
        return messageBuffer.length() > 1 && messageBuffer.charAt(0) == '{' && messageBuffer.charAt(messageBuffer.length() - 1) == '}';
    }

    private static boolean isString(StringBuilder messageBuffer) {
        return messageBuffer.length() > 1 && messageBuffer.charAt(0) == '"' && messageBuffer.charAt(messageBuffer.length() - 1) == '"';
    }

    private static void moveToRoot(StringBuilder messageBuffer) {
        messageBuffer.setCharAt(0, ' ');
        messageBuffer.setCharAt(messageBuffer.length() -1, ' ');
    }

    private boolean supportsJson(MultiformatMessage message) {
        Boolean supportsJson = this.supportsJson.get(message.getClass());
        if (supportsJson == null) {
            supportsJson = false;
            for (String format : message.getFormats()) {
                if (format.equalsIgnoreCase("JSON")) {
                    supportsJson = true;
                    break;
                }
            }
            this.supportsJson.put(message.getClass(), supportsJson);
        }
        return supportsJson;
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<LcsLayout> {

        @PluginConfiguration
        private Configuration configuration;
        @PluginBuilderAttribute("serviceName")
        private String serviceName;
        @PluginBuilderAttribute("eventDataset")
        private String eventDataset;
        @PluginBuilderAttribute("includeMarkers")
        private boolean includeMarkers = false;
        @PluginBuilderAttribute("stackTraceAsArray")
        private boolean stackTraceAsArray = false;
        @PluginElement("AdditionalField")
        private KeyValuePair[] additionalFields = new KeyValuePair[]{};
        @PluginBuilderAttribute("includeOrigin")
        private boolean includeOrigin = false;

        Builder() {
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public LcsLayout.Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public KeyValuePair[] getAdditionalFields() {
            return additionalFields.clone();
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getEventDataset() {
            return eventDataset;
        }

        public boolean isIncludeMarkers() {
            return includeMarkers;
        }

        public boolean isIncludeOrigin() {
            return includeOrigin;
        }

        /**
         * Additional fields to set on each log event.
         *
         * @return this builder
         */
        public LcsLayout.Builder setAdditionalFields(final KeyValuePair[] additionalFields) {
            this.additionalFields = additionalFields.clone();
            return this;
        }

        public LcsLayout.Builder setServiceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public LcsLayout.Builder setEventDataset(String eventDataset) {
            this.eventDataset = eventDataset;
            return this;
        }

        public LcsLayout.Builder setIncludeMarkers(final boolean includeMarkers) {
            this.includeMarkers = includeMarkers;
            return this;
        }

        public LcsLayout.Builder setIncludeOrigin(final boolean includeOrigin) {
            this.includeOrigin = includeOrigin;
            return this;
        }

        public LcsLayout.Builder setStackTraceAsArray(boolean stackTraceAsArray) {
            this.stackTraceAsArray = stackTraceAsArray;
            return this;
        }

        @Override
        public LcsLayout build() {
            return new LcsLayout(getConfiguration(), serviceName, EcsJsonSerializer.computeEventDataset(eventDataset, serviceName), includeMarkers, additionalFields, includeOrigin, stackTraceAsArray);
        }

        public boolean isStackTraceAsArray() {
            return stackTraceAsArray;
        }
    }
}
