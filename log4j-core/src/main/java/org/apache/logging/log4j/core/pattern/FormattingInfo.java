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

import org.apache.logging.log4j.util.PerformanceSensitive;

/**
 * Modifies the output of a pattern converter for a specified minimum and maximum width and alignment.
 */
@PerformanceSensitive("allocation")
public final class FormattingInfo {
    /**
     * Array of spaces.
     */
    private static final char[] SPACES = new char[] {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};

    /**
     * Array of zeros.
     */
    private static final char[] ZEROS = new char[] {'0', '0', '0', '0', '0', '0', '0', '0'};

    /**
     * Default instance.
     */
    private static final FormattingInfo DEFAULT = new FormattingInfo(false, 0, Integer.MAX_VALUE, true);

    /**
     * Minimum length.
     */
    private final int minLength;

    /**
     * Maximum length.
     */
    private final int maxLength;

    /**
     * Alignment.
     */
    private final boolean leftAlign;

    /**
     * Left vs. right-hand side truncation.
     */
    private final boolean leftTruncate;

    /**
     * Use zero-padding instead whitespace padding
     */
    private final boolean zeroPad;

    /**
     * Empty array.
     */
    public static final FormattingInfo[] EMPTY_ARRAY = {};

    /**
     * Creates new instance.
     *
     * @param leftAlign
     *            left align if true.
     * @param minLength
     *            minimum length.
     * @param maxLength
     *            maximum length.
     * @param leftTruncate
     *            truncates to the left if true
     */
    public FormattingInfo(
            final boolean leftAlign, final int minLength, final int maxLength, final boolean leftTruncate) {
        this(leftAlign, minLength, maxLength, leftTruncate, false);
    }

    /**
     * Creates new instance.
     *
     * @param leftAlign
     *            left align if true.
     * @param minLength
     *            minimum length.
     * @param maxLength
     *            maximum length.
     * @param leftTruncate
     *            truncates to the left if true
     * @param zeroPad
     *            use zero-padding instead of whitespace-padding
     */
    public FormattingInfo(
            final boolean leftAlign,
            final int minLength,
            final int maxLength,
            final boolean leftTruncate,
            final boolean zeroPad) {
        this.leftAlign = leftAlign;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.leftTruncate = leftTruncate;
        this.zeroPad = zeroPad;
    }

    /**
     * Gets default instance.
     *
     * @return default instance.
     */
    public static FormattingInfo getDefault() {
        return DEFAULT;
    }

    /**
     * Determine if left aligned.
     *
     * @return true if left aligned.
     */
    public boolean isLeftAligned() {
        return leftAlign;
    }

    /**
     * Determine if left truncated.
     *
     * @return true if left truncated.
     */
    public boolean isLeftTruncate() {
        return leftTruncate;
    }

    /**
     * Determine if zero-padded.
     *
     * @return true if zero-padded.
     */
    public boolean isZeroPad() {
        return zeroPad;
    }

    /**
     * Get minimum length.
     *
     * @return minimum length.
     */
    public int getMinLength() {
        return minLength;
    }

    /**
     * Get maximum length.
     *
     * @return maximum length.
     */
    public int getMaxLength() {
        return maxLength;
    }

    /**
     * Adjust the content of the buffer based on the specified lengths and alignment.
     *
     * @param fieldStart
     *            start of field in buffer.
     * @param buffer
     *            buffer to be modified.
     */
    public void format(final int fieldStart, final StringBuilder buffer) {
        final int rawLength = buffer.length() - fieldStart;

        if (rawLength > maxLength) {
            if (leftTruncate) {
                buffer.delete(fieldStart, buffer.length() - maxLength);
            } else {
                buffer.delete(fieldStart + maxLength, fieldStart + buffer.length());
            }
        } else if (rawLength < minLength) {
            if (leftAlign) {
                final int fieldEnd = buffer.length();
                buffer.setLength(fieldStart + minLength);

                for (int i = fieldEnd; i < buffer.length(); i++) {
                    buffer.setCharAt(i, ' ');
                }
            } else {
                int padLength = minLength - rawLength;

                final char[] paddingArray = zeroPad ? ZEROS : SPACES;

                for (; padLength > paddingArray.length; padLength -= paddingArray.length) {
                    buffer.insert(fieldStart, paddingArray);
                }

                buffer.insert(fieldStart, paddingArray, 0, padLength);
            }
        }
    }

    /**
     * Returns a String suitable for debugging.
     *
     * @return a String suitable for debugging.
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("[leftAlign=");
        sb.append(leftAlign);
        sb.append(", maxLength=");
        sb.append(maxLength);
        sb.append(", minLength=");
        sb.append(minLength);
        sb.append(", leftTruncate=");
        sb.append(leftTruncate);
        sb.append(", zeroPad=");
        sb.append(zeroPad);
        sb.append(']');
        return sb.toString();
    }
}
