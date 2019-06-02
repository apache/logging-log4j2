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
package org.apache.logging.log4j.plugins.processor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class Description goes here.
 */
public abstract class PluginService {

    private final Map<String, Map<String, PluginEntry>> categories = new LinkedHashMap<>();

    public PluginService() {
        PluginEntry[] entries = getEntries();
        for (PluginEntry entry : entries) {
            String category = entry.getCategory().toLowerCase();
            if (!categories.containsKey(category)) {
                categories.put(category, new LinkedHashMap<>());
            }
            Map<String, PluginEntry> map = categories.get(category);
            map.put(entry.getKey(), entry);
        }
    }

    public abstract PluginEntry[] getEntries();

    public Map<String, Map<String, PluginEntry>> getCategories() {
        return Collections.unmodifiableMap(categories);
    }

    public Map<String, PluginEntry> getCategory(String category) {
        return Collections.unmodifiableMap(categories.get(category.toLowerCase()));
    }

    public long size() {
        return categories.size();
    }

}
