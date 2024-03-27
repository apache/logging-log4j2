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
package org.apache.logging.log4j.layout.template.json;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;
import org.apache.logging.log4j.core.layout.Encoder;
import org.apache.logging.log4j.core.layout.StringBuilderEncoder;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverContext;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverFactory;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverInterceptor;
import org.apache.logging.log4j.layout.template.json.resolver.EventResolverStringSubstitutor;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolver;
import org.apache.logging.log4j.layout.template.json.resolver.TemplateResolvers;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.Uris;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Factory;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.spi.recycler.Recycler;
import org.apache.logging.log4j.util.Strings;

@Configurable(elementType = Layout.ELEMENT_TYPE)
@Plugin
public class JsonTemplateLayout implements StringLayout {

    private static final Map<String, String> CONTENT_FORMAT = Collections.singletonMap("version", "1");

    private final Charset charset;

    private final String contentType;

    private final TemplateResolver<LogEvent> eventResolver;

    private final String eventDelimiter;

    private final Recycler<Context> contextRecycler;

    private static final class Context implements AutoCloseable {

        final JsonWriter jsonWriter;

        final Encoder<StringBuilder> encoder;

        private Context(final JsonWriter jsonWriter, final Encoder<StringBuilder> encoder) {
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
        final String eventDelimiterSuffix = builder.getNullEventDelimiterEnabled() ? "\0" : "";
        this.eventDelimiter = builder.eventDelimiter + eventDelimiterSuffix;
        final Configuration configuration = builder.configuration;
        final JsonWriter jsonWriter = JsonWriter.newBuilder()
                .setMaxStringLength(builder.maxStringLength)
                .setTruncatedStringSuffix(builder.truncatedStringSuffix)
                .build();
        this.eventResolver = createEventResolver(builder, configuration, charset, jsonWriter);
        this.contextRecycler = createContextRecycler(builder, jsonWriter);
    }

    private TemplateResolver<LogEvent> createEventResolver(
            final Builder builder,
            final Configuration configuration,
            final Charset charset,
            final JsonWriter jsonWriter) {

        // Inject resolver factory and interceptor plugins.
        final List<EventResolverFactory> resolverFactories =
                configuration.getComponent(new @Namespace(EventResolverFactory.CATEGORY) Key<>() {});
        final Map<String, EventResolverFactory> resolverFactoryByName = resolverFactories.stream()
                .collect(Collectors.toMap(
                        EventResolverFactory::getName,
                        Function.identity(),
                        (factory, conflictingFactory) -> {
                            final String message = String.format(
                                    "found resolver factories with overlapping names: %s (%s and %s)",
                                    factory.getName(), conflictingFactory, factory);
                            throw new IllegalArgumentException(message);
                        },
                        LinkedHashMap::new));
        final List<EventResolverInterceptor> resolverInterceptors =
                configuration.getComponent(new @Namespace(EventResolverInterceptor.CATEGORY) Key<>() {});
        final EventResolverStringSubstitutor substitutor =
                new EventResolverStringSubstitutor(configuration.getStrSubstitutor());

        // Read event and stack trace element templates.
        final String eventTemplate = readEventTemplate(builder);
        final String stackTraceElementTemplate = readStackTraceElementTemplate(builder);

        // Determine the max. string byte count.
        final float maxByteCountPerChar = builder.charset.newEncoder().maxBytesPerChar();
        final int maxStringByteCount =
                Math.toIntExact(Math.round(Math.ceil(maxByteCountPerChar * builder.maxStringLength)));

        // Replace null event template additional fields with an empty array.
        final EventTemplateAdditionalField[] eventTemplateAdditionalFields =
                builder.eventTemplateAdditionalFields != null
                        ? builder.eventTemplateAdditionalFields
                        : new EventTemplateAdditionalField[0];

        // Create the resolver context.
        final EventResolverContext resolverContext = EventResolverContext.newBuilder()
                .setConfiguration(configuration)
                .setResolverFactoryByName(resolverFactoryByName)
                .setResolverInterceptors(resolverInterceptors)
                .setSubstitutor(substitutor)
                .setCharset(charset)
                .setJsonWriter(jsonWriter)
                .setMaxStringByteCount(maxStringByteCount)
                .setTruncatedStringSuffix(builder.truncatedStringSuffix)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setStackTraceElementTemplate(stackTraceElementTemplate)
                .setEventTemplateRootObjectKey(builder.eventTemplateRootObjectKey)
                .setEventTemplateAdditionalFields(eventTemplateAdditionalFields)
                .build();

        // Compile the resolver template.
        return TemplateResolvers.ofTemplate(resolverContext, eventTemplate);
    }

    private static String readEventTemplate(final Builder builder) {
        return readTemplate(builder.eventTemplate, builder.eventTemplateUri, builder.charset);
    }

    private static String readStackTraceElementTemplate(final Builder builder) {
        return readTemplate(builder.stackTraceElementTemplate, builder.stackTraceElementTemplateUri, builder.charset);
    }

    private static String readTemplate(final String template, final String templateUri, final Charset charset) {
        return Strings.isBlank(template) ? Uris.readUri(templateUri, charset) : template;
    }

    private static Recycler<Context> createContextRecycler(final Builder builder, final JsonWriter jsonWriter) {
        final Supplier<Context> supplier = createContextSupplier(builder.charset, jsonWriter);
        return builder.configuration.getRecyclerFactory().create(supplier, Context::close);
    }

    private static Supplier<Context> createContextSupplier(final Charset charset, final JsonWriter jsonWriter) {
        return () -> {
            final JsonWriter clonedJsonWriter = jsonWriter.clone();
            final Encoder<StringBuilder> encoder = new StringBuilderEncoder(charset);
            return new Context(clonedJsonWriter, encoder);
        };
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        final String eventJson = toSerializable(event);
        if (eventJson != null) {
            return eventJson.getBytes(charset != null ? charset : Charset.defaultCharset());
        }
        return null;
    }

    @Override
    public String toSerializable(final LogEvent event) {

        // Acquire a context.
        final Recycler<Context> contextRecycler = this.contextRecycler;
        final Context context = contextRecycler.acquire();
        final JsonWriter jsonWriter = context.jsonWriter;
        final StringBuilder stringBuilder = jsonWriter.getStringBuilder();

        // Render the JSON.
        try {
            eventResolver.resolve(event, jsonWriter);
            stringBuilder.append(eventDelimiter);
            return stringBuilder.toString();
        }

        // Release the context.
        finally {
            contextRecycler.release(context);
        }
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {

        // Acquire a context.
        final Recycler<Context> contextRecycler = this.contextRecycler;
        final Context context = contextRecycler.acquire();
        final JsonWriter jsonWriter = context.jsonWriter;
        final StringBuilder stringBuilder = jsonWriter.getStringBuilder();
        final Encoder<StringBuilder> encoder = context.encoder;

        // Render & write the JSON.
        try {
            eventResolver.resolve(event, jsonWriter);
            stringBuilder.append(eventDelimiter);
            encoder.encode(stringBuilder, destination);
        }

        // Release the context.
        finally {
            contextRecycler.release(context);
        }
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

    @Factory
    @SuppressWarnings("WeakerAccess")
    public static Builder newBuilder() {
        return new Builder();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static final class Builder implements org.apache.logging.log4j.plugins.util.Builder<JsonTemplateLayout> {

        @PluginConfiguration
        private Configuration configuration;

        @PluginBuilderAttribute
        private Charset charset;

        @PluginBuilderAttribute
        private Boolean locationInfoEnabled;

        @PluginBuilderAttribute
        private Boolean stackTraceEnabled;

        @PluginBuilderAttribute
        private String eventTemplate;

        @PluginBuilderAttribute
        private String eventTemplateUri;

        @PluginBuilderAttribute
        private String eventTemplateRootObjectKey;

        @PluginElement("EventTemplateAdditionalField")
        private EventTemplateAdditionalField[] eventTemplateAdditionalFields;

        @PluginBuilderAttribute
        private String stackTraceElementTemplate;

        @PluginBuilderAttribute
        private String stackTraceElementTemplateUri;

        @PluginBuilderAttribute
        private String eventDelimiter;

        @PluginBuilderAttribute
        private Boolean nullEventDelimiterEnabled;

        @PluginBuilderAttribute
        private Integer maxStringLength;

        @PluginBuilderAttribute
        private String truncatedStringSuffix;

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

        public Boolean getLocationInfoEnabled() {
            return locationInfoEnabled;
        }

        public Builder setLocationInfoEnabled(final Boolean locationInfoEnabled) {
            this.locationInfoEnabled = locationInfoEnabled;
            return this;
        }

        public Boolean getStackTraceEnabled() {
            return stackTraceEnabled;
        }

        public Builder setStackTraceEnabled(final Boolean stackTraceEnabled) {
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

        public Builder setEventTemplateRootObjectKey(final String eventTemplateRootObjectKey) {
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

        public Builder setStackTraceElementTemplate(final String stackTraceElementTemplate) {
            this.stackTraceElementTemplate = stackTraceElementTemplate;
            return this;
        }

        public String getStackTraceElementTemplateUri() {
            return stackTraceElementTemplateUri;
        }

        public Builder setStackTraceElementTemplateUri(final String stackTraceElementTemplateUri) {
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

        public Boolean getNullEventDelimiterEnabled() {
            return nullEventDelimiterEnabled;
        }

        public Builder setNullEventDelimiterEnabled(final Boolean nullEventDelimiterEnabled) {
            this.nullEventDelimiterEnabled = nullEventDelimiterEnabled;
            return this;
        }

        public Integer getMaxStringLength() {
            return maxStringLength;
        }

        public Builder setMaxStringLength(final Integer maxStringLength) {
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

        @Override
        public JsonTemplateLayout build() {
            applyDefaults();
            validate();
            return new JsonTemplateLayout(this);
        }

        private void applyDefaults() {
            Objects.requireNonNull(configuration, "configuration");
            final JsonTemplateLayoutProperties props =
                    configuration.getEnvironment().getProperty(JsonTemplateLayoutProperties.class);
            if (charset == null) {
                charset = props.charset();
            }
            if (eventDelimiter == null) {
                eventDelimiter = props.eventDelimiter();
            }
            if (eventTemplate == null) {
                eventTemplate = props.eventTemplate();
            }
            if (eventTemplateRootObjectKey == null) {
                eventTemplateRootObjectKey = props.eventTemplateRootObjectKey();
            }
            if (eventTemplateUri == null) {
                eventTemplateUri = props.eventTemplateUri();
            }
            if (locationInfoEnabled == null) {
                locationInfoEnabled = props.locationInfoEnabled();
            }
            if (maxStringLength == null) {
                maxStringLength = props.maxStringLength();
            }
            if (nullEventDelimiterEnabled == null) {
                nullEventDelimiterEnabled = props.nullEventDelimiterEnabled();
            }
            if (stackTraceEnabled == null) {
                stackTraceEnabled = props.stackTraceEnabled();
            }
            if (stackTraceElementTemplate == null) {
                stackTraceElementTemplate = props.stackTraceElementTemplate();
            }
            if (stackTraceElementTemplateUri == null) {
                stackTraceElementTemplateUri = props.stackTraceElementTemplateUri();
            }
            if (truncatedStringSuffix == null) {
                truncatedStringSuffix = props.truncatedStringSuffix();
            }
        }

        private void validate() {
            Objects.requireNonNull(configuration, "configuration");
            if (Strings.isBlank(eventTemplate) && Strings.isBlank(eventTemplateUri)) {
                throw new IllegalArgumentException("both eventTemplate and eventTemplateUri are blank");
            }
            if (stackTraceEnabled
                    && Strings.isBlank(stackTraceElementTemplate)
                    && Strings.isBlank(stackTraceElementTemplateUri)) {
                throw new IllegalArgumentException(
                        "both stackTraceElementTemplate and stackTraceElementTemplateUri are blank");
            }
            if (maxStringLength <= 0) {
                throw new IllegalArgumentException(
                        "was expecting a non-zero positive maxStringLength: " + maxStringLength);
            }
            Objects.requireNonNull(truncatedStringSuffix, "truncatedStringSuffix");
        }
    }

    @Configurable(printObject = true)
    @Plugin("EventTemplateAdditionalField")
    public static final class EventTemplateAdditionalField {

        public enum Format {
            STRING,
            JSON
        }

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
        public boolean equals(final Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            final EventTemplateAdditionalField that = (EventTemplateAdditionalField) object;
            return key.equals(that.key) && value.equals(that.value) && format == that.format;
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value, format);
        }

        @Override
        public String toString() {
            final String formattedValue = Format.STRING.equals(format) ? String.format("\"%s\"", value) : value;
            return String.format("%s=%s", key, formattedValue);
        }

        @Factory
        public static EventTemplateAdditionalField.Builder newBuilder() {
            return new EventTemplateAdditionalField.Builder();
        }

        public static class Builder
                implements org.apache.logging.log4j.plugins.util.Builder<EventTemplateAdditionalField> {

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
