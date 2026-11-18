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
package org.apache.logging.log4j.core.fuzz;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import org.apache.logging.log4j.message.StructuredDataId;
import org.apache.logging.log4j.message.StructuredDataMessage;

/**
 * Fuzzes {@code StructuredDataMessage.asString}, which the layout targets do not reach.
 */
public final class StructuredDataMessageFuzzer {

    // Mirrors the private StructuredDataMessage.MAX_LENGTH.
    private static final int MAX_KEY_LENGTH = 32;

    public static void fuzzerTestOneInput(final FuzzedDataProvider dataProvider) {
        final String key = sanitizeKey(dataProvider.consumeString(MAX_KEY_LENGTH));
        final String value = dataProvider.consumeString(256);
        final String message = dataProvider.consumeRemainingAsString();
        final StructuredDataMessage structured =
                new StructuredDataMessage(new StructuredDataId("fuzz@41094", null, null), message, "FUZZ");
        structured.put(key, value);
        structured.asString(StructuredDataMessage.Format.FULL, null);
    }

    // put() rejects keys outside printable US-ASCII or containing '=', ']' or '"'; map them to '?'.
    private static String sanitizeKey(final String key) {
        final StringBuilder sanitized = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            final char c = key.charAt(i);
            sanitized.append(c >= '!' && c <= '~' && c != '=' && c != ']' && c != '"' ? c : '?');
        }
        return sanitized.length() == 0 ? "?" : sanitized.toString();
    }
}
