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

import java.util.Objects;

/**
 * StringBuilder helpers
 */
public class StringBuilders {

    /**
     * Truncates the content of the given {@code StringBuilder} to the specified maximum number of lines.
     *
     * <p>If {@code maxOccurrenceCount} is {@link Integer#MAX_VALUE}, or if {@code delimiter} is empty,
     * the method returns without making any changes to the {@code StringBuilder}.
     *
     * @param buffer             the {@code StringBuilder} whose content is to be truncated
     * @param delimiter  the delimiter used to determine the end of a line
     * @param maxOccurrenceCount   the maximum number of lines to retain in the {@code StringBuilder};
     *                       if this value is {@link Integer#MAX_VALUE}, no truncation will occur
     */
    public static void truncateAfterDelimiter(
            final StringBuilder buffer, final String delimiter, final int maxOccurrenceCount) {
        Objects.requireNonNull(buffer, "buffer");
        Objects.requireNonNull(delimiter, "delimiter");
        if (maxOccurrenceCount < 0) {
            throw new IllegalArgumentException("maxOccurrenceCount should not be negative");
        }
        if (buffer.length() < delimiter.length() || delimiter.isEmpty() || maxOccurrenceCount == Integer.MAX_VALUE) {
            return;
        }
        final int delimiterLen = delimiter.length();
        int offset = 0;
        int currentOccurrenceCount = 0;
        while (currentOccurrenceCount < maxOccurrenceCount) {
            int delimiterIndex = buffer.indexOf(delimiter, offset);
            if (delimiterIndex == -1) {
                break;
            }
            currentOccurrenceCount++;
            offset = delimiterIndex + delimiterLen;
        }
        buffer.setLength(offset);
    }
}
