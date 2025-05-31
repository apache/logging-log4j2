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
package org.apache.logging.log4j.core.config;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;

/**
 * Container for the {@code MonitorResource} element.
 */
@Plugin(name = "MonitorResource", category = Core.CATEGORY_NAME, printObject = true)
public final class MonitorResource {

    private final URI uri;

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builds MonitorResource instances.
     */
    public static final class Builder implements org.apache.logging.log4j.core.util.Builder<MonitorResource> {

        @PluginBuilderAttribute
        @Required(message = "No URI provided")
        private URI uri;

        public Builder setUri(final URI uri) {
            this.uri = uri;
            return this;
        }

        @Override
        public MonitorResource build() {
            return new MonitorResource(uri);
        }
    }

    private MonitorResource(final URI uri) {
        this.uri = requireNonNull(uri, "uri");
        if (!"file".equals(uri.getScheme())) {
            final String message =
                    String.format("Only `file` scheme is supported in monitor resource URIs! Illegal URI: `%s`", uri);
            throw new IllegalArgumentException(message);
        }
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MonitorResource)) {
            return false;
        }
        final MonitorResource other = (MonitorResource) object;
        return this.uri == other.uri;
    }

    @Override
    public String toString() {
        return String.format("MonitorResource{%s}", uri);
    }
}
