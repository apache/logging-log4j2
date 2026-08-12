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
package org.apache.logging.log4j.trustgate.rules;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

final class AttackPatternFixtures {

    private static final String FIXTURE = "trustgate-attack-patterns.txt";

    private AttackPatternFixtures() {}

    static Stream<Arguments> patternsFor(final String category) {
        return patternsByCategory().getOrDefault(category, Collections.emptyList()).stream()
                .map(Arguments::of);
    }

    static Map<String, List<String>> patternsByCategory() {
        final Map<String, List<String>> patterns = new LinkedHashMap<>();
        String currentCategory = null;
        try (InputStream input = AttackPatternFixtures.class.getClassLoader().getResourceAsStream(FIXTURE)) {
            if (input == null) {
                throw new IllegalStateException("Missing fixture: " + FIXTURE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    if (line.startsWith("category=")) {
                        currentCategory = line.substring("category=".length());
                        patterns.computeIfAbsent(currentCategory, ignored -> new ArrayList<>());
                        continue;
                    }
                    if (currentCategory != null) {
                        patterns.get(currentCategory).add(line);
                    }
                }
            }
        } catch (final IOException ex) {
            throw new IllegalStateException("Unable to read fixture: " + FIXTURE, ex);
        }
        return patterns;
    }
}
