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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

/**
 * Container for MonitorUri objects.
 */
@Plugin(name = "MonitorUris", category = Core.CATEGORY_NAME, printObject = true)
public final class MonitorUris {

    private final List<Uri> uris;

    private MonitorUris(final Uri[] uris) {
        this.uris = new ArrayList<>(Arrays.asList(uris));
    }

    /**
     * Create a MonitorUris object to contain all the URIs to be monitored.
     *
     * @param uris An array of URIs.
     * @return A MonitorUris object.
     */
    @PluginFactory
    public static MonitorUris createMonitorUris( //
            @PluginElement("Uris") final Uri[] uris) {
        return new MonitorUris(uris == null ? Uri.EMPTY_ARRAY : uris);
    }

    /**
     * Returns a list of the {@code Uri} objects created during configuration.
     * @return the URIs to be monitored
     */
    public List<Uri> getUris() {
        return uris;
    }
}
