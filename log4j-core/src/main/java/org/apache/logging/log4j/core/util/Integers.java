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

import org.apache.logging.log4j.util.Strings;

/**
 * Helps deal with integers.
 */
public final class Integers {

    private static final int BITS_PER_INT = 32;

    private Integers() {}

    /**
     * Parses the string argument as a signed decimal integer.
     * <p>
     * The input may be surrounded by whitespace.
     * </p>
     *
     * @param s a {@code String} containing the {@code int} representation to parse, may be {@code null} or {@code ""}
     * @param defaultValue the return value, use {@code defaultValue} if {@code s} is {@code null} or {@code ""}
     * @return the integer value represented by the argument in decimal.
     * @throws NumberFormatException if the string does not contain a parsable integer.
     */
    public static int parseInt(final String s, final int defaultValue) {
        return Strings.isEmpty(s) ? defaultValue : Integer.parseInt(s.trim());
    }

    /**
     * Parses the string argument as a signed decimal integer.
     *
     * @param s a {@code String} containing the {@code int} representation to parse, may be {@code null} or {@code ""}
     * @return the integer value represented by the argument in decimal.
     * @throws NumberFormatException if the string does not contain a parsable integer.
     */
    public static int parseInt(final String s) {
        return parseInt(s, 0);
    }

    /**
     * Calculate the next power of 2, greater than or equal to x.
     * <p>
     * From Hacker's Delight, Chapter 3, Harry S. Warren Jr.
     *
     * @param x Value to round up
     * @return The next power of 2 from x inclusive
     */
    public static int ceilingNextPowerOfTwo(final int x) {
        return 1 << (BITS_PER_INT - Integer.numberOfLeadingZeros(x - 1));
    }
}
