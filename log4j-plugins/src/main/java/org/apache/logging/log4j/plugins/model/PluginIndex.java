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
package org.apache.logging.log4j.plugins.model;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PluginIndex extends AbstractCollection<PluginEntry> {
    private final Map<String, Map<String, PluginEntry>> index = new TreeMap<>();

    @Override
    public void forEach(Consumer<? super PluginEntry> action) {
        for (var namespace : index.values()) {
            for (var pluginEntry : namespace.values()) {
                action.accept(pluginEntry);
            }
        }
    }

    @Override
    public Iterator<PluginEntry> iterator() {
        return index.values().stream()
                .map(Map::values)
                .flatMap(Collection::stream)
                .iterator();
    }

    @Override
    public int size() {
        return index.values().stream().mapToInt(Map::size).sum();
    }

    @Override
    public boolean add(PluginEntry entry) {
        return getOrCreateNamespace(entry.namespace()).putIfAbsent(entry.key(), entry) == null;
    }

    @Override
    public boolean isEmpty() {
        return index.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return o instanceof PluginEntry entry
                && index.containsKey(entry.namespace())
                && index.get(entry.namespace()).containsKey(entry.key())
                && index.get(entry.namespace()).get(entry.key()).equals(entry);
    }

    @Override
    public void clear() {
        index.clear();
    }

    private Map<String, PluginEntry> getOrCreateNamespace(String namespace) {
        return index.computeIfAbsent(namespace, ignored -> new TreeMap<>());
    }
}
