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
package org.apache.logging.log4j.jackson.json.template.layout;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
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
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.jackson.json.template.layout.resolver.EventResolverContext;
import org.apache.logging.log4j.jackson.json.template.layout.resolver.StackTraceElementObjectResolverContext;
import org.apache.logging.log4j.jackson.json.template.layout.resolver.TemplateResolver;
import org.apache.logging.log4j.jackson.json.template.layout.resolver.TemplateResolvers;
import org.apache.logging.log4j.jackson.json.template.layout.util.ByteBufferOutputStream;
import org.apache.logging.log4j.jackson.json.template.layout.util.Uris;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

import java.io.IOException;
import java.lang.reflect.Method;
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

    private static final Logger LOGGER = StatusLogger.getLogger();

    private static final Map<String, String> CONTENT_FORMAT = Collections.singletonMap("version", "1");

    private final Charset charset;

    private final String contentType;

    private final byte[] emptyObjectJsonBytes;

    private final TemplateResolver<LogEvent> eventResolver;

    private final byte[] eventDelimiterBytes;

    private final Supplier<JsonTemplateLayoutSerializationContext> serializationContextSupplier;

    private final ThreadLocal<JsonTemplateLayoutSerializationContext> serializationContextRef;

    private JsonTemplateLayout(final Builder builder) {
        this.charset = builder.charset;
        this.contentType = "application/json; charset=" + charset;
        this.emptyObjectJsonBytes = "{}".getBytes(charset);
        this.eventDelimiterBytes = builder.eventDelimiter.getBytes(charset);
        final ObjectMapper objectMapper = createObjectMapper(builder.objectMapperFactoryMethod);
        final StrSubstitutor substitutor = builder.configuration.getStrSubstitutor();
        final TemplateResolver<StackTraceElement> stackTraceElementObjectResolver =
                builder.stackTraceEnabled
                        ? createStackTraceElementResolver(builder, objectMapper, substitutor)
                        : null;
        this.eventResolver = createEventResolver(builder, objectMapper, substitutor, stackTraceElementObjectResolver);
        this.serializationContextSupplier = createSerializationContextSupplier(builder, objectMapper);
        this.serializationContextRef = Constants.ENABLE_THREADLOCALS
                ? ThreadLocal.withInitial(serializationContextSupplier)
                : null;
    }

    private static ObjectMapper createObjectMapper(final String objectMapperFactoryMethod) {
        try {
            final int splitterIndex = objectMapperFactoryMethod.lastIndexOf('.');
            final String className = objectMapperFactoryMethod.substring(0, splitterIndex);
            final String methodName = objectMapperFactoryMethod.substring(splitterIndex + 1);
            final Class<?> clazz = Class.forName(className);
            if ("new".equals(methodName)) {
                return (ObjectMapper) clazz.getDeclaredConstructor().newInstance();
            } else {
                final Method method = clazz.getMethod(methodName);
                return (ObjectMapper) method.invoke(null);
            }
        } catch (final Exception error) {
            throw new RuntimeException(error);
        }
    }

    private static TemplateResolver<StackTraceElement> createStackTraceElementResolver(
            final Builder builder,
            final ObjectMapper objectMapper,
            final StrSubstitutor substitutor) {
        final StackTraceElementObjectResolverContext stackTraceElementObjectResolverContext =
                StackTraceElementObjectResolverContext
                        .newBuilder()
                        .setObjectMapper(objectMapper)
                        .setSubstitutor(substitutor)
                        .setBlankFieldExclusionEnabled(builder.blankFieldExclusionEnabled)
                        .build();
        final String stackTraceElementTemplate = readStackTraceElementTemplate(builder);
        return TemplateResolvers.ofTemplate(stackTraceElementObjectResolverContext, stackTraceElementTemplate);
    }

    private TemplateResolver<LogEvent> createEventResolver(
            final Builder builder,
            final ObjectMapper objectMapper,
            final StrSubstitutor substitutor,
            final TemplateResolver<StackTraceElement> stackTraceElementObjectResolver) {
        final String eventTemplate = readEventTemplate(builder);
        final int writerCapacity = builder.maxStringLength > 0
                ? builder.maxStringLength
                : builder.maxByteCount;
        final EventResolverContext resolverContext = EventResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setSubstitutor(substitutor)
                .setWriterCapacity(writerCapacity)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setStackTraceElementObjectResolver(stackTraceElementObjectResolver)
                .setBlankFieldExclusionEnabled(builder.blankFieldExclusionEnabled)
                .setMdcKeyPattern(builder.mdcKeyPattern)
                .setNdcPattern(builder.ndcPattern)
                .setEventTemplateAdditionalFields(builder.eventTemplateAdditionalFields.additionalFields)
                .setMapMessageFormatterIgnored(builder.mapMessageFormatterIgnored)
                .build();
        return TemplateResolvers.ofTemplate(resolverContext, eventTemplate);
    }

    private static Supplier<JsonTemplateLayoutSerializationContext>
    createSerializationContextSupplier(
            final Builder builder,
            final ObjectMapper objectMapper) {
        return JsonTemplateLayoutSerializationContexts.createSupplier(
                objectMapper,
                builder.maxByteCount,
                builder.prettyPrintEnabled,
                builder.blankFieldExclusionEnabled,
                builder.maxStringLength);
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

    @Override
    public String toSerializable(final LogEvent event) {
        final JsonTemplateLayoutSerializationContext context = getResetSerializationContext();
        try {
            encode(event, context);
            return context.getOutputStream().toString(charset);
        } catch (final Exception error) {
            reloadSerializationContext(error, context);
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    @Override
    public byte[] toByteArray(final LogEvent event) {
        final JsonTemplateLayoutSerializationContext context = getResetSerializationContext();
        try {
            encode(event, context);
            return context.getOutputStream().toByteArray();
        } catch (final Exception error) {
            reloadSerializationContext(error, context);
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    @Override
    public void encode(final LogEvent event, final ByteBufferDestination destination) {
        final JsonTemplateLayoutSerializationContext context = getResetSerializationContext();
        try {
            encode(event, context);
            final ByteBuffer byteBuffer = context.getOutputStream().getByteBuffer();
            byteBuffer.flip();
            // noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (destination) {
                ByteBufferDestinationHelper.writeToUnsynchronized(byteBuffer, destination);
            }
        } catch (final Exception error) {
            reloadSerializationContext(error, context);
            throw new RuntimeException("failed serializing JSON", error);
        }
    }

    // Visible for tests.
    JsonTemplateLayoutSerializationContext getSerializationContext() {
        return Constants.ENABLE_THREADLOCALS
                ? serializationContextRef.get()
                : serializationContextSupplier.get();
    }

    private JsonTemplateLayoutSerializationContext getResetSerializationContext() {
        JsonTemplateLayoutSerializationContext context;
        if (Constants.ENABLE_THREADLOCALS) {
            context = serializationContextRef.get();
            context.reset();
        } else {
            context = serializationContextSupplier.get();
        }
        return context;
    }

    private void reloadSerializationContext(
            final Exception cause,
            final JsonTemplateLayoutSerializationContext oldContext) {
        try {
            oldContext.close();
        } catch (final Exception error) {
            LOGGER.error("failed context close attempt suppressing the parent error", cause);
            throw new RuntimeException(error);
        }
        if (Constants.ENABLE_THREADLOCALS) {
            final JsonTemplateLayoutSerializationContext newContext =
                    serializationContextSupplier.get();
            serializationContextRef.set(newContext);
        }
    }

    private void encode(
            final LogEvent event,
            final JsonTemplateLayoutSerializationContext context)
            throws IOException {
        final JsonGenerator jsonGenerator = context.getJsonGenerator();
        eventResolver.resolve(event, jsonGenerator);
        jsonGenerator.flush();
        ByteBufferOutputStream outputStream = context.getOutputStream();
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
        private boolean prettyPrintEnabled =
                JsonTemplateLayoutDefaults.isPrettyPrintEnabled();

        @PluginBuilderAttribute
        private boolean locationInfoEnabled =
                JsonTemplateLayoutDefaults.isLocationInfoEnabled();

        @PluginBuilderAttribute
        private boolean stackTraceEnabled =
                JsonTemplateLayoutDefaults.isStackTraceEnabled();

        @PluginBuilderAttribute
        private boolean blankFieldExclusionEnabled =
                JsonTemplateLayoutDefaults.isBlankFieldExclusionEnabled();

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
        private String objectMapperFactoryMethod =
                JsonTemplateLayoutDefaults.getObjectMapperFactoryMethod();

        @PluginBuilderAttribute
        private boolean mapMessageFormatterIgnored =
                JsonTemplateLayoutDefaults.isMapMessageFormatterIgnored();

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

        public boolean isPrettyPrintEnabled() {
            return prettyPrintEnabled;
        }

        public Builder setPrettyPrintEnabled(final boolean prettyPrintEnabled) {
            this.prettyPrintEnabled = prettyPrintEnabled;
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

        public boolean isBlankFieldExclusionEnabled() {
            return blankFieldExclusionEnabled;
        }

        public Builder setBlankFieldExclusionEnabled(final boolean blankFieldExclusionEnabled) {
            this.blankFieldExclusionEnabled = blankFieldExclusionEnabled;
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

        public String getObjectMapperFactoryMethod() {
            return objectMapperFactoryMethod;
        }

        public Builder setObjectMapperFactoryMethod(
                final String objectMapperFactoryMethod) {
            this.objectMapperFactoryMethod = objectMapperFactoryMethod;
            return this;
        }

        public boolean isMapMessageFormatterIgnored() {
            return mapMessageFormatterIgnored;
        }

        public Builder setMapMessageFormatterIgnored(
                final boolean mapMessageFormatterIgnored) {
            this.mapMessageFormatterIgnored = mapMessageFormatterIgnored;
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
            if (maxByteCount <= 0) {
                throw new IllegalArgumentException(
                        "maxByteCount requires a non-zero positive integer");
            }
            if (maxStringLength < 0) {
                throw new IllegalArgumentException(
                        "maxStringLength requires a positive integer");
            }
            Objects.requireNonNull(objectMapperFactoryMethod, "objectMapperFactoryMethod");
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

            private Builder() {
                // Do nothing.
            }

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
