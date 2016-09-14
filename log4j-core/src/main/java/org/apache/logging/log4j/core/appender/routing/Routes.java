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
package org.apache.logging.log4j.core.appender.routing;

import java.util.Objects;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Contains the individual Route elements.
 */
@Plugin(name = "Routes", category = "Core", printObject = true)
public final class Routes {

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<Routes>  {

        @PluginAttribute("pattern") 
        private String pattern;
        
        @PluginElement("Routes") 
        private Route[] routes;

        @Override
        public Routes build() {
            if (routes == null || routes.length == 0) {
                LOGGER.error("No routes configured");
                return null;
            }
            return new Routes(pattern, routes);
        }

        public String getPattern() {
            return pattern;
        }

        public Route[] getRoutes() {
            return routes;
        }

        public Builder withPattern(@SuppressWarnings("hiding") String pattern) {
            this.pattern = pattern;
            return this;
        }

        public Builder withRoutes(@SuppressWarnings("hiding") Route[] routes) {
            this.routes = routes;
            return this;
        }
        
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    private static final Logger LOGGER = StatusLogger.getLogger();

    private final String pattern;
    
    // TODO Why not make this a Map or add a Map.
    private final Route[] routes;

    private Routes(final String pattern, final Route... routes) {
        this.pattern = pattern;
        this.routes = routes;
    }

    /**
     * Returns the pattern.
     * @return the pattern.
     */
    public String getPattern() {
        return pattern;
    }

    public Route getRoute(String key) {
        for (int i = 0; i < routes.length; i++) {
            final Route route = routes[i];
            if (Objects.equals(route.getKey(), key)) {
                return route;
            }
        }
        return null;
    }

    /**
     * Returns the array of Route elements.
     * @return an array of Route elements.
     */
    public Route[] getRoutes() {
        return routes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (final Route route : routes) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(route.toString());
        }
        sb.append('}');
        return sb.toString();

    }

    /**
     * Creates the Routes.
     * @param pattern The pattern.
     * @param routes An array of Route elements.
     * @return The Routes container.
     * @deprecated since 2.7; use {@link #newBuilder()}.
     */
    @Deprecated
    public static Routes createRoutes(
            final String pattern,
            final Route... routes) {
        if (routes == null || routes.length == 0) {
            LOGGER.error("No routes configured");
            return null;
        }
        return new Routes(pattern, routes);
    }

}
