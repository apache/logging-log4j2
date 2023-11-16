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

import static org.apache.logging.log4j.util.Strings.toRootLowerCase;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;

/**
 * Converts values to lower case. The passed in "key" should be the value of another lookup.
 */
@Plugin(name = "lower", category = StrLookup.CATEGORY)
public class LowerLookup implements StrLookup {

    /**
     * Converts the "key" to lower case.
     * @param key  the key to be looked up, may be null
     * @return The value associated with the key.
     */
    @Override
    public String lookup(final String key) {
        return key != null ? toRootLowerCase(key) : null;
    }

    /**
     * Converts the "key" to lower case.
     * @param event The current LogEvent.
     * @param key  the key to be looked up, may be null
     * @return The value associated with the key.
     */
    @Override
    public String lookup(final LogEvent event, final String key) {
        return lookup(key);
    }
}
