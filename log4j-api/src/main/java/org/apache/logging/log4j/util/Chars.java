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
package org.apache.logging.log4j.util;

/**
 * <em>Consider this class private.</em>
 */
@InternalApi
public final class Chars {

    /** Carriage Return. */
    public static final char CR = '\r';

    /** Double Quote. */
    public static final char DQUOTE = '\"';

    /** Equals '='. */
    public static final char EQ = '=';

    /** Line Feed. */
    public static final char LF = '\n';

    /** NUL. */
    public static final char NUL = 0;

    /** Single Quote [']. */
    public static final char QUOTE = '\'';

    /** Space. */
    public static final char SPACE = ' ';

    /** Tab. */
    public static final char TAB = '\t';

    /**
     * Converts a digit into an upper-case hexadecimal character or the null character if invalid.
     *
     * @param digit a number 0 - 15
     * @return the hex character for that digit or '\0' if invalid
     */
    public static char getUpperCaseHex(final int digit) {
        if (digit < 0 || digit >= 16) {
            return '\0';
        }
        return digit < 10 ? getNumericalDigit(digit) : getUpperCaseAlphaDigit(digit);
    }

    /**
     * Converts a digit into an lower-case hexadecimal character or the null character if invalid.
     *
     * @param digit a number 0 - 15
     * @return the hex character for that digit or '\0' if invalid
     */
    public static char getLowerCaseHex(final int digit) {
        if (digit < 0 || digit >= 16) {
            return '\0';
        }
        return digit < 10 ? getNumericalDigit(digit) : getLowerCaseAlphaDigit(digit);
    }

    private static char getNumericalDigit(final int digit) {
        return (char) ('0' + digit);
    }

    private static char getUpperCaseAlphaDigit(final int digit) {
        return (char) ('A' + digit - 10);
    }

    private static char getLowerCaseAlphaDigit(final int digit) {
        return (char) ('a' + digit - 10);
    }

    private Chars() {}
}
