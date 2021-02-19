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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        assertThat(cacheA.getAllCategories()).hasSize(2);
        assertThat(cacheA.getAllCategories().get("one")).hasSize(3);
        assertThat(cacheA.getAllCategories().get("two")).hasSize(3);
        PluginCache cacheB = new PluginCache();
        createCategory(cacheB, "two", Arrays.asList("bravo", "alpha", "charlie"));
        createCategory(cacheB, "one", Arrays.asList("alpha", "charlie", "bravo"));
        assertThat(cacheB.getAllCategories()).hasSize(2);
        assertThat(cacheB.getAllCategories().get("one")).hasSize(3);
        assertThat(cacheB.getAllCategories().get("two")).hasSize(3);
        assertThat(Objects.toString(cacheB.getAllCategories())).isEqualTo(Objects.toString(cacheA.getAllCategories()));
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

}
