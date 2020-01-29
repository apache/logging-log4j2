/*
 * Copyright 2017-2020 Volkan Yazıcı
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permits and
 * limitations under the License.
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
import org.apache.logging.log4j.core.time.internal.format.FastDateFormat;
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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
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
                return (ObjectMapper) clazz.newInstance();
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
                        .setBlankPropertyExclusionEnabled(builder.blankPropertyExclusionEnabled)
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
        final Locale locale = readLocale(builder.locale);
        final FastDateFormat timestampFormat =
                FastDateFormat.getInstance(
                        builder.dateTimeFormatPattern, builder.timeZone, locale);
        final EventResolverContext resolverContext = EventResolverContext
                .newBuilder()
                .setObjectMapper(objectMapper)
                .setSubstitutor(substitutor)
                .setWriterCapacity(writerCapacity)
                .setTimeZone(builder.timeZone)
                .setLocale(locale)
                .setTimestampFormat(timestampFormat)
                .setLocationInfoEnabled(builder.locationInfoEnabled)
                .setStackTraceEnabled(builder.stackTraceEnabled)
                .setStackTraceElementObjectResolver(stackTraceElementObjectResolver)
                .setBlankPropertyExclusionEnabled(builder.blankPropertyExclusionEnabled)
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
                builder.blankPropertyExclusionEnabled,
                builder.maxStringLength);
    }

    private static String readEventTemplate(final Builder builder) {
        return readTemplate(builder.eventTemplate, builder.eventTemplateUri);
    }

    private static String readStackTraceElementTemplate(final Builder builder) {
        return readTemplate(builder.stackTraceElementTemplate, builder.stackTraceElementTemplateUri);
    }

    private static String readTemplate(final String template, final String templateUri) {
        return Strings.isBlank(template)
                ? Uris.readUri(templateUri)
                : template;
    }

    private static Locale readLocale(final String locale) {
        if (locale == null) {
            return Locale.getDefault();
        }
        final String[] localeFields = locale.split("_", 3);
        switch (localeFields.length) {
            case 1: return new Locale(localeFields[0]);
            case 2: return new Locale(localeFields[0], localeFields[1]);
            case 3: return new Locale(localeFields[0], localeFields[1], localeFields[2]);
        }
        throw new IllegalArgumentException("invalid locale: " + locale);
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
        private Charset charset = StandardCharsets.UTF_8;

        @PluginBuilderAttribute("prettyPrint")
        private boolean prettyPrintEnabled = false;

        @PluginBuilderAttribute("locationInfo")
        private boolean locationInfoEnabled = false;

        @PluginBuilderAttribute("stackTrace")
        private boolean stackTraceEnabled = true;

        @PluginBuilderAttribute("blankPropertyExclusion")
        private boolean blankPropertyExclusionEnabled = false;

        @PluginBuilderAttribute
        private String dateTimeFormatPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZ";

        @PluginBuilderAttribute
        private TimeZone timeZone = TimeZone.getDefault();

        @PluginBuilderAttribute
        private String locale = null;

        @PluginBuilderAttribute
        private String eventTemplate = null;

        @PluginBuilderAttribute
        private String eventTemplateUri =
                "classpath:LogstashJsonEventLayoutV1.json";

        @PluginElement("EventTemplateAdditionalFields")
        private EventTemplateAdditionalFields eventTemplateAdditionalFields
                = EventTemplateAdditionalFields.EMPTY;

        @PluginBuilderAttribute
        private String stackTraceElementTemplate = null;

        @PluginBuilderAttribute
        private String stackTraceElementTemplateUri =
                "classpath:Log4j2StackTraceElementLayout.json";

        @PluginBuilderAttribute
        private String mdcKeyPattern;

        @PluginBuilderAttribute
        private String ndcPattern;

        @PluginBuilderAttribute
        private String eventDelimiter = System.lineSeparator();

        @PluginBuilderAttribute
        private int maxByteCount = 1024 * 16;  // 16 KiB

        @PluginBuilderAttribute
        private int maxStringLength = 0;

        @PluginBuilderAttribute
        private String objectMapperFactoryMethod =
                "com.fasterxml.jackson.databind.ObjectMapper.new";

        @PluginBuilderAttribute
        private boolean mapMessageFormatterIgnored = true;

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

        public boolean isBlankPropertyExclusionEnabled() {
            return blankPropertyExclusionEnabled;
        }

        public Builder setBlankPropertyExclusionEnabled(final boolean blankPropertyExclusionEnabled) {
            this.blankPropertyExclusionEnabled = blankPropertyExclusionEnabled;
            return this;
        }

        public String getDateTimeFormatPattern() {
            return dateTimeFormatPattern;
        }

        public Builder setDateTimeFormatPattern(final String dateTimeFormatPattern) {
            this.dateTimeFormatPattern = dateTimeFormatPattern;
            return this;
        }

        public TimeZone getTimeZone() {
            return timeZone;
        }

        public Builder setTimeZone(final TimeZone timeZone) {
            this.timeZone = timeZone;
            return this;
        }

        public String getLocale() {
            return locale;
        }

        public Builder setLocale(final String locale) {
            this.locale = locale;
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
            if (Strings.isBlank(dateTimeFormatPattern)) {
                throw new IllegalArgumentException("dateTimeFormatPattern");
            }
            Objects.requireNonNull(timeZone, "timeZone");
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
