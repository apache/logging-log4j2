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
package org.apache.logging.log4j.core.util;

import java.util.HashSet;
import java.util.Set;

/**
 * Set-related convenience methods.
 */
public final class SetUtils {
    private SetUtils() {
    }

    /**
     * Extracts the Strings from a Set that start with a given prefix.
     *
     * @param set a Set of Strings (assumed to all be non-{@code null})
     * @param prefix the prefix to look for in the string set
     * @return an array of the matching strings from the given set
     */
    public static String[] prefixSet(final Set<String> set, final String prefix) {
        final Set<String> prefixSet = new HashSet<>();
        for (final String str : set) {
            if (str.startsWith(prefix)) {
                prefixSet.add(str);
            }
        }
        return prefixSet.toArray(new String[prefixSet.size()]);
    }
}
