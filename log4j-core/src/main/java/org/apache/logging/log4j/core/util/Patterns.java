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

import java.util.regex.Pattern;

/**
 * Pattern strings used throughout Log4j.
 * 
 * @see Pattern
 */
public final class Patterns {

    /**
     * A pattern string for comma separated lists with optional whitespace.
     */
    public static final String COMMA_SEPARATOR = toWhitespaceSeparator(",");

    /**
     * The whitespace pattern string.
     */
    public static final String WHITESPACE = "\\s*";

    private Patterns() {
    }

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
