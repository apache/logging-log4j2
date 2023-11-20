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

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Specialized abbreviator that shortens all words to the first char except the indicated number of rightmost words.
 * To select this abbreviator, use pattern <code>1.n*</code> where n (&gt; 0) is the number of rightmost words to leave unchanged.</p>
 *
 * By example for input <code>org.apache.logging.log4j.core.pattern.NameAbbreviator</code>:<br>
 * <pre>
 * 1.1*     =&gt;   o.a.l.l.c.p.NameAbbreviator
 * 1.2*     =&gt;   o.a.l.l.c.pattern.NameAbbreviator
 * 1.3*     =&gt;   o.a.l.l.core.pattern.NameAbbreviator
 * ..
 * 1.999*   =&gt;   org.apache.logging.log4j.core.pattern.NameAbbreviator
 * </pre>
 * @since 2.19.1
 */
final class DynamicWordAbbreviator extends NameAbbreviator {

    /** Right-most number of words (at least one) that will not be abbreviated. */
    private final int rightWordCount;

    static DynamicWordAbbreviator create(final String pattern) {
        if (pattern != null) {
            final Matcher matcher = Pattern.compile("1\\.([1-9][0-9]*)\\*").matcher(pattern);
            if (matcher.matches()) {
                return new DynamicWordAbbreviator(Integer.parseInt(matcher.group(1)));
            }
        }
        return null;
    }

    private DynamicWordAbbreviator(final int rightWordCount) {
        this.rightWordCount = rightWordCount;
    }

    @Override
    public void abbreviate(final String original, final StringBuilder destination) {
        if (original == null || destination == null) {
            return;
        }

        // for efficiency refrain from using String#split or StringTokenizer
        final String[] words = split(original, '.');
        final int wordCount = words.length;

        if (rightWordCount >= wordCount) {
            // nothing to abbreviate
            destination.append(original);
            return;
        }

        final int lastAbbrevIdx = wordCount - rightWordCount; // last index to abbreviate
        for (int i = 0; i < wordCount; i++) {
            if (i >= lastAbbrevIdx) {
                destination.append(words[i]);
                if (i < wordCount - 1) {
                    destination.append(".");
                }
            } else if (words[i].length() > 0) {
                destination.append(words[i].charAt(0)).append(".");
            }
        }
    }

    static String[] split(final String input, final char delim) {
        if (input == null) {
            return null;
        } else if (input.isEmpty()) {
            return new String[0];
        }

        final int countDelim = input.chars().filter(c -> c == delim).map(c -> 1).sum();
        final String[] tokens = new String[countDelim + 1];

        int countToken = 0;
        int idxBegin = 0;
        int idxDelim = 0;

        while ((idxDelim = input.indexOf(delim, idxBegin)) > -1) {
            if (idxBegin < idxDelim) {
                tokens[countToken++] = input.substring(idxBegin, idxDelim);
            }
            idxBegin = idxDelim + 1;
        }

        if (idxBegin < input.length()) { // remains
            tokens[countToken++] = input.substring(idxBegin);
        }

        if (countToken < tokens.length) {
            return Arrays.copyOf(tokens, countToken);
        }

        return tokens;
    }
}
