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
package org.apache.logging.log4j.layout.template.json.util;

import org.apache.logging.log4j.util.Strings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;

public final class StringParameterParser {

    private StringParameterParser() {}

    public static final class Values {

        private Values() {}

        static NullValue nullValue() {
            return NullValue.INSTANCE;
        }

        static StringValue stringValue(final String string) {
            return new StringValue(string);
        }

        static DoubleQuotedStringValue doubleQuotedStringValue(
                final String doubleQuotedString) {
            return new DoubleQuotedStringValue(doubleQuotedString);
        }

    }

    public interface Value {}

    public static final class NullValue implements Value {

        private static final NullValue INSTANCE = new NullValue();

        private NullValue() {}

        @Override
        public String toString() {
            return "null";
        }

    }

    public static final class StringValue implements Value {

        private final String string;

        private StringValue(String string) {
            this.string = string;
        }

        public String getString() {
            return string;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            StringValue that = (StringValue) object;
            return string.equals(that.string);
        }

        @Override
        public int hashCode() {
            return 31 + Objects.hashCode(string);
        }

        @Override
        public String toString() {
            return string;
        }

    }

    public static final class DoubleQuotedStringValue implements Value {

        private final String doubleQuotedString;

        private DoubleQuotedStringValue(String doubleQuotedString) {
            this.doubleQuotedString = doubleQuotedString;
        }

        public String getDoubleQuotedString() {
            return doubleQuotedString;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
                return false;
            }
            DoubleQuotedStringValue that = (DoubleQuotedStringValue) object;
            return doubleQuotedString.equals(that.doubleQuotedString);
        }

        @Override
        public int hashCode() {
            return 31 + Objects.hashCode(doubleQuotedString);
        }

        @Override
        public String toString() {
            return doubleQuotedString.replaceAll("\\\\\"", "\"");
        }

    }

    private enum State { READING_KEY, READING_VALUE }

    private static final class Parser implements Callable<Map<String, Value>> {

        private final String input;

        private final Map<String, Value> map;

        private State state;

        private int i;

        private String key;

        private Parser(final String input) {
            this.input = Objects.requireNonNull(input, "input");
            this.map = new LinkedHashMap<>();
            this.state = State.READING_KEY;
            this.i = 0;
            this.key = null;
        }

        @Override
        public Map<String, Value> call() {
            while (true) {
                skipWhitespace();
                if (i >= input.length()) {
                    break;
                }
                switch (state) {
                    case READING_KEY:
                        readKey();
                        break;
                    case READING_VALUE:
                        readValue();
                        break;
                    default:
                        throw new IllegalStateException("unknown state: " + state);
                }
            }
            if (state == State.READING_VALUE) {
                map.put(key, Values.nullValue());
            }
            return map;
        }

        private void readKey() {
            final int eq = input.indexOf('=', i);
            final int co = input.indexOf(',', i);
            final int j;
            final int nextI;
            if (eq < 0 && co < 0) {
                // Neither '=', nor ',' was found.
                j = nextI = input.length();
            } else if (eq < 0) {
                // Found ','.
                j = nextI = co;
            } else if (co < 0) {
                // Found '='.
                j = eq;
                nextI = eq + 1;
            } else if (eq < co) {
                // Found '=...,'.
                j = eq;
                nextI = eq + 1;
            } else {
                // Found ',...='.
                j = co;
                nextI = co;
            }
            key = input.substring(i, j).trim();
            if (Strings.isEmpty(key)) {
                final String message = String.format(
                        "failed to locate key at index %d: %s",
                        i, input);
                throw new IllegalArgumentException(message);
            }
            if (map.containsKey(key)) {
                final String message = String.format(
                        "conflicting key at index %d: %s",
                        i, input);
                throw new IllegalArgumentException(message);
            }
            state = State.READING_VALUE;
            i = nextI;
        }

        private void readValue() {
            final boolean doubleQuoted = input.charAt(i) == '"';
            if (doubleQuoted) {
                readDoubleQuotedStringValue();
            } else {
                readStringValue();
            }
            key = null;
            state = State.READING_KEY;
        }

        private void readDoubleQuotedStringValue() {
            int j = i + 1;
            while (j < input.length()) {
                if (input.charAt(j) == '"' && input.charAt(j - 1) != '\\') {
                    break;
                } else {
                    j++;
                }
            }
            if (j >= input.length()) {
                final String message = String.format(
                        "failed to locate the end of double-quoted content starting at index %d: %s",
                        i, input);
                throw new IllegalArgumentException(message);
            }
            final String content = input
                    .substring(i + 1, j)
                    .replaceAll("\\\\\"", "\"");
            final Value value = Values.doubleQuotedStringValue(content);
            map.put(key, value);
            i = j + 1;
            skipWhitespace();
            if (i < input.length()) {
                if (input.charAt(i) != ',') {
                    final String message = String.format(
                            "was expecting comma at index %d: %s",
                            i, input);
                    throw new IllegalArgumentException(message);
                }
                i++;
            }
        }

        private void skipWhitespace() {
            while (i < input.length()) {
                final char c = input.charAt(i);
                if (!Character.isWhitespace(c)) {
                    break;
                } else {
                    i++;
                }
            }
        }

        private void readStringValue() {
            int j = input.indexOf(',', i/* + 1*/);
            if (j < 0) {
                j = input.length();
            }
            final String content = input.substring(i, j);
            final String trimmedContent = content.trim();
            final Value value = trimmedContent.isEmpty()
                    ? Values.nullValue()
                    : Values.stringValue(trimmedContent);
            map.put(key, value);
            i += content.length() + 1;
        }

    }

    public static Map<String, Value> parse(final String input) {
        return parse(input, null);
    }

    public static Map<String, Value> parse(
            final String input,
            final Set<String> allowedKeys) {
        if (Strings.isBlank(input)) {
            return Collections.emptyMap();
        }
        final Map<String, Value> map = new Parser(input).call();
        final Set<String> actualKeys = map.keySet();
        for (final String actualKey : actualKeys) {
            final boolean allowed = allowedKeys == null || allowedKeys.contains(actualKey);
            if (!allowed) {
                final String message = String.format(
                        "unknown key \"%s\" is found in input: %s",
                        actualKey, input);
                throw new IllegalArgumentException(message);
            }
        }
        return map;
    }

}
