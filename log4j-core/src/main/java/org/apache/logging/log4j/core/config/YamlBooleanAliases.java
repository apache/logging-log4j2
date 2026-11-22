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
package org.apache.logging.log4j.core.config;

import java.util.Locale;
import org.apache.logging.log4j.core.config.yaml.Jackson3YamlConfiguration;

/**
 * Utilities for parsing YAML 1.1 boolean aliases in Jackson 3 configurations.
 */
public final class YamlBooleanAliases {

    private YamlBooleanAliases() {}

    /**
     * Normalizes an alias only for Jackson 3 YAML configurations and boolean attributes.
     *
     * @param configuration the configuration being parsed
     * @param conversionType the target attribute type
     * @param value the scalar value
     * @return the normalized or unchanged value
     */
    public static String normalizeBooleanAttribute(
            final Configuration configuration, final Class<?> conversionType, final String value) {
        if (configuration instanceof Jackson3YamlConfiguration
                && (conversionType == Boolean.TYPE || conversionType == Boolean.class)) {
            return normalize(value);
        }
        return value;
    }

    /**
     * Converts a YAML 1.1 boolean alias to its canonical value.
     *
     * @param value the scalar value
     * @return the canonical boolean value, or the unchanged value if it is not an alias
     */
    public static String normalize(final String value) {
        if (value == null) {
            return null;
        }
        switch (value.toLowerCase(Locale.ROOT)) {
            case "y":
            case "yes":
            case "on":
                return "true";
            case "n":
            case "no":
            case "off":
                return "false";
            default:
                return value;
        }
    }
}
