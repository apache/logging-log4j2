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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;

class JsonReaderTest {

    @Test
    void test_null() {
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("json");
    }

    @Test
    void test_valid_null() {
        test("null", null);
        test("[null, null]", Arrays.asList(null, null));
    }

    @Test
    void test_invalid_null() {
        for (final String json : new String[]{"nuL", "nulL", "nul1"}) {
            Assertions
                    .assertThatThrownBy(() -> JsonReader.read(json))
                    .as("json=%s", json)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith("was expecting keyword 'null' at index");

        }
    }

    @Test
    void test_valid_boolean() {
        test("true", true);
        test("false", false);
        test("[true, false]", Arrays.asList(true, false));
    }

    @Test
    void test_invalid_boolean() {
        for (final String json : new String[]{"tru", "truE", "fals", "falsE"}) {
            Assertions
                    .assertThatThrownBy(() -> JsonReader.read(json))
                    .as("json=%s", json)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageMatching("^was expecting keyword '(true|false)' at index [0-9]+: .*$");

        }
    }

    @Test
    void test_valid_string() {
        test("\"\"", "");
        test("\" \"", " ");
        test("\" a\"", " a");
        test("\"a \"", "a ");
        test("\"abc\"", "abc");
        test("\"abc\\\"\"", "abc\"");
        test("\"\\b\\f\\n\\r\\t\"", "\b\f\n\r\t");
    }

    @Test
    void test_invalid_string_start() {
        final String json = "abc\"";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid character at index 0: a");
    }

    @Test
    void test_invalid_string_end() {
        for (final String json : new String[]{"", " ", "\r", "\t", "\"abc"}) {
            Assertions
                    .assertThatThrownBy(() -> JsonReader.read(json))
                    .as("json=%s", json)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("premature end of input");
        }
    }

    @Test
    void test_invalid_string_escape() {
        for (final String json : new String[]{"\"\\k\"", "\"\\d\""}) {
            Assertions
                    .assertThatThrownBy(() -> JsonReader.read(json))
                    .as("json=%s", json)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageStartingWith(
                            "was expecting an escape character at index 2: ");
        }
    }

    @Test
    void test_invalid_string_concat() {
        final String json = "\"foo\"\"bar\"";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was not expecting input at index 5: \"");
    }

    @Test
    void test_valid_unicode_string() {
        final String json = "\"a\\u00eF4bc\"";
        Assertions
                .assertThat(JsonReader.read(json))
                .as("json=%s", json)
                .isEqualTo("a\u00ef4bc");
    }

    @Test
    void test_invalid_unicode() {
        Assertions
                .assertThatThrownBy(() -> JsonReader.read("\"\\u000x\""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting a unicode character at index 6: x");
    }

    @Test
    void test_valid_integers() {
        for (final String integer : new String[]{
                "0",
                "1",
                "" + Long.MAX_VALUE + "" + Long.MAX_VALUE}) {
            for (final String signedInteger : new String[]{integer, '-' + integer}) {
                final Object expectedToken =
                        signedInteger.length() < 3
                                ? Integer.parseInt(signedInteger)
                                : new BigInteger(signedInteger);
                test(signedInteger, expectedToken);
            }
        }
    }

    @Test
    void test_invalid_integers() {
        for (final String integer : new String[]{
                "0-",
                "1a"}) {
            for (final String signedInteger : new String[]{integer, '-' + integer}) {
                Assertions
                        .assertThatThrownBy(() -> JsonReader.read(signedInteger))
                        .as("signedInteger=%s", signedInteger)
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageStartingWith("was not expecting input at index");
            }
        }
    }

    @Test
    void test_valid_decimals() {
        for (final String decimal : new String[]{
                "0.0",
                "1.0",
                "1.2",
                "1e2",
                "1e-2",
                "1.2e3",
                "1.2e-3"}) {
            for (final String signedDecimal : new String[]{decimal, '-' + decimal}) {
                test(signedDecimal, new BigDecimal(signedDecimal));
            }
        }
    }

    @Test
    void test_invalid_decimals() {
        for (final String decimal : new String[]{
                "0.",
                ".1",
                "1e",
                "1e-",
                "1.2e",
                "1.2e-"}) {
            for (final String signedDecimal : new String[]{decimal, '-' + decimal}) {
                Assertions
                        .assertThatThrownBy(() -> JsonReader.read(signedDecimal))
                        .as("signedDecimal=%s", signedDecimal)
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    void test_valid_arrays() {
        for (final String json : new String[]{
                "[]",
                "[ ]"}) {
            test(json, Collections.emptyList());
        }
        for (final String json : new String[]{
                "[1]",
                "[ 1]",
                "[1 ]",
                "[ 1 ]"}) {
            test(json, Collections.singletonList(1));
        }
        for (final String json : new String[]{
                "[1,2]",
                "[1, 2]",
                "[ 1, 2]",
                "[1 , 2]",
                "[ 1 , 2 ]"}) {
            test(json, Arrays.asList(1, 2));
        }
    }

    @Test
    void test_invalid_array_start() {
        final String json = "[";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("premature end of input");
    }

    @Test
    void test_invalid_array_end_1() {
        final String json = "]";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was not expecting ARRAY_END at index 0");
    }

    @Test
    void test_invalid_array_comma() {
        final String json = "[,";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting an array element at index 1: COMMA");
    }

    @Test
    void test_invalid_array_end_2() {
        final String json = "[1,";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("premature end of input");
    }

    @Test
    void test_invalid_array_end_3() {
        final String json = "[1,]";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting an array element at index 3: ARRAY_END");
    }

    @Test
    void test_valid_objects() {
        test("{}", Collections.emptyMap());
        test("{\"foo\":\"bar\"}", Collections.singletonMap("foo", "bar"));
    }

    @Test
    void test_invalid_object_start() {
        final String json = "{";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("premature end of input");
    }

    @Test
    void test_invalid_object_end() {
        final String json = "}";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was not expecting OBJECT_END at index 0");
    }

    @Test
    void test_invalid_object_colon_1() {
        final String json = "{\"foo\"\"bar\"}";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting COLON at index 6: bar");
    }

    @Test
    void test_invalid_object_colon_2() {
        final String json = "{\"foo\":}";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("premature end of input");
    }

    @Test
    void test_invalid_object_token() {
        final String json = "{\"foo\":\"bar}";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("premature end of input");
    }

    @Test
    void test_invalid_object_comma() {
        final String json = "{\"foo\":\"bar\",}";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting an object key at index 13: OBJECT_END");
    }

    @Test
    void test_invalid_object_key() {
        final String json = "{\"foo\":\"bar\",]}";
        Assertions
                .assertThatThrownBy(() -> JsonReader.read(json))
                .as("json=%s", json)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("was expecting an object key at index 13: ARRAY_END");
    }

    @Test
    @SuppressWarnings("DoubleBraceInitialization")
    public void test_nesting() {
        test(
                "{\"k1\": [true, null, 1e5, {\"k2\": \"v2\", \"k3\": {\"k4\": \"v4\"}}]}",
                Collections.singletonMap(
                        "k1",
                        Arrays.asList(
                                true,
                                null,
                                new BigDecimal("1e5"),
                                new LinkedHashMap<String, Object>() {{
                                    put("k2", "v2");
                                    put("k3", Collections.singletonMap("k4", "v4"));
                                }})));
    }

    private void test(final String json, final Object expected) {
        // Wrapping the assertion one more time to decorate it with the input.
        Assertions
                .assertThatCode(() -> Assertions
                        .assertThat(JsonReader.read(json))
                        .isEqualTo(expected))
                .as("json=%s", json)
                .doesNotThrowAnyException();
    }

}
