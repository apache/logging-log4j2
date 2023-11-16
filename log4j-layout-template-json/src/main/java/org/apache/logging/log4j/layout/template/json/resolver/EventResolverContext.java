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
package org.apache.logging.log4j.layout.template.json.resolver;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.EventTemplateAdditionalField;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;
import org.apache.logging.log4j.layout.template.json.util.RecyclerFactory;
import org.apache.logging.log4j.util.Strings;

/**
 * {@link TemplateResolverContext} specialized for {@link LogEvent}s.
 *
 * @see EventResolver
 * @see EventResolverFactory
 */
public final class EventResolverContext implements TemplateResolverContext<LogEvent, EventResolverContext> {

    private final Configuration configuration;

    private final Map<String, EventResolverFactory> resolverFactoryByName;

    private final List<EventResolverInterceptor> resolverInterceptors;

    private final EventResolverStringSubstitutor substitutor;

    private final Charset charset;

    private final JsonWriter jsonWriter;

    private final RecyclerFactory recyclerFactory;

    private final int maxStringByteCount;

    private final String truncatedStringSuffix;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final String stackTraceElementTemplate;

    private final String eventTemplateRootObjectKey;

    private final EventTemplateAdditionalField[] eventTemplateAdditionalFields;

    private EventResolverContext(final Builder builder) {
        this.configuration = builder.configuration;
        this.resolverFactoryByName = builder.resolverFactoryByName;
        this.resolverInterceptors = builder.resolverInterceptors;
        this.substitutor = builder.substitutor;
        this.charset = builder.charset;
        this.jsonWriter = builder.jsonWriter;
        this.recyclerFactory = builder.recyclerFactory;
        this.maxStringByteCount = builder.maxStringByteCount;
        this.truncatedStringSuffix = builder.truncatedStringSuffix;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
        this.stackTraceElementTemplate = builder.stackTraceElementTemplate;
        this.eventTemplateRootObjectKey = builder.eventTemplateRootObjectKey;
        this.eventTemplateAdditionalFields = builder.eventTemplateAdditionalFields;
    }

    @Override
    public final Class<EventResolverContext> getContextClass() {
        return EventResolverContext.class;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public Map<String, EventResolverFactory> getResolverFactoryByName() {
        return resolverFactoryByName;
    }

    @Override
    public List<EventResolverInterceptor> getResolverInterceptors() {
        return resolverInterceptors;
    }

    @Override
    public EventResolverStringSubstitutor getSubstitutor() {
        return substitutor;
    }

    public Charset getCharset() {
        return charset;
    }

    @Override
    public JsonWriter getJsonWriter() {
        return jsonWriter;
    }

    public RecyclerFactory getRecyclerFactory() {
        return recyclerFactory;
    }

    public int getMaxStringByteCount() {
        return maxStringByteCount;
    }

    public String getTruncatedStringSuffix() {
        return truncatedStringSuffix;
    }

    public boolean isLocationInfoEnabled() {
        return locationInfoEnabled;
    }

    public boolean isStackTraceEnabled() {
        return stackTraceEnabled;
    }

    public String getStackTraceElementTemplate() {
        return stackTraceElementTemplate;
    }

    public String getEventTemplateRootObjectKey() {
        return eventTemplateRootObjectKey;
    }

    public EventTemplateAdditionalField[] getEventTemplateAdditionalFields() {
        return eventTemplateAdditionalFields;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Configuration configuration;

        private Map<String, EventResolverFactory> resolverFactoryByName;

        private List<EventResolverInterceptor> resolverInterceptors;

        private EventResolverStringSubstitutor substitutor;

        private Charset charset;

        private JsonWriter jsonWriter;

        private RecyclerFactory recyclerFactory;

        private int maxStringByteCount;

        private String truncatedStringSuffix;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

        private String stackTraceElementTemplate;

        private String eventTemplateRootObjectKey;

        private EventTemplateAdditionalField[] eventTemplateAdditionalFields;

        private Builder() {
            // Do nothing.
        }

        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setResolverFactoryByName(final Map<String, EventResolverFactory> resolverFactoryByName) {
            this.resolverFactoryByName = resolverFactoryByName;
            return this;
        }

        public Builder setResolverInterceptors(final List<EventResolverInterceptor> resolverInterceptors) {
            this.resolverInterceptors = resolverInterceptors;
            return this;
        }

        public Builder setSubstitutor(final EventResolverStringSubstitutor substitutor) {
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

        public Builder setStackTraceElementTemplate(final String stackTraceElementTemplate) {
            this.stackTraceElementTemplate = stackTraceElementTemplate;
            return this;
        }

        public Builder setEventTemplateRootObjectKey(final String eventTemplateRootObjectKey) {
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
            Objects.requireNonNull(resolverFactoryByName, "resolverFactoryByName");
            if (resolverFactoryByName.isEmpty()) {
                throw new IllegalArgumentException("empty resolverFactoryByName");
            }
            Objects.requireNonNull(resolverInterceptors, "resolverInterceptors");
            Objects.requireNonNull(substitutor, "substitutor");
            Objects.requireNonNull(charset, "charset");
            Objects.requireNonNull(jsonWriter, "jsonWriter");
            Objects.requireNonNull(recyclerFactory, "recyclerFactory");
            if (maxStringByteCount <= 0) {
                throw new IllegalArgumentException("was expecting maxStringByteCount > 0: " + maxStringByteCount);
            }
            Objects.requireNonNull(truncatedStringSuffix, "truncatedStringSuffix");
            if (stackTraceEnabled && Strings.isBlank(stackTraceElementTemplate)) {
                throw new IllegalArgumentException(
                        "stackTraceElementTemplate cannot be blank when stackTraceEnabled is set to true");
            }
            Objects.requireNonNull(stackTraceElementTemplate, "stackTraceElementTemplate");
            Objects.requireNonNull(eventTemplateAdditionalFields, "eventTemplateAdditionalFields");
        }
    }
}
