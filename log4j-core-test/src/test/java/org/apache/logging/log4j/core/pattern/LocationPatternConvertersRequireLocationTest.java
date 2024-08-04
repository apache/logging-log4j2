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
package org.apache.logging.log4j.core.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.logging.log4j.core.impl.LocationAware;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LocationPatternConvertersRequireLocationTest {

    /**
     * Reproduces <a href="https://github.com/apache/logging-log4j2/issues/2781">github issue #2781</a>.
     */
    @ParameterizedTest
    @CsvSource({"%L", "%l", "%F", "%C", "%M"})
    void testThatLocationDependentPatternConvertersIndicateLocationRequirement(final String converterKey)
            throws Exception {
        final PatternFormatter formatter = createSinglePatternFormatterFromConverterKey(converterKey);

        final LocationAware locationAwareFormatter = assertInstanceOf(LocationAware.class, formatter.getConverter());
        assertTrue(locationAwareFormatter.requiresLocation());
    }

    private static PatternFormatter createSinglePatternFormatterFromConverterKey(final String converterKey) {
        final PatternParser parser = new PatternParser(PatternConverter.CATEGORY);
        final List<PatternFormatter> formatters = parser.parse(converterKey);
        assertEquals(1, formatters.size());
        final PatternFormatter formatter = formatters.get(0);
        return formatter;
    }
}
