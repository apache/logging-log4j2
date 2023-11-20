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
package org.apache.logging.log4j.core.util;

/**
 * Boolean helpers.
 */
public final class Booleans {

    private Booleans() {}

    /**
     * Returns {@code true} if {@code s} is {@code "true"} (case-insensitive), {@code false} if {@code s} is
     * {@code "false"} (case-insensitive), and {@code defaultValue} if {@code s} is anything else (including null or
     * empty).
     *
     * @param s The {@code String} to parse into a {@code boolean}
     * @param defaultValue The default value to use if {@code s} is neither {@code "true"} nor {@code "false"}
     * @return the {@code boolean} value represented by the argument, or {@code defaultValue}.
     */
    public static boolean parseBoolean(final String s, final boolean defaultValue) {
        return "true".equalsIgnoreCase(s) || (defaultValue && !"false".equalsIgnoreCase(s));
    }
}
