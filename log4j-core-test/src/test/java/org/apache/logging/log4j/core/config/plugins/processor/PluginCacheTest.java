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

package org.apache.logging.log4j.core.config.plugins.processor;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class PluginCacheTest {

    @Test
    public void testOutputIsReproducibleWhenInputOrderingChanges() throws IOException {
        PluginCache cacheA = new PluginCache();
        createCategory(cacheA, "one", Arrays.asList("bravo", "alpha", "charlie"));
        createCategory(cacheA, "two", Arrays.asList("alpha", "charlie", "bravo"));
        assertEquals(cacheA.getAllCategories().size(), 2);
        assertEquals(cacheA.getAllCategories().get("one").size(), 3);
        assertEquals(cacheA.getAllCategories().get("two").size(), 3);
        PluginCache cacheB = new PluginCache();
        createCategory(cacheB, "two", Arrays.asList("bravo", "alpha", "charlie"));
        createCategory(cacheB, "one", Arrays.asList("alpha", "charlie", "bravo"));
        assertEquals(cacheB.getAllCategories().size(), 2);
        assertEquals(cacheB.getAllCategories().get("one").size(), 3);
        assertEquals(cacheB.getAllCategories().get("two").size(), 3);
        assertArrayEquals(cacheData(cacheA), cacheData(cacheB));
    }

    private void createCategory(PluginCache cache, String categoryName, List<String> entryNames) {
        Map<String, PluginEntry> category = cache.getCategory(categoryName);
        for (String entryName: entryNames) {
            PluginEntry entry = new PluginEntry();
            entry.setKey(entryName);
            entry.setClassName("com.example.Plugin");
            entry.setName("name");
            entry.setCategory(categoryName);
            category.put(entryName, entry);
        }
    }

    private byte[] cacheData(PluginCache cache) throws IOException {
        ByteArrayOutputStream outputB = new ByteArrayOutputStream();
        cache.writeCache(outputB);
        return outputB.toByteArray();
    }

}
