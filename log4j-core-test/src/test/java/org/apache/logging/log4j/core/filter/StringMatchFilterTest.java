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
package org.apache.logging.log4j.core.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link StringMatchFilter}.
 */
class StringMatchFilterTest {

    /**
     * Test that if no match-string is set on the builder, the '{@link StringMatchFilter.Builder#build()}' returns
     * {@code null}.
     */
    @Test
    void testFilterBuilderFailsWithNullText() {
        assertNull(StringMatchFilter.newBuilder().build());
    }

    /**
     * Test that if a {@code null} string is set as a match-pattern, an {@code IllegalArgumentExeption} is thrown.
     */
    @Test
    void testFilterBuilderFailsWithExceptionOnNullText() {
        assertThrows(IllegalArgumentException.class, () -> StringMatchFilter.newBuilder()
                .setText(null));
    }

    /**
     * Test that if an empty ({@code ""}) string is set as a match-pattern, an {@code IllegalArgumentException} is thrown.
     */
    @Test
    void testFilterBuilderFailsWithExceptionOnEmptyText() {
        assertThrows(IllegalArgumentException.class, () -> StringMatchFilter.newBuilder()
                .setText(""));
    }

    /**
     * Test that if a {@link StringMatchFilter} is specified with a 'text' attribute it is correctly instantiated.
     *
     * @param configuration the configuration
     */
    @Test
    @LoggerContextSource("log4j2-stringmatchfilter-3153-ok.xml")
    void testConfigurationWithTextPOS(final Configuration configuration) {
        final Filter filter = configuration.getFilter();
        assertNotNull(filter, "The filter should not be null.");
        assertInstanceOf(
                StringMatchFilter.class, filter, "Expected a StringMatchFilter, but got: " + filter.getClass());
        assertEquals("FooBar", filter.toString());
    }

    /**
     * Test that if a {@link StringMatchFilter} is specified without a 'text' attribute it is not instantiated.
     *
     * @param configuration the configuration
     */
    @Test
    @LoggerContextSource("log4j2-stringmatchfilter-3153-nok.xml")
    void testConfigurationWithTextNEG(final Configuration configuration) {
        final Filter filter = configuration.getFilter();
        assertNull(filter, "The filter should be null.");
    }
}
