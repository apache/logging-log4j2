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
package org.apache.logging.log4j.layout.json.template.util;

import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.logging.log4j.layout.json.template.ObjectMapperFixture;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonGeneratorsTest {

    private static final class WriteDoubleTestCase {

        private final long integralPart;

        private final int fractionalPart;

        private WriteDoubleTestCase(
                final long integralPart,
                final int fractionalPart) {
            this.integralPart = integralPart;
            this.fractionalPart = fractionalPart;
        }

        private String write() throws IOException {
            try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 final JsonGenerator jsonGenerator = ObjectMapperFixture
                         .getObjectMapper()
                         .getFactory()
                         .createGenerator(outputStream)) {
                JsonGenerators.writeDouble(jsonGenerator, integralPart, fractionalPart);
                jsonGenerator.flush();
                return outputStream.toString(StandardCharsets.UTF_8.name());
            }
        }

    }

    @Test
    public void test_writeDouble() {

        // Create test cases.
        final Map<String, WriteDoubleTestCase> testCaseByExpectedJson = new LinkedHashMap<>();
        testCaseByExpectedJson.put(
                "" + Long.MIN_VALUE,
                new WriteDoubleTestCase(Long.MIN_VALUE, 0));
        testCaseByExpectedJson.put(
                "" + Long.MIN_VALUE + '.' + Integer.MAX_VALUE,
                new WriteDoubleTestCase(Long.MIN_VALUE, Integer.MAX_VALUE));
        testCaseByExpectedJson.put(
                "" + Long.MAX_VALUE,
                new WriteDoubleTestCase(Long.MAX_VALUE, 0));
        testCaseByExpectedJson.put(
                "" + Long.MAX_VALUE + '.' + Integer.MAX_VALUE,
                new WriteDoubleTestCase(Long.MAX_VALUE, Integer.MAX_VALUE));
        testCaseByExpectedJson.put("0", new WriteDoubleTestCase(0, 0));
        testCaseByExpectedJson.put("1", new WriteDoubleTestCase(1, 0));
        testCaseByExpectedJson.put("-1", new WriteDoubleTestCase(-1, 0));
        testCaseByExpectedJson.put("1.2", new WriteDoubleTestCase(1, 2));
        testCaseByExpectedJson.put("-1.2", new WriteDoubleTestCase(-1, 2));

        // Execute test cases.
        testCaseByExpectedJson.forEach(
                (final String expectedJson, final WriteDoubleTestCase testCase) -> {
                    try {
                        final String actualJson = testCase.write();
                        Assertions
                                .assertThat(actualJson)
                                .as(
                                        "integralPart=%d, fractionalPart=%d",
                                        testCase.integralPart, testCase.fractionalPart)
                                .isEqualTo(expectedJson);
                    } catch (final IOException error) {
                        throw new RuntimeException(error);
                    }
                });

    }

    @Test
    public void test_writeDouble_with_negative_fractionalPart() {
        Assertions
                .assertThatThrownBy(() -> new WriteDoubleTestCase(0, -1).write())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative fraction");
    }

}
