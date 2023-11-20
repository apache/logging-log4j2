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
 * Pattern strings used throughout Log4j.
 *
 * @see java.util.regex.Pattern
 */
public final class Patterns {

    /**
     * A pattern string for comma separated lists with optional whitespace.
     */
    public static final String COMMA_SEPARATOR = toWhitespaceSeparator(",");

    /**
     * A pattern string for lists separated by commas with optional whitespace or just whitespace.
     */
    public static final String COMMA_SPACE_SEPARATOR = toWhitespaceSeparator("[,\\s]");

    /**
     * The whitespace pattern string.
     */
    public static final String WHITESPACE = "\\s*";

    private Patterns() {}

    /**
     * Creates a pattern string for {@code separator} surrounded by whitespace.
     *
     * @param separator The separator.
     * @return a pattern for {@code separator} surrounded by whitespace.
     */
    public static String toWhitespaceSeparator(final String separator) {
        return WHITESPACE + separator + WHITESPACE;
    }
}
