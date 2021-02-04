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

import org.apache.logging.log4j.layout.template.json.util.StringParameterParser.DoubleQuotedStringValue;
import org.apache.logging.log4j.layout.template.json.util.StringParameterParser.NullValue;
import org.apache.logging.log4j.layout.template.json.util.StringParameterParser.StringValue;
import org.apache.logging.log4j.layout.template.json.util.StringParameterParser.Value;
import org.apache.logging.log4j.layout.template.json.util.StringParameterParser.Values;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("DoubleBraceInitialization")
class StringParameterParserTest {

    @Test
    void test_empty_string() {
        testSuccess(
                "",
                Collections.emptyMap());
    }

    @Test
    void test_blank_string() {
        testSuccess(
                "\t",
                Collections.emptyMap());
    }

    @Test
    void test_simple_pair() {
        testSuccess(
                "a=b",
                Collections.singletonMap("a", Values.stringValue("b")));
    }

    @Test
    void test_simple_pair_with_whitespace_1() {
        testSuccess(
                " a=b",
                Collections.singletonMap("a", Values.stringValue("b")));
    }

    @Test
    void test_simple_pair_with_whitespace_2() {
        testSuccess(
                " a =b",
                Collections.singletonMap("a", Values.stringValue("b")));
    }

    @Test
    void test_simple_pair_with_whitespace_3() {
        testSuccess(
                " a = b",
                Collections.singletonMap("a", Values.stringValue("b")));
    }

    @Test
    void test_simple_pair_with_whitespace_4() {
        testSuccess(
                " a = b ",
                Collections.singletonMap("a", Values.stringValue("b")));
    }

    @Test
    void test_null_value_1() {
        testSuccess(
                "a",
                Collections.singletonMap("a", Values.nullValue()));
    }

    @Test
    void test_null_value_2() {
        testSuccess(
                "a,b=c,d=",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.nullValue());
                    put("b", Values.stringValue("c"));
                    put("d", Values.nullValue());
                }});
    }

    @Test
    void test_null_value_3() {
        testSuccess(
                "a,b=c,d",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.nullValue());
                    put("b", Values.stringValue("c"));
                    put("d", Values.nullValue());
                }});
    }

    @Test
    void test_null_value_4() {
        testSuccess(
                "a,b=\"c,=\\\"\",d=,e=f",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.nullValue());
                    put("b", Values.doubleQuotedStringValue("c,=\""));
                    put("d", Values.nullValue());
                    put("e", Values.stringValue("f"));
                }});
    }

    @Test
    void test_two_pairs() {
        testSuccess(
                "a=b,c=d",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.stringValue("b"));
                    put("c", Values.stringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_01() {
        testSuccess(
                "a=\"b\"",
                Collections.singletonMap("a", Values.doubleQuotedStringValue("b")));
    }

    @Test
    void test_quoted_string_02() {
        testSuccess(
                "a=\"b\",c=d",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("b"));
                    put("c", Values.stringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_03() {
        testSuccess(
                "a=b,c=\"d\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.stringValue("b"));
                    put("c", Values.doubleQuotedStringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_04() {
        testSuccess(
                "a=\"b\",c=\"d\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("b"));
                    put("c", Values.doubleQuotedStringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_05() {
        testSuccess(
                "a=\"\\\"b\"",
                Collections.singletonMap("a", Values.doubleQuotedStringValue("\"b")));
    }

    @Test
    void test_quoted_string_06() {
        testSuccess(
                "a=\"\\\"b\\\"\"",
                Collections.singletonMap("a", Values.doubleQuotedStringValue("\"b\"")));
    }

    @Test
    void test_quoted_string_07() {
        testSuccess(
                "a=\"\\\"b\",c=d",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b"));
                    put("c", Values.stringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_08() {
        testSuccess(
                "a=\"\\\"b\\\"\",c=d",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b\""));
                    put("c", Values.stringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_09() {
        testSuccess(
                "a=\"\\\"b,\",c=d",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b,"));
                    put("c", Values.stringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_10() {
        testSuccess(
                "a=\"\\\"b\\\",\",c=d",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b\","));
                    put("c", Values.stringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_11() {
        testSuccess(
                "a=\"\\\"b\",c=\"d\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b"));
                    put("c", Values.doubleQuotedStringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_12() {
        testSuccess(
                "a=\"\\\"b\\\"\",c=\"d\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b\""));
                    put("c", Values.doubleQuotedStringValue("d"));
                }});
    }

    @Test
    void test_quoted_string_13() {
        testSuccess(
                "a=\"\\\"b,\",c=\"\\\"d\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b,"));
                    put("c", Values.doubleQuotedStringValue("\"d"));
                }});
    }

    @Test
    void test_quoted_string_14() {
        testSuccess(
                "a=\"\\\"b\\\",\",c=\"\\\"d\\\"\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b\","));
                    put("c", Values.doubleQuotedStringValue("\"d\""));
                }});
    }

    @Test
    void test_quoted_string_15() {
        testSuccess(
                "a=\"\\\"b\",c=\",d\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b"));
                    put("c", Values.doubleQuotedStringValue(",d"));
                }});
    }

    @Test
    void test_quoted_string_16() {
        testSuccess(
                "a=\"\\\"b\\\"\",c=\",d\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b\""));
                    put("c", Values.doubleQuotedStringValue(",d"));
                }});
    }

    @Test
    void test_quoted_string_17() {
        testSuccess(
                "a=\"\\\"b,\",c=\"\\\"d,\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b,"));
                    put("c", Values.doubleQuotedStringValue("\"d,"));
                }});
    }

    @Test
    void test_quoted_string_18() {
        testSuccess(
                "a=\"\\\"b\\\",\",c=\"\\\"d\\\",\"",
                new LinkedHashMap<String, Value>() {{
                    put("a", Values.doubleQuotedStringValue("\"b\","));
                    put("c", Values.doubleQuotedStringValue("\"d\","));
                }});
    }

    private static void testSuccess(
            final String input,
            final Map<String, Value> expectedMap) {
        final Map<String, Value> actualMap = StringParameterParser.parse(input);
        Assertions
                .assertThat(actualMap)
                .as("input: %s", input)
                .isEqualTo(expectedMap);
    }

    @Test
    void test_missing_key() {
        Assertions
                .assertThatThrownBy(() -> {
                    final String input = ",a=b";
                    StringParameterParser.parse(input);
                })
                .hasMessageStartingWith("failed to locate key at index 0");
    }

    @Test
    void test_conflicting_key() {
        Assertions
                .assertThatThrownBy(() -> {
                    final String input = "a,a";
                    StringParameterParser.parse(input);
                })
                .hasMessageStartingWith("conflicting key at index 2");
    }

    @Test
    void test_prematurely_ending_quoted_string_01() {
        Assertions
                .assertThatThrownBy(() -> {
                    final String input = "a,b=\"";
                    StringParameterParser.parse(input);
                })
                .hasMessageStartingWith("failed to locate the end of double-quoted content starting at index 4");
    }

    @Test
    void test_prematurely_ending_quoted_string_02() {
        Assertions
                .assertThatThrownBy(() -> {
                    final String input = "a,b=\"c";
                    StringParameterParser.parse(input);
                })
                .hasMessageStartingWith("failed to locate the end of double-quoted content starting at index 4");
    }

    @Test
    void test_prematurely_ending_quoted_string_03() {
        Assertions
                .assertThatThrownBy(() -> {
                    final String input = "a,b=\",c";
                    StringParameterParser.parse(input);
                })
                .hasMessageStartingWith("failed to locate the end of double-quoted content starting at index 4");
    }

    @Test
    void test_prematurely_ending_quoted_string_04() {
        Assertions
                .assertThatThrownBy(() -> {
                    final String input = "a,b=\",c\" x";
                    StringParameterParser.parse(input);
                })
                .hasMessageStartingWith("was expecting comma at index 9");
    }

    @Test
    void test_NullValue_toString() {
        final Map<String, Value> map = StringParameterParser.parse("a");
        final NullValue value = (NullValue) map.get("a");
        Assertions.assertThat(value.toString()).isEqualTo("null");
    }

    @Test
    void test_StringValue_toString() {
        final Map<String, Value> map = StringParameterParser.parse("a=b");
        final StringValue value = (StringValue) map.get("a");
        Assertions.assertThat(value.toString()).isEqualTo("b");
    }

    @Test
    void test_DoubleQuotedStringValue_toString() {
        final Map<String, Value> map = StringParameterParser.parse("a=\"\\\"b\"");
        final DoubleQuotedStringValue value = (DoubleQuotedStringValue) map.get("a");
        Assertions.assertThat(value.toString()).isEqualTo("\"b");
    }

    @Test
    void test_allowedKeys() {
        Assertions
                .assertThatThrownBy(() -> {
                    final String input = "a,b";
                    final Set<String> allowedKeys =
                            new LinkedHashSet<>(Collections.singletonList("a"));
                    StringParameterParser.parse(input, allowedKeys);
                })
                .hasMessageStartingWith("unknown key \"b\" is found in input: a,b");
    }

}
