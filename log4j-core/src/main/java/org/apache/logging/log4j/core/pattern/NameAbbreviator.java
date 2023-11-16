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
package org.apache.logging.log4j.core.pattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * NameAbbreviator generates abbreviated logger and class names.
 */
@PerformanceSensitive("allocation")
public abstract class NameAbbreviator {
    /**
     * Default (no abbreviation) abbreviator.
     */
    private static final NameAbbreviator DEFAULT = new NOPAbbreviator();

    /**
     * Gets an abbreviator.
     * <p>
     * For example, "%logger{2}" will output only 2 elements of the logger name, "%logger{1.}" will output only the
     * first character of the non-final elements in the name, "%logger(1~.2~} will output the first character of the
     * first element, two characters of the second and subsequent elements and will use a tilde to indicate abbreviated
     * characters.
     * </p>
     *
     * @param pattern
     *        abbreviation pattern.
     * @return abbreviator, will not be null.
     */
    public static NameAbbreviator getAbbreviator(final String pattern) {
        if (pattern.length() > 0) {
            //  if pattern is just spaces and numbers then
            //     use MaxElementAbbreviator
            final String trimmed = pattern.trim();

            if (trimmed.isEmpty()) {
                return DEFAULT;
            }

            final NameAbbreviator dwa = DynamicWordAbbreviator.create(trimmed);
            if (dwa != null) {
                return dwa;
            }

            boolean isNegativeNumber;
            final String number;

            // check if number is a negative number
            if (trimmed.length() > 1 && trimmed.charAt(0) == '-') {
                isNegativeNumber = true;
                number = trimmed.substring(1);
            } else {
                isNegativeNumber = false;
                number = trimmed;
            }

            int i = 0;

            while (i < number.length() && number.charAt(i) >= '0' && number.charAt(i) <= '9') {
                i++;
            }

            //
            //  if all blanks and digits
            //
            if (i == number.length()) {
                return new MaxElementAbbreviator(
                        Integers.parseInt(number),
                        isNegativeNumber ? MaxElementAbbreviator.Strategy.DROP : MaxElementAbbreviator.Strategy.RETAIN);
            }

            final List<PatternAbbreviatorFragment> fragments = new ArrayList<>(5);
            char ellipsis;
            int charCount;
            int pos = 0;

            while (pos < trimmed.length() && pos >= 0) {
                int ellipsisPos = pos;

                if (trimmed.charAt(pos) == '*') {
                    charCount = Integer.MAX_VALUE;
                    ellipsisPos++;
                } else if (trimmed.charAt(pos) >= '0' && trimmed.charAt(pos) <= '9') {
                    charCount = trimmed.charAt(pos) - '0';
                    ellipsisPos++;
                } else {
                    charCount = 0;
                }

                ellipsis = '\0';

                if (ellipsisPos < trimmed.length()) {
                    ellipsis = trimmed.charAt(ellipsisPos);

                    if (ellipsis == '.') {
                        ellipsis = '\0';
                    }
                }

                fragments.add(new PatternAbbreviatorFragment(charCount, ellipsis));
                pos = trimmed.indexOf('.', pos);

                if (pos == -1) {
                    break;
                }

                pos++;
            }

            return new PatternAbbreviator(fragments);
        }

        //
        //  no matching abbreviation, return defaultAbbreviator
        //
        return DEFAULT;
    }

    /**
     * Gets default abbreviator.
     *
     * @return default abbreviator.
     */
    public static NameAbbreviator getDefaultAbbreviator() {
        return DEFAULT;
    }

    /**
     * Abbreviates a name in a String.
     *
     * @param original the text to abbreviate, may not be null.
     * @param destination StringBuilder to write the result to
     */
    public abstract void abbreviate(final String original, final StringBuilder destination);

    /**
     * Abbreviator that simply appends full name to buffer.
     */
    private static class NOPAbbreviator extends NameAbbreviator {
        /**
         * Constructor.
         */
        public NOPAbbreviator() {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void abbreviate(final String original, final StringBuilder destination) {
            destination.append(original);
        }
    }

    /**
     * Abbreviator that drops starting path elements.
     */
    private static class MaxElementAbbreviator extends NameAbbreviator {

        /**
         * <p>When the name is reduced in length by cutting parts, there can be two ways to do it.</p>
         * 1. Remove a given number of parts starting from front - called DROP <br/>
         * 2. Retain a given number of parts starting from the end - called RETAIN
         */
        private enum Strategy {
            DROP(0) {
                @Override
                void abbreviate(final int count, final String original, final StringBuilder destination) {
                    // If a path does not contain enough path elements to drop, none will be dropped.
                    int start = 0;
                    int nextStart;
                    for (int i = 0; i < count; i++) {
                        nextStart = original.indexOf('.', start);
                        if (nextStart == -1) {
                            destination.append(original);
                            return;
                        }
                        start = nextStart + 1;
                    }
                    destination.append(original, start, original.length());
                }
            },
            RETAIN(1) {
                @Override
                void abbreviate(final int count, final String original, final StringBuilder destination) {
                    // We subtract 1 from 'len' when assigning to 'end' to avoid out of
                    // bounds exception in return r.substring(end+1, len). This can happen if
                    // precision is 1 and the category name ends with a dot.
                    int end = original.length() - 1;

                    for (int i = count; i > 0; i--) {
                        end = original.lastIndexOf('.', end - 1);
                        if (end == -1) {
                            destination.append(original);
                            return;
                        }
                    }
                    destination.append(original, end + 1, original.length());
                }
            };

            final int minCount;

            Strategy(final int minCount) {
                this.minCount = minCount;
            }

            abstract void abbreviate(final int count, final String original, final StringBuilder destination);
        }

        /**
         * Maximum number of path elements to output.
         */
        private final int count;

        /**
         * Strategy used for cutting down the size of the name
         */
        private final Strategy strategy;

        /**
         * Create new instance.
         *
         * @param count maximum number of path elements to drop or output.
         * @param strategy drop or retain
         */
        public MaxElementAbbreviator(final int count, final Strategy strategy) {
            this.count = Math.max(count, strategy.minCount);
            this.strategy = strategy;
        }

        /**
         * Abbreviate name.
         *
         * @param original The String to abbreviate.
         * @param destination the buffer to write the abbreviated name into
         */
        @Override
        public void abbreviate(final String original, final StringBuilder destination) {
            strategy.abbreviate(count, original, destination);
        }
    }

    /**
     * Fragment of an pattern abbreviator.
     */
    private static final class PatternAbbreviatorFragment {

        static final PatternAbbreviatorFragment[] EMPTY_ARRAY = {};

        /**
         * Count of initial characters of element to output.
         */
        private final int charCount;

        /**
         * Character used to represent dropped characters.
         * '\0' indicates no representation of dropped characters.
         */
        private final char ellipsis;

        /**
         * Creates a PatternAbbreviatorFragment.
         *
         * @param charCount number of initial characters to preserve.
         * @param ellipsis  character to represent elimination of characters,
         *                  '\0' if no ellipsis is desired.
         */
        PatternAbbreviatorFragment(final int charCount, final char ellipsis) {
            this.charCount = charCount;
            this.ellipsis = ellipsis;
        }

        /**
         * Abbreviate element of name.
         *
         * @param input      input string which is being written to the output {@code buf}.
         * @param inputIndex starting index of name element in the {@code input} string.
         * @param buf        buffer to receive element.
         * @return starting  index of next element.
         */
        int abbreviate(final String input, final int inputIndex, final StringBuilder buf) {
            // Note that indexOf(char) performs worse than indexOf(String) on pre-16 JREs
            // due to missing intrinsics for the character implementation. The difference
            // is a few nanoseconds in most cases, so we opt to give the jre as much
            // information as possible for best performance on new runtimes, with the
            // possibility that such optimizations may be back-ported.
            // See https://bugs.openjdk.java.net/browse/JDK-8173585
            final int nextDot = input.indexOf('.', inputIndex);
            if (nextDot < 0) {
                buf.append(input, inputIndex, input.length());
                return nextDot;
            }
            if (nextDot - inputIndex > charCount) {
                buf.append(input, inputIndex, inputIndex + charCount);
                if (ellipsis != '\0') {
                    buf.append(ellipsis);
                }
                buf.append('.');
            } else {
                // Include the period to reduce interactions with the buffer
                buf.append(input, inputIndex, nextDot + 1);
            }
            return nextDot + 1;
        }

        @Override
        public String toString() {
            return String.format(
                    "%s[charCount=%s, ellipsis=%s]",
                    getClass().getSimpleName(), charCount, Integer.toHexString(ellipsis));
        }
    }

    /**
     * Pattern abbreviator.
     */
    private static final class PatternAbbreviator extends NameAbbreviator {
        /**
         * Element abbreviation patterns.
         */
        private final PatternAbbreviatorFragment[] fragments;

        /**
         * Create PatternAbbreviator.
         *
         * @param fragments element abbreviation patterns.
         */
        PatternAbbreviator(final List<PatternAbbreviatorFragment> fragments) {
            if (fragments.isEmpty()) {
                throw new IllegalArgumentException("fragments must have at least one element");
            }

            this.fragments = fragments.toArray(PatternAbbreviatorFragment.EMPTY_ARRAY);
        }

        /**
         * Abbreviates name.
         *
         * @param original the original string to abbreviate
         * @param destination buffer that abbreviated name is appended to
         */
        @Override
        public void abbreviate(final String original, final StringBuilder destination) {
            // non-terminal patterns are executed once
            int originalIndex = 0;
            int iteration = 0;
            final int originalLength = original.length();
            while (originalIndex >= 0 && originalIndex < originalLength) {
                originalIndex = fragment(iteration++).abbreviate(original, originalIndex, destination);
            }
        }

        PatternAbbreviatorFragment fragment(final int index) {
            return fragments[Math.min(index, fragments.length - 1)];
        }

        @Override
        public String toString() {
            return String.format("%s[fragments=%s]", getClass().getSimpleName(), Arrays.toString(fragments));
        }
    }
}
