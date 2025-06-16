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
package org.apache.logging.log4j.core.config.plugins.processor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PluginCacheTest {

    @Test
    void testOutputIsReproducibleWhenInputOrderingChanges() throws IOException {
        final PluginCache cacheA = new PluginCache();
        createCategory(cacheA, "one", Arrays.asList("bravo", "alpha", "charlie"));
        createCategory(cacheA, "two", Arrays.asList("alpha", "charlie", "bravo"));
        assertEquals(2, cacheA.getAllCategories().size());
        assertEquals(3, cacheA.getAllCategories().get("one").size());
        assertEquals(3, cacheA.getAllCategories().get("two").size());
        final PluginCache cacheB = new PluginCache();
        createCategory(cacheB, "two", Arrays.asList("bravo", "alpha", "charlie"));
        createCategory(cacheB, "one", Arrays.asList("alpha", "charlie", "bravo"));
        assertEquals(2, cacheB.getAllCategories().size());
        assertEquals(3, cacheB.getAllCategories().get("one").size());
        assertEquals(3, cacheB.getAllCategories().get("two").size());
        assertArrayEquals(cacheData(cacheA), cacheData(cacheB));
    }

    private void createCategory(final PluginCache cache, final String categoryName, final List<String> entryNames) {
        final Map<String, PluginEntry> category = cache.getCategory(categoryName);
        for (String entryName : entryNames) {
            final PluginEntry entry = new PluginEntry();
            entry.setKey(entryName);
            entry.setClassName("com.example.Plugin");
            entry.setName("name");
            entry.setCategory(categoryName);
            category.put(entryName, entry);
        }
    }

    private byte[] cacheData(final PluginCache cache) throws IOException {
        final ByteArrayOutputStream outputB = new ByteArrayOutputStream();
        cache.writeCache(outputB);
        return outputB.toByteArray();
    }
}
