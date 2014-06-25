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

/**
 * Modifies the output of a pattern converter for a specified minimum and maximum width and alignment.
 */
public final class FormattingInfo {
    /**
     * Array of spaces.
     */
    private static final char[] SPACES = new char[] { ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ' };

    /**
     * Default instance.
     */
    private static final FormattingInfo DEFAULT = new FormattingInfo(false, 0, Integer.MAX_VALUE);

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
     * Creates new instance.
     *
     * @param leftAlign
     *            left align if true.
     * @param minLength
     *            minimum length.
     * @param maxLength
     *            maximum length.
     */
    public FormattingInfo(final boolean leftAlign, final int minLength, final int maxLength) {
        this.leftAlign = leftAlign;
        this.minLength = minLength;
        this.maxLength = maxLength;
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
            buffer.delete(fieldStart, buffer.length() - maxLength);
        } else if (rawLength < minLength) {
            if (leftAlign) {
                final int fieldEnd = buffer.length();
                buffer.setLength(fieldStart + minLength);

                for (int i = fieldEnd; i < buffer.length(); i++) {
                    buffer.setCharAt(i, ' ');
                }
            } else {
                int padLength = minLength - rawLength;

                for (; padLength > SPACES.length; padLength -= SPACES.length) {
                    buffer.insert(fieldStart, SPACES);
                }

                buffer.insert(fieldStart, SPACES, 0, padLength);
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
        sb.append(']');
        return sb.toString();
    }

}
