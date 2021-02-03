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
package org.apache.logging.log4j.layout.template.json;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;
import org.apache.logging.log4j.core.layout.LockingStringBuilderEncoder;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.StringEncoder;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverContext;
import org.apache.logging.log4j.layout.template.json.resolver.StackTraceElementObjectResolverContext;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolver;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolvers;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.Recycler;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactory;
import org.apache.logging.log4j.layout.template.json.util.Uris;
import org.apache.logging.log4j.util.Strings;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Plugin(name = "JsonTemplateLayout",
        category = Node.CATEGORY,
        elementType = Layout.ELEMENT_TYPE)
public class JsonTemplateLayout implements StringLayout {

    private static final Map<String, String> CONTENT_FORMAT =
            Collections.singletonMap("version", "1");

    private final Charset charset;

    private final String contentType;

    private final TemplateResolver<LogEvent> eventResolver;

    private final String eventDelimiter;

    private final Recycler<Context> contextRecycler;

    // The class and fields are visible for tests.
    static final class Context implements AutoCloseable {

        final JsonWriter jsonWriter;

        final Encoder<StringBuilder> encoder;

        private Context(
                final JsonWriter jsonWriter,
                final Encoder<StringBuilder> encoder) {
            this.jsonWriter = jsonWriter;
            this.encoder = encoder;
        }

        @Override
        public void close() {
            jsonWriter.close();
        }

    }

    private JsonTemplateLayout(final Builder builder) {
        this.charset = builder.charset;
        this.contentType = "application/json; charset=" + charset;
        final String eventDelimiterSuffix = builder.isNullEventDelimiterEnabled() ? "\0" : "";
        this.eventDelimiter = builder.eventDelimiter + eventDelimiterSuffix;
        final Configuration configuration = builder.configuration;
        final StrSubstitutor substitutor = configuration.getStrSubstitutor();
        final JsonWriter jsonWriter = JsonWriter
                .newBuilder()
                .setMaxStringLength(builder.maxStringLength)
                .setTruncatedStringSuffix(builder.truncatedStringSuffix)
                .build();
        final TemplateResolver<StackTraceElement> stackTraceElementObjectResolver =
                builder.stackTraceEnabled
                        ? createStackTraceElementResolver(builder, substitutor, jsonWriter)
                        : null;
        this.eventResolver = createEventResolver(
                builder,
                configuration,
                substitutor,
                charset,
                jsonWriter,
                stackTraceElementObjectResolver);
        this.contextRecycler = createContextRecycler(builder, jsonWriter);
    }

    private static TemplateResolver<StackTraceElement> createStackTraceElementResolver(
            final Builder builder,
            final StrSubstitutor substitutor,
            final JsonWriter jsonWriter) {
        final StackTraceElementObjectResolverContext stackTraceElementObjectResolverContext =
                StackTraceElementObjectResolverContext
                        .newBuilder()
                        .setSubstitutor(substitutor)
                        .setJsonWriter(jsonWriter)
                        .build();
        final String stackTraceElementTemplate = readStackTraceElementTemplate(builder);
        return TemplateResolvers.ofTemplate(stackTraceElementObjectResolverContext, stackTraceElementTemplate);
    }

    private TemplateResolver<LogEvent> createEventResolver(
            final Builder builder,
            final Configuration configuration,
            final StrSubstitutor substitutor,
            final Charset charset,
            final JsonWriter jsonWriter,
            final TemplateResolver<StackTraceElement> stackTraceElementObjectResolver) {
        final String eventTemplate = readEventTemplate(builder);
        final float maxByteCountPerChar = builder.charset.newEncoder().maxBytesPerChar();
        final int maxStringByteCount =
                Math.toIntExact(Math.round(Math.ceil(
                        maxByteCountPerChar * builder.maxStringLength)));
        final EventTemplateAdditionalField[] eventTemplateAdditionalFields =
                builder.eventTemplateAdditionalFields != null
                        ? builder.eventTemplateAdditionalFields
                        : new EventTemplateAdditionalField[0];
        final EventResolverContext resolverContext = EventResolverContext
                .newBuilder()
                .setConfiguration(configuration)
                .setSubstitutor(substitutor)
                .setCharset(charset)
                .setJsonWriter(jsonWriter)
                .setRecyclerFactory(builder.recyclerFactory)
                .setMaxStringByteCount(maxStringByteCount)
                .setTruncatedStringSuffix(builder.truncatedStringSuffix)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setStackTraceElementObjectResolver(stackTraceElementObjectResolver)
                .setEventTemplateRootObjectKey(builder.eventTemplateRootObjectKey)
                .setEventTemplateAdditionalFields(eventTemplateAdditionalFields)
                .build();
        return TemplateResolvers.ofTemplate(resolverContext, eventTemplate);
    }

    private static String readEventTemplate(final Builder builder) {
        return readTemplate(
                builder.eventTemplate,
                builder.eventTemplateUri,
                builder.charset);
    }

    private static String readStackTraceElementTemplate(final Builder builder) {
        return readTemplate(
                builder.stackTraceElementTemplate,
                builder.stackTraceElementTemplateUri,
                builder.charset);
    }

    private static String readTemplate(
            final String template,
            final String templateUri,
            final Charset charset) {
        return Strings.isBlank(template)
                ? Uris.readUri(templateUri, charset)
                : template;
    }

    private static Recycler<Context> createContextRecycler(
            final Builder builder,
            final JsonWriter jsonWriter) {
        final Supplier<Context> supplier =
                createContextSupplier(builder.charset, jsonWriter);
        return builder
                .recyclerFactory
                .create(supplier, Context::close);
    }

    private static Supplier<Context> createContextSupplier(
            final Charset charset,
            final JsonWriter jsonWriter) {
        return () -> {
            final JsonWriter clonedJsonWriter = jsonWriter.clone();
            final Encoder<StringBuilder> encoder =
                    Constants.ENABLE_DIRECT_ENCODERS
                            ? new LockingStringBuilderEncoder(charset)
                            : null;
            return new Context(clonedJsonWriter, encoder);
        };
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        final String eventJson = toSerializable(event);
        return StringEncoder.toBytes(eventJson, charset);
    }

    @Override
    public String toSerializable(final LogEvent event) {
        final Context context = acquireContext();
        final JsonWriter jsonWriter = context.jsonWriter;
        final StringBuilder stringBuilder = jsonWriter.getStringBuilder();
        try {
            eventResolver.resolve(event, jsonWriter);
            stringBuilder.append(eventDelimiter);
            return stringBuilder.toString();
        } finally {
            contextRecycler.release(context);
        }
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {

        // Acquire a context.
        final Context context = acquireContext();
        final JsonWriter jsonWriter = context.jsonWriter;
        final StringBuilder stringBuilder = jsonWriter.getStringBuilder();
        final Encoder<StringBuilder> encoder = context.encoder;

        try {

            // Render the JSON.
            eventResolver.resolve(event, jsonWriter);
            stringBuilder.append(eventDelimiter);

            // Write to the destination.
            if (encoder == null) {
                final String eventJson = stringBuilder.toString();
                final byte[] eventJsonBytes = StringEncoder.toBytes(eventJson, charset);
                destination.writeBytes(eventJsonBytes, 0, eventJsonBytes.length);
            } else {
                encoder.encode(stringBuilder, destination);
            }

        }

        // Release the context.
        finally {
            contextRecycler.release(context);
        }

    }

    // Visible for tests.
    Context acquireContext() {
        return contextRecycler.acquire();
    }

    @Override
    public byte[] getFooter() {
        return null;
    }

    @Override
    public byte[] getHeader() {
        return null;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public Map<String, String> getContentFormat() {
        return CONTENT_FORMAT;
    }

    @PluginBuilderFactory
    @SuppressWarnings("WeakerAccess")
    public static Builder newBuilder() {
        return new Builder();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final class Builder
            implements org.apache.logging.log4j.core.util.Builder<JsonTemplateLayout> {

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private Charset charset = JsonTemplateLayoutDefaults.getCharset();

        @PluginBuilderAttribute
        private boolean locationInfoEnabled =
                JsonTemplateLayoutDefaults.isLocationInfoEnabled();

        @PluginBuilderAttribute
        private boolean stackTraceEnabled =
                JsonTemplateLayoutDefaults.isStackTraceEnabled();

        @PluginBuilderAttribute
        private String eventTemplate = JsonTemplateLayoutDefaults.getEventTemplate();

        @PluginBuilderAttribute
        private String eventTemplateUri =
                JsonTemplateLayoutDefaults.getEventTemplateUri();

        @PluginBuilderAttribute
        private String eventTemplateRootObjectKey =
                JsonTemplateLayoutDefaults.getEventTemplateRootObjectKey();

        @PluginElement("EventTemplateAdditionalField")
        private EventTemplateAdditionalField[] eventTemplateAdditionalFields;

        @PluginBuilderAttribute
        private String stackTraceElementTemplate =
                JsonTemplateLayoutDefaults.getStackTraceElementTemplate();

        @PluginBuilderAttribute
        private String stackTraceElementTemplateUri =
                JsonTemplateLayoutDefaults.getStackTraceElementTemplateUri();

        @PluginBuilderAttribute
        private String eventDelimiter = JsonTemplateLayoutDefaults.getEventDelimiter();

        @PluginBuilderAttribute
        private boolean nullEventDelimiterEnabled =
                JsonTemplateLayoutDefaults.isNullEventDelimiterEnabled();

        @PluginBuilderAttribute
        private int maxStringLength = JsonTemplateLayoutDefaults.getMaxStringLength();

        @PluginBuilderAttribute
        private String truncatedStringSuffix =
                JsonTemplateLayoutDefaults.getTruncatedStringSuffix();

        @PluginBuilderAttribute
        private RecyclerFactory recyclerFactory =
                JsonTemplateLayoutDefaults.getRecyclerFactory();

        private Builder() {
            // Do nothing.
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Charset getCharset() {
            return charset;
        }

        public Builder setCharset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        public boolean isLocationInfoEnabled() {
            return locationInfoEnabled;
        }

        public Builder setLocationInfoEnabled(final boolean locationInfoEnabled) {
            this.locationInfoEnabled = locationInfoEnabled;
            return this;
        }

        public boolean isStackTraceEnabled() {
            return stackTraceEnabled;
        }

        public Builder setStackTraceEnabled(final boolean stackTraceEnabled) {
            this.stackTraceEnabled = stackTraceEnabled;
            return this;
        }

        public String getEventTemplate() {
            return eventTemplate;
        }

        public Builder setEventTemplate(final String eventTemplate) {
            this.eventTemplate = eventTemplate;
            return this;
        }

        public String getEventTemplateUri() {
            return eventTemplateUri;
        }

        public Builder setEventTemplateUri(final String eventTemplateUri) {
            this.eventTemplateUri = eventTemplateUri;
            return this;
        }

        public String getEventTemplateRootObjectKey() {
            return eventTemplateRootObjectKey;
        }

        public Builder setEventTemplateRootObjectKey(String eventTemplateRootObjectKey) {
            this.eventTemplateRootObjectKey = eventTemplateRootObjectKey;
            return this;
        }

        public EventTemplateAdditionalField[] getEventTemplateAdditionalFields() {
            return eventTemplateAdditionalFields;
        }

        public Builder setEventTemplateAdditionalFields(
                final EventTemplateAdditionalField[] eventTemplateAdditionalFields) {
            this.eventTemplateAdditionalFields = eventTemplateAdditionalFields;
            return this;
        }

        public String getStackTraceElementTemplate() {
            return stackTraceElementTemplate;
        }

        public Builder setStackTraceElementTemplate(
                final String stackTraceElementTemplate) {
            this.stackTraceElementTemplate = stackTraceElementTemplate;
            return this;
        }

        public String getStackTraceElementTemplateUri() {
            return stackTraceElementTemplateUri;
        }

        public Builder setStackTraceElementTemplateUri(
                final String stackTraceElementTemplateUri) {
            this.stackTraceElementTemplateUri = stackTraceElementTemplateUri;
            return this;
        }

        public String getEventDelimiter() {
            return eventDelimiter;
        }

        public Builder setEventDelimiter(final String eventDelimiter) {
            this.eventDelimiter = eventDelimiter;
            return this;
        }

        public boolean isNullEventDelimiterEnabled() {
            return nullEventDelimiterEnabled;
        }

        public Builder setNullEventDelimiterEnabled(
                final boolean nullEventDelimiterEnabled) {
            this.nullEventDelimiterEnabled = nullEventDelimiterEnabled;
            return this;
        }

        public int getMaxStringLength() {
            return maxStringLength;
        }

        public Builder setMaxStringLength(final int maxStringLength) {
            this.maxStringLength = maxStringLength;
            return this;
        }

        public String getTruncatedStringSuffix() {
            return truncatedStringSuffix;
        }

        public Builder setTruncatedStringSuffix(final String truncatedStringSuffix) {
            this.truncatedStringSuffix = truncatedStringSuffix;
            return this;
        }

        public RecyclerFactory getRecyclerFactory() {
            return recyclerFactory;
        }

        public Builder setRecyclerFactory(final RecyclerFactory recyclerFactory) {
            this.recyclerFactory = recyclerFactory;
            return this;
        }

        @Override
        public JsonTemplateLayout build() {
            validate();
            return new JsonTemplateLayout(this);
        }

        private void validate() {
            Objects.requireNonNull(configuration, "config");
            if (Strings.isBlank(eventTemplate) && Strings.isBlank(eventTemplateUri)) {
                    throw new IllegalArgumentException(
                            "both eventTemplate and eventTemplateUri are blank");
            }
            if (stackTraceEnabled &&
                    Strings.isBlank(stackTraceElementTemplate)
                    && Strings.isBlank(stackTraceElementTemplateUri)) {
                throw new IllegalArgumentException(
                        "both stackTraceElementTemplate and stackTraceElementTemplateUri are blank");
            }
            if (maxStringLength <= 0) {
                throw new IllegalArgumentException(
                        "was expecting a non-zero positive maxStringLength: " +
                                maxStringLength);
            }
            Objects.requireNonNull(truncatedStringSuffix, "truncatedStringSuffix");
            Objects.requireNonNull(recyclerFactory, "recyclerFactory");
        }

    }

    @Plugin(name = "EventTemplateAdditionalField",
            category = Node.CATEGORY,
            printObject = true)
    public static final class EventTemplateAdditionalField {

        public enum Format { STRING, JSON }

        private final String key;

        private final String value;

        private final Format format;

        private EventTemplateAdditionalField(final Builder builder) {
            this.key = builder.key;
            this.value = builder.value;
            this.format = builder.format;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public Format getFormat() {
            return format;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            EventTemplateAdditionalField that = (EventTemplateAdditionalField) object;
            return key.equals(that.key) &&
                    value.equals(that.value) &&
                    format == that.format;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value, format);
        }

        @Override
        public String toString() {
            final String formattedValue = Format.STRING.equals(format)
                    ? String.format("\"%s\"", value)
                    : value;
            return String.format("%s=%s", key, formattedValue);
        }

        @PluginBuilderFactory
        public static EventTemplateAdditionalField.Builder newBuilder() {
            return new EventTemplateAdditionalField.Builder();
        }

        public static class Builder
                implements org.apache.logging.log4j.core.util.Builder<EventTemplateAdditionalField> {

            @PluginBuilderAttribute
            private String key;

            @PluginBuilderAttribute
            private String value;

            @PluginBuilderAttribute
            private Format format = Format.STRING;

            public Builder setKey(final String key) {
                this.key = key;
                return this;
            }

            public Builder setValue(final String value) {
                this.value = value;
                return this;
            }

            public Builder setFormat(final Format format) {
                this.format = format;
                return this;
            }

            @Override
            public EventTemplateAdditionalField build() {
                validate();
                return new EventTemplateAdditionalField(this);
            }

            private void validate() {
                if (Strings.isBlank(key)) {
                    throw new IllegalArgumentException("blank key");
                }
                if (Strings.isBlank(value)) {
                    throw new IllegalArgumentException("blank value");
                }
                Objects.requireNonNull(format, "format");
            }

        }

    }

}
