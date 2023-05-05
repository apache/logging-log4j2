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
package org.apache.logging.log4j.core.appender.routing;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginNode;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * A Route to an appender.
 */
@Plugin(name = "Route", category = Core.CATEGORY_NAME, printObject = true, deferChildren = true)
public final class Route {
    private static final Logger LOGGER = StatusLogger.getLogger();

    private final Node node;
    private final String appenderRef;
    private final String key;

    private Route(final Node node, final String appenderRef, final String key) {
        this.node = node;
        this.appenderRef = appenderRef;
        this.key = key;
    }

    /**
     * Returns the Dynamic Appender Node.
     * @return The Node.
     */
    public Node getNode() {
        return node;
    }

    /**
     * Returns the appender reference.
     * @return The Appender reference.
     */
    public String getAppenderRef() {
        return appenderRef;
    }

    /**
     * Returns the key for this Route.
     * @return the key for this Route.
     */
    public String getKey() {
        return key;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Route(");
        sb.append("type=");
        if (appenderRef != null) {
            sb.append("static Reference=").append(appenderRef);
        } else if (node != null) {
            sb.append("dynamic - type=").append(node.getName());
        } else {
            sb.append("invalid Route");
        }
        if (key != null) {
            sb.append(" key='").append(key).append('\'');
        } else {
            sb.append(" default");
        }
        sb.append(')');
        return sb.toString();
    }

    /**
     * Create the Route.
     * @param appenderRef The Appender reference.
     * @param key The key.
     * @param node The Node.
     * @return A Route.
     */
    @PluginFactory
    public static Route createRoute(
            @PluginAttribute("ref") final String appenderRef,
            @PluginAttribute("key") final String key,
            @PluginNode final Node node) {
        if (node != null && node.hasChildren()) {
            if (appenderRef != null) {
                LOGGER.error("A route cannot be configured with an appender reference and an appender definition");
                return null;
            }
        } else if (appenderRef == null) {
            LOGGER.error("A route must specify an appender reference or an appender definition");
            return null;
        }
        return new Route(node, appenderRef, key);
    }
}
