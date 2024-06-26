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
package org.apache.logging.log4j.core.util.internal;

/**
 * StringBuilder helpers
 */
public class StringBuilders {

    /**
     * Truncates the content of the given {@code StringBuilder} to the specified maximum number of lines.
     *
     * <p>If {@code maxLineCount} is {@code null}, {@link Integer#MAX_VALUE}, or if {@code lineSeparator} is empty,
     * the method returns without making any changes to the {@code StringBuilder}.
     *
     * @param buffer             the {@code StringBuilder} whose content is to be truncated
     * @param lineSeparator  the line separator used to determine the end of a line
     * @param maxLineCount   the maximum number of lines to retain in the {@code StringBuilder};
     *                       if this value is {@code null} or {@link Integer#MAX_VALUE}, no truncation will occur
     */
    public static void truncateLines(
            final StringBuilder buffer, final String lineSeparator, final Integer maxLineCount) {
        if (buffer == null
                || maxLineCount == null
                || maxLineCount == Integer.MAX_VALUE
                || lineSeparator == null
                || lineSeparator.isEmpty()) {
            return;
        }
        final int lineSeparatorLen = lineSeparator.length();
        int offset = 0;
        int currentLineCount = 0;
        while (currentLineCount < maxLineCount) {
            int lineSeparatorIndex = buffer.indexOf(lineSeparator, offset);
            if (lineSeparatorIndex == -1) {
                break;
            }
            currentLineCount++;
            offset = lineSeparatorIndex + lineSeparatorLen;
        }
        buffer.setLength(offset);
    }
}
