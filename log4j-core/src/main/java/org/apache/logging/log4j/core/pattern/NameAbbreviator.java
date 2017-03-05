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
package org.apache.logging.log4j.core.pattern;

import java.util.ArrayList;
import java.util.List;

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

            while (i < number.length() && number.charAt(i) >= '0'
                    && number.charAt(i) <= '9') {
                i++;
            }

            //
            //  if all blanks and digits
            //
            if (i == number.length()) {
                return new MaxElementAbbreviator(Integer.parseInt(number),
                        isNegativeNumber? MaxElementAbbreviator.Strategy.DROP : MaxElementAbbreviator.Strategy.RETAIN);
            }

            final ArrayList<PatternAbbreviatorFragment> fragments = new ArrayList<>(5);
            char ellipsis;
            int charCount;
            int pos = 0;

            while (pos < trimmed.length() && pos >= 0) {
                int ellipsisPos = pos;

                if (trimmed.charAt(pos) == '*') {
                    charCount = Integer.MAX_VALUE;
                    ellipsisPos++;
                } else {
                    if (trimmed.charAt(pos) >= '0' && trimmed.charAt(pos) <= '9') {
                        charCount = trimmed.charAt(pos) - '0';
                        ellipsisPos++;
                    } else {
                        charCount = 0;
                    }
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
        public NOPAbbreviator() {
        }

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
    private static class PatternAbbreviatorFragment {
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
        public PatternAbbreviatorFragment(
            final int charCount, final char ellipsis) {
            this.charCount = charCount;
            this.ellipsis = ellipsis;
        }

        /**
         * Abbreviate element of name.
         *
         * @param buf      buffer to receive element.
         * @param startPos starting index of name element.
         * @return starting index of next element.
         */
        public int abbreviate(final StringBuilder buf, final int startPos) {
            final int start = (startPos < 0) ? 0 : startPos;
            final int max = buf.length();
            int nextDot = -1;
            for (int i = start; i < max; i++) {
                if (buf.charAt(i) == '.') {
                    nextDot = i;
                    break;
                }
            }
            if (nextDot != -1) {
                if (nextDot - startPos > charCount) {
                    buf.delete(startPos + charCount, nextDot);
                    nextDot = startPos + charCount;

                    if (ellipsis != '\0') {
                        buf.insert(nextDot, ellipsis);
                        nextDot++;
                    }
                }
                nextDot++;
            }
            return nextDot;
        }
    }

    /**
     * Pattern abbreviator.
     */
    private static class PatternAbbreviator extends NameAbbreviator {
        /**
         * Element abbreviation patterns.
         */
        private final PatternAbbreviatorFragment[] fragments;

        /**
         * Create PatternAbbreviator.
         *
         * @param fragments element abbreviation patterns.
         */
        public PatternAbbreviator(final List<PatternAbbreviatorFragment> fragments) {
            if (fragments.isEmpty()) {
                throw new IllegalArgumentException(
                    "fragments must have at least one element");
            }

            this.fragments = new PatternAbbreviatorFragment[fragments.size()];
            fragments.toArray(this.fragments);
        }

        /**
         * Abbreviates name.
         *
         * @param original the original string to abbreviate
         * @param destination buffer that abbreviated name is appended to
         */
        @Override
        public void abbreviate(final String original, final StringBuilder destination) {
            //
            //  all non-terminal patterns are executed once
            //
            int pos = destination.length();
            final int max = pos + original.length();
            final StringBuilder sb = destination.append(original);//new StringBuilder(original);

            for (int i = 0; i < fragments.length - 1 && pos < original.length(); i++) {
                pos = fragments[i].abbreviate(sb, pos);
            }

            //
            //   last pattern in executed repeatedly
            //
            final PatternAbbreviatorFragment terminalFragment = fragments[fragments.length - 1];

            while (pos < max && pos >= 0) {
                pos = terminalFragment.abbreviate(sb, pos);
            }
        }
    }
}
