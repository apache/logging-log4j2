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
package org.apache.logging.log4j.layout.json.template;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.ByteBufferDestinationHelper;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.layout.json.template.resolver.EventResolverContext;
import org.apache.logging.log4j.layout.json.template.resolver.StackTraceElementObjectResolverContext;
import org.apache.logging.log4j.layout.json.template.resolver.TemplateResolver;
import org.apache.logging.log4j.layout.json.template.resolver.TemplateResolvers;
import org.apache.logging.log4j.layout.json.template.util.JsonWriter;
import org.apache.logging.log4j.layout.json.template.util.ByteBufferOutputStream;
import org.apache.logging.log4j.layout.json.template.util.Recycler;
import org.apache.logging.log4j.layout.json.template.util.RecyclerFactory;
import org.apache.logging.log4j.layout.json.template.util.Uris;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.Strings;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Plugin(name = "JsonTemplateLayout",
        category = Node.CATEGORY,
        elementType = Layout.ELEMENT_TYPE,
        printObject = true)
public class JsonTemplateLayout implements StringLayout {

    private static final Map<String, String> CONTENT_FORMAT =
            Collections.singletonMap("version", "1");

    private final Charset charset;

    private final String contentType;

    private final byte[] emptyObjectJsonBytes;

    private final TemplateResolver<LogEvent> eventResolver;

    private final byte[] eventDelimiterBytes;

    private final Recycler<JsonWriter> jsonWriterRecycler;

    private JsonTemplateLayout(final Builder builder) {
        this.charset = builder.charset;
        this.contentType = "application/json; charset=" + charset;
        this.emptyObjectJsonBytes = "{}".getBytes(charset);
        this.eventDelimiterBytes = builder.eventDelimiter.getBytes(charset);
        final StrSubstitutor substitutor = builder.configuration.getStrSubstitutor();
        final TemplateResolver<StackTraceElement> stackTraceElementObjectResolver =
                builder.stackTraceEnabled
                        ? createStackTraceElementResolver(builder, substitutor)
                        : null;
        this.eventResolver = createEventResolver(
                builder,
                substitutor,
                stackTraceElementObjectResolver);
        this.jsonWriterRecycler = createJsonWriterRecycler(builder);
    }

    private static TemplateResolver<StackTraceElement> createStackTraceElementResolver(
            final Builder builder,
            final StrSubstitutor substitutor) {
        final StackTraceElementObjectResolverContext stackTraceElementObjectResolverContext =
                StackTraceElementObjectResolverContext
                        .newBuilder()
                        .setSubstitutor(substitutor)
                        .build();
        final String stackTraceElementTemplate = readStackTraceElementTemplate(builder);
        return TemplateResolvers.ofTemplate(stackTraceElementObjectResolverContext, stackTraceElementTemplate);
    }

    private TemplateResolver<LogEvent> createEventResolver(
            final Builder builder,
            final StrSubstitutor substitutor,
            final TemplateResolver<StackTraceElement> stackTraceElementObjectResolver) {
        final String eventTemplate = readEventTemplate(builder);
        final EventResolverContext resolverContext = EventResolverContext
                .newBuilder()
                .setSubstitutor(substitutor)
                .setRecyclerFactory(builder.recyclerFactory)
                .setMaxByteCount(builder.maxByteCount)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setStackTraceElementObjectResolver(stackTraceElementObjectResolver)
                .setMdcKeyPattern(builder.mdcKeyPattern)
                .setNdcPattern(builder.ndcPattern)
                .setEventTemplateAdditionalFields(builder.eventTemplateAdditionalFields.additionalFields)
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

    private static Recycler<JsonWriter> createJsonWriterRecycler(
            final Builder builder) {
        final Supplier<JsonWriter> supplier = createJsonWriterSupplier(builder);
        return builder
                .recyclerFactory
                .create(supplier, JsonWriter::close);
    }

    private static Supplier<JsonWriter>
    createJsonWriterSupplier(final Builder builder) {
        return () -> JsonWriter
                .newBuilder()
                .setCharset(builder.getCharset())
                .setMaxByteCount(builder.maxByteCount)
                .setMaxStringLength(builder.maxStringLength)
                .setTruncatedStringSuffix(builder.truncatedStringSuffix)
                .build();
    }

    @Override
    public String toSerializable(final LogEvent event) {
        try (final JsonWriter jsonWriter = acquireJsonWriter()) {
            encode(event, jsonWriter);
            final String encodedEvent = jsonWriter.getOutputStream().toString(charset);
            jsonWriterRecycler.release(jsonWriter);
            return encodedEvent;
        }
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        try (final JsonWriter jsonWriter = acquireJsonWriter()) {
            encode(event, jsonWriter);
            final byte[] encodedEvent = jsonWriter.getOutputStream().toByteArray();
            jsonWriterRecycler.release(jsonWriter);
            return encodedEvent;
        }
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        try (final JsonWriter jsonWriter = acquireJsonWriter()) {
            encode(event, jsonWriter);
            final ByteBuffer byteBuffer = jsonWriter.getOutputStream().getByteBuffer();
            byteBuffer.flip();
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (destination) {
                ByteBufferDestinationHelper.writeToUnsynchronized(byteBuffer, destination);
            }
            jsonWriterRecycler.release(jsonWriter);
        }
    }

    // Visible for tests.
    JsonWriter acquireJsonWriter() {
        return jsonWriterRecycler.acquire();
    }

    private void encode(
            final LogEvent event,
            final JsonWriter jsonWriter) {
        eventResolver.resolve(event, jsonWriter);
        ByteBufferOutputStream outputStream = jsonWriter.getOutputStream();
        if (outputStream.getByteBuffer().position() == 0) {
            outputStream.write(emptyObjectJsonBytes);
        }
        outputStream.write(eventDelimiterBytes);
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

        @PluginElement("EventTemplateAdditionalFields")
        private EventTemplateAdditionalFields eventTemplateAdditionalFields
                = EventTemplateAdditionalFields.EMPTY;

        @PluginBuilderAttribute
        private String stackTraceElementTemplate =
                JsonTemplateLayoutDefaults.getStackTraceElementTemplate();

        @PluginBuilderAttribute
        private String stackTraceElementTemplateUri =
                JsonTemplateLayoutDefaults.getStackTraceElementTemplateUri();

        @PluginBuilderAttribute
        private String mdcKeyPattern = JsonTemplateLayoutDefaults.getMdcKeyPattern();

        @PluginBuilderAttribute
        private String ndcPattern = JsonTemplateLayoutDefaults.getNdcPattern();

        @PluginBuilderAttribute
        private String eventDelimiter = JsonTemplateLayoutDefaults.getEventDelimiter();

        @PluginBuilderAttribute
        private int maxByteCount = JsonTemplateLayoutDefaults.getMaxByteCount();

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

        public EventTemplateAdditionalFields getEventTemplateAdditionalFields() {
            return eventTemplateAdditionalFields;
        }

        public Builder setEventTemplateAdditionalFields(
                final EventTemplateAdditionalFields eventTemplateAdditionalFields) {
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

        public String getMdcKeyPattern() {
            return mdcKeyPattern;
        }

        public Builder setMdcKeyPattern(final String mdcKeyPattern) {
            this.mdcKeyPattern = mdcKeyPattern;
            return this;
        }

        public String getNdcPattern() {
            return ndcPattern;
        }

        public Builder setNdcPattern(final String ndcPattern) {
            this.ndcPattern = ndcPattern;
            return this;
        }

        public String getEventDelimiter() {
            return eventDelimiter;
        }

        public Builder setEventDelimiter(final String eventDelimiter) {
            this.eventDelimiter = eventDelimiter;
            return this;
        }

        public int getMaxByteCount() {
            return maxByteCount;
        }

        public Builder setMaxByteCount(final int maxByteCount) {
            this.maxByteCount = maxByteCount;
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
            Objects.requireNonNull(eventTemplateAdditionalFields, "eventTemplateAdditionalFields");
            if (stackTraceEnabled &&
                    Strings.isBlank(stackTraceElementTemplate)
                    && Strings.isBlank(stackTraceElementTemplateUri)) {
                throw new IllegalArgumentException(
                        "both stackTraceElementTemplate and stackTraceElementTemplateUri are blank");
            }
            if (maxByteCount < JsonTemplateLayoutDefaults.MIN_BYTE_COUNT) {
                throw new IllegalArgumentException(
                        "was expecting maxByteCount >= " +
                                JsonTemplateLayoutDefaults.MIN_BYTE_COUNT +
                                ":" + maxByteCount);
            }
            Objects.requireNonNull(truncatedStringSuffix, "truncatedStringSuffix");
            Objects.requireNonNull(recyclerFactory, "recyclerFactory");
        }

    }

    // We need this ugly model and its builder just to be able to allow
    // key-value pairs in a dedicated element.
    @SuppressWarnings({"unused", "WeakerAccess"})
    @Plugin(name = "EventTemplateAdditionalFields", category = Node.CATEGORY, printObject = true)
    public static final class EventTemplateAdditionalFields {

        private static final EventTemplateAdditionalFields EMPTY = newBuilder().build();

        private final KeyValuePair[] additionalFields;

        private EventTemplateAdditionalFields(final Builder builder) {
            this.additionalFields = builder.additionalFields;
        }

        public KeyValuePair[] getAdditionalFields() {
            return additionalFields;
        }

        @PluginBuilderFactory
        public static Builder newBuilder() {
            return new Builder();
        }

        public static class Builder
                implements org.apache.logging.log4j.core.util.Builder<EventTemplateAdditionalFields> {

            @PluginElement("AdditionalField")
            private KeyValuePair[] additionalFields;

            private Builder() {}

            public KeyValuePair[] getAdditionalFields() {
                return additionalFields;
            }

            public Builder setAdditionalFields(final KeyValuePair[] additionalFields) {
                this.additionalFields = additionalFields;
                return this;
            }

            @Override
            public EventTemplateAdditionalFields build() {
                return new EventTemplateAdditionalFields(this);
            }

        }

    }

}
