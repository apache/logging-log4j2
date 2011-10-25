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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginNode;
import org.apache.logging.log4j.status.StatusLogger;

/**
 *
 */
@Plugin(name="Routes", type="Core", printObject=true)
public class Routes {

    private final String pattern;
    private final Route[] routes;
    private static final Logger logger = StatusLogger.getLogger();

    private Routes(String pattern, Route[] routes) {
        this.pattern = pattern;
        this.routes = routes;
    }

    public String getPattern() {
        return pattern;
    }

    public Route[] getRoutes() {
        return routes;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Route route : routes) {
            if ((!first)) {
                sb.append(",");
            }
            first = false;
            sb.append(route.toString());
        }
        sb.append("}");
        return sb.toString();

    }

    @PluginFactory
    public static Routes createRoutes(@PluginAttr("pattern") String pattern,
                                      @PluginElement("routes") Route[] routes) {
        if (pattern == null) {
            logger.error("A pattern is required");
            return null;
        }
        if (routes == null || routes.length == 0) {
            logger.error("No routes configured");
            return null;
        }
        return new Routes(pattern, routes);
    }
}
