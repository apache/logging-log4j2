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
package org.apache.log4j.pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * NameAbbreviator generates abbreviated logger and class names.
 */
public abstract class NameAbbreviator {

    /**
     * Abbreviator that drops starting path elements.
     */
    private static class DropElementAbbreviator extends NameAbbreviator {
        /**
         * Maximum number of path elements to output.
         */
        private final int count;

        /**
         * Create new instance.
         *
         * @param count maximum number of path elements to output.
         */
        public DropElementAbbreviator(final int count) {
            this.count = count;
        }

        /**
         * Abbreviate name.
         *
         * @param buf buffer to append abbreviation.
         * @param nameStart start of name to abbreviate.
         */
        @Override
        public void abbreviate(final int nameStart, final StringBuffer buf) {
            int i = count;
            for (int pos = buf.indexOf(".", nameStart); pos != -1; pos = buf.indexOf(".", pos + 1)) {
                if (--i == 0) {
                    buf.delete(nameStart, pos + 1);
                    break;
                }
            }
        }
    }

    /**
     * Abbreviator that drops starting path elements.
     */
    private static class MaxElementAbbreviator extends NameAbbreviator {
        /**
         * Maximum number of path elements to output.
         */
        private final int count;

        /**
         * Create new instance.
         *
         * @param count maximum number of path elements to output.
         */
        public MaxElementAbbreviator(final int count) {
            this.count = count;
        }

        /**
         * Abbreviate name.
         *
         * @param buf buffer to append abbreviation.
         * @param nameStart start of name to abbreviate.
         */
        @Override
        public void abbreviate(final int nameStart, final StringBuffer buf) {
            // We substract 1 from 'len' when assigning to 'end' to avoid out of
            // bounds exception in return r.substring(end+1, len). This can happen if
            // precision is 1 and the category name ends with a dot.
            int end = buf.length() - 1;

            final String bufString = buf.toString();
            for (int i = count; i > 0; i--) {
                end = bufString.lastIndexOf(".", end - 1);

                if ((end == -1) || (end < nameStart)) {
                    return;
                }
            }

            buf.delete(nameStart, end + 1);
        }
    }

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
        public void abbreviate(final int nameStart, final StringBuffer buf) {}
    }

    /**
     * Pattern abbreviator.
     *
     *
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
        public PatternAbbreviator(final List fragments) {
            if (fragments.size() == 0) {
                throw new IllegalArgumentException("fragments must have at least one element");
            }

            this.fragments = new PatternAbbreviatorFragment[fragments.size()];
            fragments.toArray(this.fragments);
        }

        /**
         * Abbreviate name.
         *
         * @param buf buffer that abbreviated name is appended.
         * @param nameStart start of name.
         */
        @Override
        public void abbreviate(final int nameStart, final StringBuffer buf) {
            //
            // all non-terminal patterns are executed once
            //
            int pos = nameStart;

            for (int i = 0; (i < (fragments.length - 1)) && (pos < buf.length()); i++) {
                pos = fragments[i].abbreviate(buf, pos);
            }

            //
            // last pattern in executed repeatedly
            //
            final PatternAbbreviatorFragment terminalFragment = fragments[fragments.length - 1];

            while ((pos < buf.length()) && (pos >= 0)) {
                pos = terminalFragment.abbreviate(buf, pos);
            }
        }
    }

    /**
     * Fragment of an pattern abbreviator.
     *
     */
    private static class PatternAbbreviatorFragment {
        /**
         * Count of initial characters of element to output.
         */
        private final int charCount;

        /**
         * Character used to represent dropped characters. '\0' indicates no representation of dropped characters.
         */
        private final char ellipsis;

        /**
         * Creates a PatternAbbreviatorFragment.
         *
         * @param charCount number of initial characters to preserve.
         * @param ellipsis character to represent elimination of characters, '\0' if no ellipsis is desired.
         */
        public PatternAbbreviatorFragment(final int charCount, final char ellipsis) {
            this.charCount = charCount;
            this.ellipsis = ellipsis;
        }

        /**
         * Abbreviate element of name.
         *
         * @param buf buffer to receive element.
         * @param startPos starting index of name element.
         * @return starting index of next element.
         */
        public int abbreviate(final StringBuffer buf, final int startPos) {
            int nextDot = buf.toString().indexOf(".", startPos);

            if (nextDot != -1) {
                if ((nextDot - startPos) > charCount) {
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
     * Default (no abbreviation) abbreviator.
     */
    private static final NameAbbreviator DEFAULT = new NOPAbbreviator();

    /**
     * Gets an abbreviator.
     *
     * For example, "%logger{2}" will output only 2 elements of the logger name, %logger{-2} will drop 2 elements from the
     * logger name, "%logger{1.}" will output only the first character of the non-final elements in the name,
     * "%logger{1~.2~} will output the first character of the first element, two characters of the second and subsequent
     * elements and will use a tilde to indicate abbreviated characters.
     *
     * @param pattern abbreviation pattern.
     * @return abbreviator, will not be null.
     */
    public static NameAbbreviator getAbbreviator(final String pattern) {
        if (pattern.length() > 0) {
            // if pattern is just spaces and numbers then
            // use MaxElementAbbreviator
            final String trimmed = pattern.trim();

            if (trimmed.length() == 0) {
                return DEFAULT;
            }

            int i = 0;
            if (trimmed.length() > 0) {
                if (trimmed.charAt(0) == '-') {
                    i++;
                }
                for (; (i < trimmed.length()) && (trimmed.charAt(i) >= '0') && (trimmed.charAt(i) <= '9'); i++) {}
            }

            //
            // if all blanks and digits
            //
            if (i == trimmed.length()) {
                final int elements = Integer.parseInt(trimmed);
                if (elements >= 0) {
                    return new MaxElementAbbreviator(elements);
                } else {
                    return new DropElementAbbreviator(-elements);
                }
            }

            final ArrayList fragments = new ArrayList(5);
            char ellipsis;
            int charCount;
            int pos = 0;

            while ((pos < trimmed.length()) && (pos >= 0)) {
                int ellipsisPos = pos;

                if (trimmed.charAt(pos) == '*') {
                    charCount = Integer.MAX_VALUE;
                    ellipsisPos++;
                } else {
                    if ((trimmed.charAt(pos) >= '0') && (trimmed.charAt(pos) <= '9')) {
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
                pos = trimmed.indexOf(".", pos);

                if (pos == -1) {
                    break;
                }

                pos++;
            }

            return new PatternAbbreviator(fragments);
        }

        //
        // no matching abbreviation, return defaultAbbreviator
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
     * Abbreviates a name in a StringBuffer.
     *
     * @param nameStart starting position of name in buf.
     * @param buf buffer, may not be null.
     */
    public abstract void abbreviate(final int nameStart, final StringBuffer buf);
}
