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
package org.apache.logging.log4j.hamcrest;

import java.util.Map;

import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * Hamcrest Matchers for Maps.
 *
 * @since 2.1
 */
public final class MapMatchers {

    /**
     * Returns a Map Matcher matching on the size of a Map.
     *
     * @param matcher the Matcher to match against the Map size.
     * @param <K>     the key type.
     * @param <V>     the value type.
     */
    public static <K, V> Matcher<Map<? extends K, ? extends V>> hasSize(final Matcher<Integer> matcher) {
        return new FeatureMatcher<Map<? extends K, ? extends V>, Integer>(matcher, "map with size", "map with size") {
            @Override
            protected Integer featureValueOf(final Map<? extends K, ? extends V> actual) {
                return actual.size();
            }
        };
    }

    /**
     * Returns a Map Matcher matching on the exact size of a Map.
     *
     * @param size the number of entries to match the Map against.
     * @param <K>  the key type.
     * @param <V>  the value type.
     */
    public static <K, V> Matcher<Map<? extends K, ? extends V>> hasSize(final int size) {
        return hasSize(equalTo(size));
    }

    private MapMatchers() {
    }
}
