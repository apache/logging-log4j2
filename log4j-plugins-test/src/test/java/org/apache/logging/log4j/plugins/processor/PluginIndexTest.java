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
package org.apache.logging.log4j.plugins.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.List;
import org.apache.logging.log4j.plugins.model.PluginEntry;
import org.apache.logging.log4j.plugins.model.PluginIndex;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

public class PluginIndexTest {

    @Test
    @Issue("https://issues.apache.org/jira/browse/LOG4J2-2735")
    public void testOutputIsReproducibleWhenInputOrderingChanges() {
        final PluginIndex indexA = new PluginIndex();
        createNamespace(indexA, "one", List.of("bravo", "alpha", "charlie"));
        createNamespace(indexA, "two", List.of("alpha", "charlie", "bravo"));
        assertEquals(6, indexA.size());
        final PluginIndex indexB = new PluginIndex();
        createNamespace(indexB, "two", List.of("bravo", "alpha", "charlie"));
        createNamespace(indexB, "one", List.of("alpha", "charlie", "bravo"));
        assertEquals(6, indexB.size());
        assertIterableEquals(indexA, indexB);
    }

    private void createNamespace(final PluginIndex index, final String namespace, final List<String> entryNames) {
        for (String entryName : entryNames) {
            final PluginEntry entry = PluginEntry.builder()
                    .setKey(entryName)
                    .setName(entryName)
                    .setClassName("com.example.Plugin")
                    .setNamespace(namespace)
                    .get();
            index.add(entry);
        }
    }
}
