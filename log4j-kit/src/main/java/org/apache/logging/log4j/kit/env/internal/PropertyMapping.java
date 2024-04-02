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
package org.apache.logging.log4j.kit.env.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A specialized map the converts 3.x properties into 2.x properties.
 * @since 3.0.0
 */
public class PropertyMapping {

    public static final PropertyMapping EMPTY = new PropertyMapping(Stream.empty());

    private final Map<String, List<String>> mapping;

    PropertyMapping(final Stream<Map.Entry<String, List<String>>> entries) {
        this.mapping = entries.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (c1, c2) -> {
            final List<String> result = new ArrayList<>(c1.size() + c2.size());
            result.addAll(c1);
            result.addAll(c2);
            return result;
        }));
    }

    /**
     * Converts between 3.x property keys and 2.x property keys
     * @param key A 3.x property key.
     * @return The corresponding 2.x property keys.
     */
    List<? extends String> getLegacyKeys(final String key) {
        return mapping.getOrDefault(key, Collections.emptyList());
    }

    /**
     * Merges two property mappings into a new one.
     * @param other Property mapping to merge.
     * @return A property mapping the combines {@code this} and {@code other}.
     */
    PropertyMapping merge(final PropertyMapping other) {
        return new PropertyMapping(Stream.concat(this.mapping.entrySet().stream(), other.mapping.entrySet().stream()));
    }
}
