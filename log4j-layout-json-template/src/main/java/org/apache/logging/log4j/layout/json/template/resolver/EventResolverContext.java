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

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.layout.json.template.util.RecyclerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class EventResolverContext implements TemplateResolverContext<LogEvent, EventResolverContext> {

    private final StrSubstitutor substitutor;

    private final RecyclerFactory recyclerFactory;

    private final int maxByteCount;

    private final boolean locationInfoEnabled;

    private final boolean stackTraceEnabled;

    private final TemplateResolver<Throwable> stackTraceObjectResolver;

    private final Pattern mdcKeyPattern;

    private final Pattern ndcPattern;

    private final KeyValuePair[] additionalFields;

    private EventResolverContext(final Builder builder) {
        this.substitutor = builder.substitutor;
        this.recyclerFactory = builder.recyclerFactory;
        this.maxByteCount = builder.maxByteCount;
        this.locationInfoEnabled = builder.locationInfoEnabled;
        this.stackTraceEnabled = builder.stackTraceEnabled;
        this.stackTraceObjectResolver = stackTraceEnabled
                ? new StackTraceObjectResolver(builder.stackTraceElementObjectResolver)
                : null;
        this.mdcKeyPattern = builder.mdcKeyPattern == null ? null : Pattern.compile(builder.mdcKeyPattern);
        this.ndcPattern = builder.ndcPattern == null ? null : Pattern.compile(builder.ndcPattern);
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

    @Override
    public StrSubstitutor getSubstitutor() {
        return substitutor;
    }

    RecyclerFactory getRecyclerFactory() {
        return recyclerFactory;
    }

    int getMaxByteCount() {
        return maxByteCount;
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

    Pattern getMdcKeyPattern() {
        return mdcKeyPattern;
    }

    Pattern getNdcPattern() {
        return ndcPattern;
    }

    KeyValuePair[] getAdditionalFields() {
        return additionalFields;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private StrSubstitutor substitutor;

        private RecyclerFactory recyclerFactory;

        private int maxByteCount;

        private boolean locationInfoEnabled;

        private boolean stackTraceEnabled;

        private TemplateResolver<StackTraceElement> stackTraceElementObjectResolver;

        private String mdcKeyPattern;

        private String ndcPattern;

        private KeyValuePair[] eventTemplateAdditionalFields;

        private Builder() {
            // Do nothing.
        }

        public Builder setSubstitutor(final StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public Builder setRecyclerFactory(final RecyclerFactory recyclerFactory) {
            this.recyclerFactory = recyclerFactory;
            return this;
        }

        public Builder setMaxByteCount(final int maxByteCount) {
            this.maxByteCount = maxByteCount;
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

        public EventResolverContext build() {
            validate();
            return new EventResolverContext(this);
        }

        private void validate() {
            Objects.requireNonNull(substitutor, "substitutor");
            Objects.requireNonNull(recyclerFactory, "recyclerFactory");
            if (maxByteCount <= 0) {
                throw new IllegalArgumentException(
                        "was expecting maxByteCount > 0: " +
                                maxByteCount);
            }
            if (stackTraceEnabled) {
                Objects.requireNonNull(
                        stackTraceElementObjectResolver,
                        "stackTraceElementObjectResolver");
            }
        }

    }

}
