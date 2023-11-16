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
package org.apache.logging.log4j.core.lookup;

import org.apache.logging.log4j.core.LogEvent;

/**
 * Lookup a String key to a String value.
 * <p>
 * This class represents the simplest form of a string to string map.
 * It has a benefit over a map in that it can create the result on
 * demand based on the key.
 * </p>
 * <p>
 * This class comes complete with various factory methods.
 * If these do not suffice, you can subclass and implement your own matcher.
 * </p>
 * <p>
 * For example, it would be possible to implement a lookup that used the
 * key as a primary key, and looked up the value on demand from the database
 * </p>
 */
public interface StrLookup {

    /**
     * Main plugin category for StrLookup plugins.
     *
     * @since 2.1
     */
    String CATEGORY = "Lookup";

    /**
     * Looks up a String key to a String value.
     * <p>
     * The internal implementation may use any mechanism to return the value.
     * The simplest implementation is to use a Map. However, virtually any
     * implementation is possible.
     * </p>
     * <p>
     * For example, it would be possible to implement a lookup that used the
     * key as a primary key, and looked up the value on demand from the database
     * Or, a numeric based implementation could be created that treats the key
     * as an integer, increments the value and return the result as a string -
     * converting 1 to 2, 15 to 16 etc.
     * </p>
     * <p>
     * This method always returns a String, regardless of
     * the underlying data, by converting it as necessary. For example:
     * </p>
     * <pre>
     * Map&lt;String, Object&gt; map = new HashMap&lt;String, Object&gt;();
     * map.put("number", new Integer(2));
     * assertEquals("2", StrLookup.mapLookup(map).lookup("number"));
     * </pre>
     * @param key  the key to be looked up, may be null
     * @return the matching value, null if no match
     */
    String lookup(String key);

    /**
     * Looks up a String key to a String value possibly using the current LogEvent.
     * <p>
     * The internal implementation may use any mechanism to return the value.
     * The simplest implementation is to use a Map. However, virtually any
     * implementation is possible.
     * </p>
     * <p>
     * For example, it would be possible to implement a lookup that used the
     * key as a primary key, and looked up the value on demand from the database
     * Or, a numeric based implementation could be created that treats the key
     * as an integer, increments the value and return the result as a string -
     * converting 1 to 2, 15 to 16 etc.
     * </p>
     * <p>
     * This method always returns a String, regardless of
     * the underlying data, by converting it as necessary. For example:
     * </p>
     * <pre>
     * Map&lt;String, Object&gt; map = new HashMap&lt;String, Object&gt;();
     * map.put("number", new Integer(2));
     * assertEquals("2", StrLookup.mapLookup(map).lookup("number"));
     * </pre>
     * @param event The current LogEvent.
     * @param key  the key to be looked up, may be null
     * @return the matching value, null if no match
     */
    String lookup(LogEvent event, String key);

    /**
     * Same as {@link #lookup(String)}, but provides additional metadata describing the result.
     * Returns null if the key cannot be evaluated, otherwise a {@link LookupResult} wrapping the non-null string value.
     */
    default LookupResult evaluate(String key) {
        final String value = lookup(key);
        return value == null ? null : new DefaultLookupResult(value);
    }

    /**
     * Same as {@link #lookup(LogEvent, String)}, but provides additional metadata describing the result.
     * Returns null if the key cannot be evaluated, otherwise a {@link LookupResult} wrapping the non-null string value.
     */
    default LookupResult evaluate(LogEvent event, String key) {
        final String value = lookup(event, key);
        return value == null ? null : new DefaultLookupResult(value);
    }
}
