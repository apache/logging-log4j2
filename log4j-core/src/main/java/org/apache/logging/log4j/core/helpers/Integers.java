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
package org.apache.logging.log4j.core.helpers;

/**
 * Helps deal with integers.
 */
public class Integers {

    /**
     * Parses the string argument as a signed decimal integer.
     *
     * @param s
     *            a {@code String} containing the {@code int} representation to parse, may be {@code null} or {@code ""}
     * @param defaultValue
     *            the return value, use {@code defaultValue} if {@code s} is {@code null} or {@code ""}
     * @return the integer value represented by the argument in decimal.
     * @throws NumberFormatException
     *             if the string does not contain a parsable integer.
     */
    public static int parseInt(String s, int defaultValue) {
        return Strings.isEmpty(s) ? defaultValue : Integer.parseInt(s);
    }

    /**
     * Parses the string argument as a signed decimal integer.
     *
     * @param s
     *            a {@code String} containing the {@code int} representation to parse, may be {@code null} or {@code ""}
     * @return the integer value represented by the argument in decimal.
     * @throws NumberFormatException
     *             if the string does not contain a parsable integer.
     */
    public static int parseInt(String s) {
        return parseInt(s, 0);
    }
}
