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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactory;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public final class EventResolverContext implements TemplateResolverContext<LogEvent, EventResolverContext> {

    private final Configuration configuration;

    private final StrSubstitutor substitutor;

    private final Charset charset;

    private final JsonWriter jsonWriter;

    private final RecyclerFactory recyclerFactory;

    private final int maxStringByteCount;

    private final String truncatedStringSuffix;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final TemplateResolver<Throwable> stackTraceObjectResolver;

    private final String eventTemplateRootObjectKey;

    private final EventTemplateAdditionalField[] additionalFields;

    private EventResolverContext(final Builder builder) {
        this.configuration = builder.configuration;
        this.substitutor = builder.substitutor;
        this.charset = builder.charset;
        this.jsonWriter = builder.jsonWriter;
        this.recyclerFactory = builder.recyclerFactory;
        this.maxStringByteCount = builder.maxStringByteCount;
        this.truncatedStringSuffix = builder.truncatedStringSuffix;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
        this.stackTraceObjectResolver = stackTraceEnabled
                ? new StackTraceObjectResolver(builder.stackTraceElementObjectResolver)
                : null;
        this.eventTemplateRootObjectKey = builder.eventTemplateRootObjectKey;
        this.additionalFields = builder.eventTemplateAdditionalFields;
    }

    @Override
    public Class<EventResolverContext> getContextClass() {
        return EventResolverContext.class;
    }

    @Override
    public Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> getResolverFactoryByName() {
        return EventResolverFactories.getResolverFactoryByName();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public StrSubstitutor getSubstitutor() {
        return substitutor;
    }

    public Charset getCharset() {
        return charset;
    }

    @Override
    public JsonWriter getJsonWriter() {
        return jsonWriter;
    }

    RecyclerFactory getRecyclerFactory() {
        return recyclerFactory;
    }

    int getMaxStringByteCount() {
        return maxStringByteCount;
    }

    String getTruncatedStringSuffix() {
        return truncatedStringSuffix;
    }

    boolean isLocationInfoEnabled() {
        return locationInfoEnabled;
    }

    boolean isStackTraceEnabled() {
        return stackTraceEnabled;
    }

    TemplateResolver<Throwable> getStackTraceObjectResolver() {
        return stackTraceObjectResolver;
    }

    String getEventTemplateRootObjectKey() {
        return eventTemplateRootObjectKey;
    }

    EventTemplateAdditionalField[] getAdditionalFields() {
        return additionalFields;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Configuration configuration;

        private StrSubstitutor substitutor;

        private Charset charset;

        private JsonWriter jsonWriter;

        private RecyclerFactory recyclerFactory;

        private int maxStringByteCount;

        private String truncatedStringSuffix;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

        private TemplateResolver<StackTraceElement> stackTraceElementObjectResolver;

        private String eventTemplateRootObjectKey;

        private EventTemplateAdditionalField[] eventTemplateAdditionalFields;

        private Builder() {
            // Do nothing.
        }

        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setSubstitutor(final StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public Builder setCharset(final Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder setJsonWriter(final JsonWriter jsonWriter) {
            this.jsonWriter = jsonWriter;
            return this;
        }

        public Builder setRecyclerFactory(final RecyclerFactory recyclerFactory) {
            this.recyclerFactory = recyclerFactory;
            return this;
        }

        public Builder setMaxStringByteCount(final int maxStringByteCount) {
            this.maxStringByteCount = maxStringByteCount;
            return this;
        }

        public String getTruncatedStringSuffix() {
            return truncatedStringSuffix;
        }

        public Builder setTruncatedStringSuffix(final String truncatedStringSuffix) {
            this.truncatedStringSuffix = truncatedStringSuffix;
            return this;
        }

        public Builder setLocationInfoEnabled(final boolean locationInfoEnabled) {
            this.locationInfoEnabled = locationInfoEnabled;
            return this;
        }

        public Builder setStackTraceEnabled(final boolean stackTraceEnabled) {
            this.stackTraceEnabled = stackTraceEnabled;
            return this;
        }

        public Builder setStackTraceElementObjectResolver(
                final TemplateResolver<StackTraceElement> stackTraceElementObjectResolver) {
            this.stackTraceElementObjectResolver = stackTraceElementObjectResolver;
            return this;
        }

        public Builder setEventTemplateRootObjectKey(String eventTemplateRootObjectKey) {
            this.eventTemplateRootObjectKey = eventTemplateRootObjectKey;
            return this;
        }

        public Builder setEventTemplateAdditionalFields(
                final EventTemplateAdditionalField[] eventTemplateAdditionalFields) {
            this.eventTemplateAdditionalFields = eventTemplateAdditionalFields;
            return this;
        }

        public EventResolverContext build() {
            validate();
            return new EventResolverContext(this);
        }

        private void validate() {
            Objects.requireNonNull(configuration, "configuration");
            Objects.requireNonNull(substitutor, "substitutor");
            Objects.requireNonNull(charset, "charset");
            Objects.requireNonNull(jsonWriter, "jsonWriter");
            Objects.requireNonNull(recyclerFactory, "recyclerFactory");
            if (maxStringByteCount <= 0) {
                throw new IllegalArgumentException(
                        "was expecting maxStringByteCount > 0: " +
                                maxStringByteCount);
            }
            if (stackTraceEnabled) {
                Objects.requireNonNull(
                        stackTraceElementObjectResolver,
                        "stackTraceElementObjectResolver");
            }
        }

    }

}
