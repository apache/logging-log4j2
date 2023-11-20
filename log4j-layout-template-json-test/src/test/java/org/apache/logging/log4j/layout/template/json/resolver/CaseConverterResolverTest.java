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
package org.apache.logging.log4j.layout.template.json.resolver;

import static org.apache.logging.log4j.layout.template.json.TestHelpers.CONFIGURATION;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.asMap;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.readJson;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.usingSerializedLogEventAccessor;
import static org.apache.logging.log4j.layout.template.json.TestHelpers.writeJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;
import org.apache.logging.log4j.util.SortedArrayStringMap;
import org.apache.logging.log4j.util.StringMap;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CaseConverterResolverTest {

    @ParameterizedTest
    @CsvSource({
        // case     | locale    | input         | output
        "upper" + ",nl" + ",ioz" + ",IOZ",
        "upper" + ",nl" + ",IOZ" + ",IOZ",
        "lower" + ",nl" + ",ioz" + ",ioz",
        "lower" + ",nl" + ",IOZ" + ",ioz",
        "upper" + ",tr" + ",ıiğüşöç" + ",IİĞÜŞÖÇ",
        "upper" + ",tr" + ",IİĞÜŞÖÇ" + ",IİĞÜŞÖÇ",
        "lower" + ",tr" + ",ıiğüşöç" + ",ıiğüşöç",
        "lower" + ",tr" + ",IİĞÜŞÖÇ" + ",ıiğüşöç"
    })
    void test_upper(final String case_, final String locale, final String input, final String output) {

        // Create the event template.
        final String eventTemplate = writeJson(asMap(
                "output",
                asMap(
                        "$resolver",
                        "caseConverter",
                        "case",
                        case_,
                        "locale",
                        locale,
                        "input",
                        asMap(
                                "$resolver", "mdc",
                                "key", "input"))));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final StringMap contextData = new SortedArrayStringMap();
        contextData.putValue("input", input);
        final LogEvent logEvent =
                Log4jLogEvent.newBuilder().setContextData(contextData).build();

        // Check the serialized event.
        usingSerializedLogEventAccessor(layout, logEvent, accessor -> assertThat(accessor.getString("output"))
                .isEqualTo(output));
    }

    @ParameterizedTest
    @CsvSource({
        // failure message              | locale    | input     | strategy      | replacement   | output
        "" + ",nl" + ",1" + ",pass" + ",null" + ",1",
        "" + ",nl" + ",[2]" + ",pass" + ",null" + ",[2]",
        "was expecting a string value" + ",nl" + ",1" + ",fail" + ",null" + ",null",
        "" + ",nl" + ",1" + ",replace" + ",null" + ",null",
        "" + ",nl" + ",1" + ",replace" + ",2" + ",2",
        "" + ",nl" + ",1" + ",replace" + ",\"s\"" + ",\"s\""
    })
    void test_errorHandlingStrategy(
            final String failureMessage,
            final String locale,
            final String inputJson,
            final String errorHandlingStrategy,
            final String replacementJson,
            final String outputJson) {

        // Parse arguments.
        final Object input = readJson(inputJson);
        final Object replacement = readJson(replacementJson);
        final Object output = readJson(outputJson);

        // Create the event template.
        final String eventTemplate = writeJson(asMap(
                "output",
                asMap(
                        "$resolver", "caseConverter",
                        "case", "lower",
                        "locale", locale,
                        "input", input,
                        "errorHandlingStrategy", errorHandlingStrategy,
                        "replacement", replacement)));

        // Create the layout.
        final JsonTemplateLayout layout = JsonTemplateLayout.newBuilder()
                .setConfiguration(CONFIGURATION)
                .setEventTemplate(eventTemplate)
                .build();

        // Create the log event.
        final LogEvent logEvent = Log4jLogEvent.newBuilder().build();

        // Check the serialized event.
        final boolean failureExpected = Strings.isNotBlank(failureMessage);
        if (failureExpected) {
            assertThatThrownBy(() -> layout.toSerializable(logEvent)).hasMessageContaining(failureMessage);
        } else {
            usingSerializedLogEventAccessor(layout, logEvent, accessor -> assertThat(accessor.getObject("output"))
                    .isEqualTo(output));
        }
    }
}
