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
package org.apache.logging.log4j.layout.template.json.resolver;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.layout.template.json.util.JsonWriter;

/**
 * An index-based resolver for the <tt>main()</tt> method arguments.
 *
 * <h3>Configuration</h3>
 *
 * <pre>
 * config = index | key
 * index  = "index" -> number
 * key    = "key" -> string
 * </pre>
 *
 * <h3>Examples</h3>
 *
 * Resolve the 1st <tt>main()</tt> method argument:
 *
 * <pre>
 * {
 *   "$resolver": "main",
 *   "index": 0
 * }
 * </pre>
 *
 * Resolve the argument coming right after <tt>--userId</tt>:
 *
 * <pre>
 * {
 *   "$resolver": "main",
 *   "key": "--userId"
 * }
 * </pre>
 *
 * @see MainMapResolver
 */
public final class MainMapResolver implements EventResolver {

    private static final MainMapLookup MAIN_MAP_LOOKUP = new MainMapLookup();

    private final String key;

    static String getName() {
        return "main";
    }

    MainMapResolver(final TemplateResolverConfig config) {
        final String key = config.getString("key");
        final Integer index = config.getInteger("index");
        if (key != null && index != null) {
            throw new IllegalArgumentException(
                    "provided both key and index: " + config);
        }
        if (key == null && index == null) {
            throw new IllegalArgumentException(
                    "either key or index must be provided: " + config);
        }
        this.key = index != null
                ? String.valueOf(index)
                : key;
    }

    @Override
    public void resolve(
            final LogEvent logEvent,
            final JsonWriter jsonWriter) {
        final String value = MAIN_MAP_LOOKUP.lookup(key);
        jsonWriter.writeString(value);
    }

}
