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

/**
 * A default lookup for others to extend.
 *
 * @since 2.1
 */
public abstract class AbstractLookup implements StrLookup {

    /**
     * Calls {@code lookup(null, key)} in the implementation.
     *
     * @see StrLookup#lookup(LogEvent, String)
     */
    @Override
    public String lookup(final String key) {
        return lookup(null, key);
    }

    /**
     * Calls {@code evaluate(null, key)} in the implementation.
     *
     * @see StrLookup#evaluate(LogEvent, String)
     */
    @Override
    public LookupResult evaluate(final String key) {
        return evaluate(null, key);
    }
}
