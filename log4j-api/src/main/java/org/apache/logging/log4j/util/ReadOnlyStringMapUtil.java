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
package org.apache.logging.log4j.util;

import java.util.Map;

/**
 * Utility class for ReadOnlyStringMap implementations. Provides methods for equals and hashCode calculations.
 *
 * @since 2.25.0
 */
@InternalApi
public final class ReadOnlyStringMapUtil {

    private ReadOnlyStringMapUtil() {}

    /**
     * Compares two ReadOnlyStringMap instances for equality.
     * Two ReadOnlyStringMap instances are considered equal if they have the same size
     * and contain the same key-value pairs.
     *
     * @param map1 the first ReadOnlyStringMap to compare
     * @param map2 the second ReadOnlyStringMap to compare
     * @return true if the maps are equal, false otherwise
     */
    public static boolean equals(final ReadOnlyStringMap map1, final ReadOnlyStringMap map2) {
        if (map1 == map2) {
            return true;
        }
        if (map1 == null || map2 == null) {
            return false;
        }
        if (map1.size() != map2.size()) {
            return false;
        }

        // Convert to maps and compare
        final Map<String, String> thisMap = map1.toMap();
        final Map<String, String> otherMap = map2.toMap();
        return thisMap.equals(otherMap);
    }

    /**
     * Calculates the hash code for a ReadOnlyStringMap.
     * The hash code is calculated based on the key-value pairs in the map.
     *
     * @param map the ReadOnlyStringMap to calculate the hash code for
     * @return the hash code
     */
    public static int hashCode(final ReadOnlyStringMap map) {
        if (map == null) {
            return 0;
        }
        return map.toMap().hashCode();
    }
}
