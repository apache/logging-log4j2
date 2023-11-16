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

public interface LookupResult {

    /** Value of the lookup result. Never null. */
    String value();

    /**
     * True if the {@link #value()} should be re-evaluated for other lookups.
     * This is used by {@link PropertiesLookup} to allow properties to be evaluated against other properties,
     * because the configuration properties are completely trusted and designed with lookups in mind. It is
     * unsafe to return true in most cases because it may allow unintended lookups to evaluate other lookups.
     */
    default boolean isLookupEvaluationAllowedInValue() {
        return false;
    }
}
