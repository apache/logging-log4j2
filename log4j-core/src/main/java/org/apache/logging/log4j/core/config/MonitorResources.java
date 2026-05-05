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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Container for the {@code MonitorResources} element.
 */
@Plugin(name = "MonitorResources", category = Core.CATEGORY_NAME, printObject = true)
public final class MonitorResources {

    private final Set<MonitorResource> resources;

    private MonitorResources(final Set<MonitorResource> resources) {
        this.resources = requireNonNull(resources, "resources");
    }

    @PluginFactory
    public static MonitorResources createMonitorResources(
            @PluginElement("monitorResource") final MonitorResource[] resources) {
        requireNonNull(resources, "resources");
        final LinkedHashSet<MonitorResource> distinctResources =
                Arrays.stream(resources).collect(Collectors.toCollection(LinkedHashSet::new));
        return new MonitorResources(distinctResources);
    }

    public Set<MonitorResource> getResources() {
        return resources;
    }
}
