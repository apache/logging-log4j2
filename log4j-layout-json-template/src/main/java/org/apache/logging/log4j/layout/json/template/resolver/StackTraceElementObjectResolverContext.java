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

import org.apache.logging.log4j.core.lookup.StrSubstitutor;

import java.util.Map;
import java.util.Objects;

public final class StackTraceElementObjectResolverContext
        implements TemplateResolverContext<StackTraceElement, StackTraceElementObjectResolverContext> {

    private final StrSubstitutor substitutor;

    private StackTraceElementObjectResolverContext(final Builder builder) {
        this.substitutor = builder.substitutor;
    }

    @Override
    public Class<StackTraceElementObjectResolverContext> getContextClass() {
        return StackTraceElementObjectResolverContext.class;
    }

    @Override
    public Map<String, TemplateResolverFactory<StackTraceElement, StackTraceElementObjectResolverContext, ? extends TemplateResolver<StackTraceElement>>> getResolverFactoryByName() {
        return StackTraceElementObjectResolverFactories.getResolverFactoryByName();
    }

    @Override
    public StrSubstitutor getSubstitutor() {
        return substitutor;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private StrSubstitutor substitutor;

        private Builder() {
            // Do nothing.
        }

        public Builder setSubstitutor(final StrSubstitutor substitutor) {
            this.substitutor = substitutor;
            return this;
        }

        public StackTraceElementObjectResolverContext build() {
            validate();
            return new StackTraceElementObjectResolverContext(this);
        }

        private void validate() {
            Objects.requireNonNull(substitutor, "substitutor");
        }

    }

}
