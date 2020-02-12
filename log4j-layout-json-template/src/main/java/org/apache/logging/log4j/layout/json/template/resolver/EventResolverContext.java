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
package org.apache.logging.log4j.layout.json.template.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.KeyValuePair;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class EventResolverContext implements TemplateResolverContext<LogEvent, EventResolverContext> {

    private final ObjectMapper objectMapper;

    private final StrSubstitutor substitutor;

    private final int writerCapacity;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final TemplateResolver<Throwable> stackTraceObjectResolver;

    private final boolean blankFieldExclusionEnabled;

    private final Pattern mdcKeyPattern;

    private final Pattern ndcPattern;

    private final KeyValuePair[] additionalFields;

    private final boolean mapMessageFormatterIgnored;

    private EventResolverContext(final Builder builder) {
        this.objectMapper = builder.objectMapper;
        this.substitutor = builder.substitutor;
        this.writerCapacity = builder.writerCapacity;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
        this.stackTraceObjectResolver = stackTraceEnabled
                ? new StackTraceObjectResolver(builder.stackTraceElementObjectResolver)
                : null;
        this.blankFieldExclusionEnabled = builder.blankFieldExclusionEnabled;
        this.mdcKeyPattern = builder.mdcKeyPattern == null ? null : Pattern.compile(builder.mdcKeyPattern);
        this.ndcPattern = builder.ndcPattern == null ? null : Pattern.compile(builder.ndcPattern);
        this.additionalFields = builder.eventTemplateAdditionalFields;
        this.mapMessageFormatterIgnored = builder.mapMessageFormatterIgnored;
    }

    @Override
    public Class<EventResolverContext> getContextClass() {
        return EventResolverContext.class;
    }

    @Override
    public Map<String, TemplateResolverFactory<LogEvent, EventResolverContext, ? extends TemplateResolver<LogEvent>>> getResolverFactoryByName() {
        return EventResolverFactories.getResolverFactoryByName();
    }

    @Override
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public StrSubstitutor getSubstitutor() {
        return substitutor;
    }

    int getWriterCapacity() {
        return writerCapacity;
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

    @Override
    public boolean isBlankFieldExclusionEnabled() {
        return blankFieldExclusionEnabled;
    }

    Pattern getMdcKeyPattern() {
        return mdcKeyPattern;
    }

    Pattern getNdcPattern() {
        return ndcPattern;
    }

    KeyValuePair[] getAdditionalFields() {
        return additionalFields;
    }

    boolean isMapMessageFormatterIgnored() {
        return mapMessageFormatterIgnored;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ObjectMapper objectMapper;

        private StrSubstitutor substitutor;

        private int writerCapacity;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

        private TemplateResolver<StackTraceElement> stackTraceElementObjectResolver;

        private boolean blankFieldExclusionEnabled;

        private String mdcKeyPattern;

        private String ndcPattern;

        private KeyValuePair[] eventTemplateAdditionalFields;

        private boolean mapMessageFormatterIgnored;

        private Builder() {
            // Do nothing.
        }

        public Builder setObjectMapper(final ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        public Builder setSubstitutor(final StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public Builder setWriterCapacity(final int writerCapacity) {
            this.writerCapacity = writerCapacity;
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

        public Builder setBlankFieldExclusionEnabled(
                final boolean blankFieldExclusionEnabled) {
            this.blankFieldExclusionEnabled = blankFieldExclusionEnabled;
            return this;
        }

        public Builder setMdcKeyPattern(final String mdcKeyPattern) {
            this.mdcKeyPattern = mdcKeyPattern;
            return this;
        }

        public Builder setNdcPattern(final String ndcPattern) {
            this.ndcPattern = ndcPattern;
            return this;
        }

        public Builder setEventTemplateAdditionalFields(
                final KeyValuePair[] eventTemplateAdditionalFields) {
            this.eventTemplateAdditionalFields = eventTemplateAdditionalFields;
            return this;
        }

        public Builder setMapMessageFormatterIgnored(
                final boolean mapMessageFormatterIgnored) {
            this.mapMessageFormatterIgnored = mapMessageFormatterIgnored;
            return this;
        }

        public EventResolverContext build() {
            validate();
            return new EventResolverContext(this);
        }

        private void validate() {
            Objects.requireNonNull(objectMapper, "objectMapper");
            Objects.requireNonNull(substitutor, "substitutor");
            if (writerCapacity <= 0) {
                throw new IllegalArgumentException(
                        "writerCapacity requires a non-zero positive integer");
            }
            if (stackTraceEnabled) {
                Objects.requireNonNull(stackTraceElementObjectResolver, "stackTraceElementObjectResolver");
            }
        }

    }

}
