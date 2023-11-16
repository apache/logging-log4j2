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

import java.util.Map;
import java.util.Properties;

/**
 * {@link RuntimeStrSubstitutor} is a {@link StrSubstitutor} which only supports recursive evaluation of lookups.
 * This can be dangerous when combined with user-provided inputs, and should only be used on data directly from
 * a configuration.
 */
public final class ConfigurationStrSubstitutor extends StrSubstitutor {

    public ConfigurationStrSubstitutor() {}

    public ConfigurationStrSubstitutor(final Map<String, String> valueMap) {
        super(valueMap);
    }

    public ConfigurationStrSubstitutor(final Properties properties) {
        super(properties);
    }

    public ConfigurationStrSubstitutor(final StrLookup lookup) {
        super(lookup);
    }

    public ConfigurationStrSubstitutor(final StrSubstitutor other) {
        super(other);
    }

    @Override
    public String toString() {
        return "ConfigurationStrSubstitutor{" + super.toString() + "}";
    }
}
