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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple JSON parser mapping tokens to basic Java types.
 * <p>
 * The type mapping is as follows:
 * <p>
 * <ul>
 * <li><tt>object</tt>s are mapped to {@link LinkedHashMap LinkedHashMap&lt;String,Object&gt;}
 * <li><tt>array</tt>s are mapped to {@link LinkedList}
 * <li><tt>string</tt>s are mapped to {@link String} with proper Unicode and
 * escape character conversion
 * <li><tt>true</tt>, <tt>false</tt>, and <tt>null</tt> are mapped to their Java
 * counterparts
 * <li>floating point <tt>number</tt>s are mapped to {@link BigDecimal}
 * <li>integral <tt>number</tt>s are mapped to either primitive types
 * (<tt>int</tt>, <tt>long</tt>) or {@link BigInteger}
 * </ul>
 * <p>
 * This code is heavily influenced by the reader of
 * <a href="https://github.com/bolerio/mjson/blob/e7a4da2daa6e17a63ec057948bc30818e8f44686/src/java/mjson/Json.java#L2684">mjson</a>.
 */
public final class JsonReader {

    private enum Delimiter {

        OBJECT_START("{"),

        OBJECT_END("}"),

        ARRAY_START("["),

        ARRAY_END("]"),

        COLON(":"),

        COMMA(",");

        private final String string;

        Delimiter(final String string) {
            this.string = string;
        }

        private static boolean exists(final Object token) {
            for (Delimiter delimiter : values()) {
                if (delimiter.string.equals(token)) {
                    return true;
                }
            }
            return false;
        }

    }

    private CharacterIterator it;

    private int readCharIndex = -1;

    private char readChar;

    private int readTokenStartIndex = -1;

    private Object readToken;

    private final StringBuilder buffer;

    private JsonReader() {
         this.buffer = new StringBuilder();
    }

    public static Object read(final String json) {
        Objects.requireNonNull(json, "json");
        final JsonReader reader = new JsonReader();
        return reader.read(new StringCharacterIterator(json));
    }

    private Object read(final CharacterIterator ci) {
        it = ci;
        readCharIndex = 0;
        readChar = it.first();
        final Object token = readToken();
        if (token instanceof Delimiter) {
            final String message = String.format(
                    "was not expecting %s at index %d",
                    readToken, readTokenStartIndex);
            throw new IllegalArgumentException(message);
        }
        skipWhiteSpace();
        if (it.getIndex() != it.getEndIndex()) {
            final String message = String.format(
                    "was not expecting input at index %d: %c",
                    readCharIndex, readChar);
            throw new IllegalArgumentException(message);
        }
        return token;
    }

    private Object readToken() {
        skipWhiteSpace();
        readTokenStartIndex = readCharIndex;
        final char prevChar = readChar;
        readChar();
        switch (prevChar) {

            case '"':
                readToken = readString();
                break;

            case '[':
                readToken = readArray();
                break;

            case ']':
                readToken = Delimiter.ARRAY_END;
                break;

            case ',':
                readToken = Delimiter.COMMA;
                break;

            case '{':
                readToken = readObject();
                break;

            case '}':
                readToken = Delimiter.OBJECT_END;
                break;

            case ':':
                readToken = Delimiter.COLON;
                break;

            case 't':
                readToken = readTrue();
                break;

            case 'f':
                readToken = readFalse();
                break;

            case 'n':
                readToken = readNull();
                break;

            default:
                unreadChar();
                if (Character.isDigit(readChar) || readChar == '-') {
                    readToken = readNumber();
                } else {
                    String message = String.format(
                            "invalid character at index %d: %c",
                            readCharIndex, readChar);
                    throw new IllegalArgumentException(message);
                }

        }
        return readToken;
    }

    private void skipWhiteSpace() {
        do {
            if (!Character.isWhitespace(readChar)) {
                break;
            }
        } while (readChar() != CharacterIterator.DONE);
    }

    private char readChar() {
        if (it.getIndex() == it.getEndIndex()) {
            throw new IllegalArgumentException("premature end of input");
        }
        readChar = it.next();
        readCharIndex = it.getIndex();
        return readChar;
    }

    private void unreadChar() {
        readChar = it.previous();
        readCharIndex = it.getIndex();
    }

    private String readString() {
        buffer.setLength(0);
        while (readChar != '"') {
            if (readChar == '\\') {
                readChar();
                if (readChar == 'u') {
                    final char unicodeChar = readUnicodeChar();
                    bufferChar(unicodeChar);
                } else {
                    switch (readChar) {
                        case '"':
                        case '\\':
                            bufferReadChar();
                            break;
                        case 'b':
                            bufferChar('\b');
                            break;
                        case 'f':
                            bufferChar('\f');
                            break;
                        case 'n':
                            bufferChar('\n');
                            break;
                        case 'r':
                            bufferChar('\r');
                            break;
                        case 't':
                            bufferChar('\t');
                            break;
                        default: {
                            final String message = String.format(
                                    "was expecting an escape character at index %d: %c",
                                    readCharIndex, readChar);
                            throw new IllegalArgumentException(message);
                        }
                    }
                }
            } else {
                bufferReadChar();
            }
        }
        readChar();
        return buffer.toString();
    }

    private void bufferReadChar() {
        bufferChar(readChar);
    }

    private void bufferChar(final char c) {
        buffer.append(c);
        readChar();
    }

    private char readUnicodeChar() {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            readChar();
            if (readChar >= '0' && readChar <= '9') {
                value = (value << 4) + readChar - '0';
            } else if (readChar >= 'a' && readChar <= 'f') {
                value = (value << 4) + (readChar - 'a') + 10;
            } else if (readChar >= 'A' && readChar <= 'F') {
                value = (value << 4) + (readChar - 'A') + 10;
            } else {
                final String message = String.format(
                        "was expecting a unicode character at index %d: %c",
                        readCharIndex, readChar);
                throw new IllegalArgumentException(message);
            }
        }
        return (char) value;
    }

    private Map<String, Object> readObject() {
        final Map<String, Object> object = new LinkedHashMap<>();
        String key = readObjectKey();
        while (readToken != Delimiter.OBJECT_END) {
            expectDelimiter(Delimiter.COLON, readToken());
            if (readToken != Delimiter.OBJECT_END) {
                Object value = readToken();
                object.put(key, value);
                if (readToken() == Delimiter.COMMA) {
                    key = readObjectKey();
                    if (key == null || Delimiter.exists(key)) {
                        String message = String.format(
                                "was expecting an object key at index %d: %s",
                                readTokenStartIndex, readToken);
                        throw new IllegalArgumentException(message);
                    }
                } else {
                    expectDelimiter(Delimiter.OBJECT_END, readToken);
                }
            }
        }
        return object;
    }

    private List<Object> readArray() {
        @SuppressWarnings("JdkObsolete")
        final List<Object> array = new LinkedList<>();
        readToken();
        while (readToken != Delimiter.ARRAY_END) {
            if (readToken instanceof Delimiter) {
                final String message = String.format(
                        "was expecting an array element at index %d: %s",
                        readTokenStartIndex, readToken);
                throw new IllegalArgumentException(message);
            }
            array.add(readToken);
            if (readToken() == Delimiter.COMMA) {
                if (readToken() == Delimiter.ARRAY_END) {
                    final String message = String.format(
                            "was expecting an array element at index %d: %s",
                            readTokenStartIndex, readToken);
                    throw new IllegalArgumentException(message);
                }
            } else {
                expectDelimiter(Delimiter.ARRAY_END, readToken);
            }
        }
        return array;
    }

    private String readObjectKey() {
        readToken();
        if (readToken == Delimiter.OBJECT_END) {
            return null;
        } else if (readToken instanceof String) {
            return (String) readToken;
        } else {
            final String message = String.format(
                    "was expecting an object key at index %d: %s",
                    readTokenStartIndex, readToken);
            throw new IllegalArgumentException(message);
        }
    }

    private void expectDelimiter(
            final Delimiter expectedDelimiter,
            final Object actualToken) {
        if (!expectedDelimiter.equals(actualToken)) {
            String message = String.format(
                    "was expecting %s at index %d: %s",
                    expectedDelimiter, readTokenStartIndex, actualToken);
            throw new IllegalArgumentException(message);
        }
    }

    private boolean readTrue() {
        if (readChar != 'r' || readChar() != 'u' || readChar() != 'e') {
            String message = String.format(
                    "was expecting keyword 'true' at index %d: %s",
                    readCharIndex, readChar);
            throw new IllegalArgumentException(message);
        }
        readChar();
        return true;
    }

    private boolean readFalse() {
        if (readChar != 'a' || readChar() != 'l' || readChar() != 's' || readChar() != 'e') {
            String message = String.format(
                    "was expecting keyword 'false' at index %d: %s",
                    readCharIndex, readChar);
            throw new IllegalArgumentException(message);
        }
        readChar();
        return false;
    }

    private Object readNull() {
        if (readChar != 'u' || readChar() != 'l' || readChar() != 'l') {
            String message = String.format(
                    "was expecting keyword 'null' at index %d: %s",
                    readCharIndex, readChar);
            throw new IllegalArgumentException(message);
        }
        readChar();
        return null;
    }

    private Number readNumber() {

        // Read sign.
        buffer.setLength(0);
        if (readChar == '-') {
            bufferReadChar();
        }

        // Read fraction.
        boolean floatingPoint = false;
        bufferDigits();
        if (readChar == '.') {
            bufferReadChar();
            bufferDigits();
            floatingPoint = true;
        }

        // Read exponent.
        if (readChar == 'e' || readChar == 'E') {
            floatingPoint = true;
            bufferReadChar();
            if (readChar == '+' || readChar == '-') {
                bufferReadChar();
            }
            bufferDigits();
        }

        // Convert the read number.
        final String string = buffer.toString();
        if (floatingPoint) {
            return new BigDecimal(string);
        } else {
            final BigInteger bigInteger = new BigInteger(string);
            try {
                return bigInteger.intValueExact();
            } catch (ArithmeticException ignoredIntOverflow) {
                try {
                    return bigInteger.longValueExact();
                } catch (ArithmeticException ignoredLongOverflow) {
                    return bigInteger;
                }
            }
        }

    }

    private void bufferDigits() {
        boolean found = false;
        while (Character.isDigit(readChar)) {
            found = true;
            bufferReadChar();
        }
        if (!found) {
            final String message = String.format(
                    "was expecting a digit at index %d: %c",
                    readCharIndex, readChar);
            throw new IllegalArgumentException(message);
        }
    }

}
