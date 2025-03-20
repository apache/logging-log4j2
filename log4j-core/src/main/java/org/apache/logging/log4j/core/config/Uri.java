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

import java.util.Objects;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginValue;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Descriptor of a URI object that is created via configuration.
 */
@Plugin(name = "Uri", category = Core.CATEGORY_NAME, printObject = true)
public final class Uri {

    /**
     * The empty array.
     */
    static final Uri[] EMPTY_ARRAY = {};

    private final String uri;

    private Uri(final String uri) {
        this.uri = Objects.requireNonNull(uri, "uri is null");
    }

    /**
     * Creates a Uri object.
     *
     * @param uri the URI.
     * @return A Uri object.
     */
    @PluginFactory
    public static Uri createUri( // @formatter:off
            @PluginValue("uri") final String uri) {
        // @formatter:on

        StatusLogger.getLogger().debug("Creating Uri('{}')", uri);
        return new Uri(uri);
    }

    /**
     * Returns the URI.
     *
     * @return the URI
     */
    public String getUri() {
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
        if (!(object instanceof Uri)) {
            return false;
        }
        final Uri other = (Uri) object;
        return this.uri == other.uri;
    }

    @Override
    public String toString() {
        return "Uri[" + uri + "]";
    }
}
