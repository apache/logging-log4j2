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
package org.apache.logging.log4j.core.util.internal;

import java.util.HashMap;
import java.util.LinkedHashMap;

public final class Maps {
    private Maps() {}

    /**
     * Calculate initial capacity from expected size and default load factor (0.75).
     */
    private static int calculateCapacity(int numMappings) {
        return (int) Math.ceil(numMappings / 0.75d);
    }

    /**
     * Creates a new, empty HashMap suitable for the expected number of mappings.
     * The returned map is large enough so that the expected number of mappings can be
     * added without resizing the map.
     *
     * This is essentially a backport of HashMap.newHashMap which was added in JDK19.
     */
    public static <K, V> HashMap<K, V> newHashMap(int numMappings) {
        return new HashMap<>(calculateCapacity(numMappings));
    }

    /**
     * Creates a new, empty LinkedHashMap suitable for the expected number of mappings.
     * The returned map is large enough so that the expected number of mappings can be
     * added without resizing the map.
     *
     * This is essentially a backport of LinkedHashMap.newLinkedHashMap which was added in JDK19.
     */
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int numMappings) {
        return new LinkedHashMap<>(calculateCapacity(numMappings));
    }
}
