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
package org.apache.logging.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import org.apache.logging.log4j.MarkerManager.Log4jMarker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link MarkerMixIn}.
 *
 * This class is in this package to let {@link Log4jMarker} have the least visibility.
 */
abstract class MarkerMixInTest {

    private ObjectReader reader;
    private ObjectWriter writer;

    @BeforeEach
    void setUp() {
        final ObjectMapper log4jObjectMapper = newObjectMapper();
        writer = log4jObjectMapper.writer();
        reader = log4jObjectMapper.readerFor(Log4jMarker.class);
        MarkerManager.clear();
    }

    protected abstract ObjectMapper newObjectMapper();

    @Test
    void testNameOnly() throws IOException {
        final Marker expected = MarkerManager.getMarker("A");
        final String str = writeValueAsString(expected);
        assertFalse(str.contains("parents"));
        final Marker actual = reader.readValue(str);
        assertEquals(expected, actual);
    }

    @Test
    void testOneParent() throws IOException {
        final Marker expected = MarkerManager.getMarker("A");
        final Marker parent = MarkerManager.getMarker("PARENT_MARKER");
        expected.addParents(parent);
        final String str = writeValueAsString(expected);
        assertTrue(str.contains("PARENT_MARKER"));
        final Marker actual = reader.readValue(str);
        assertEquals(expected, actual);
    }

    /**
     * @param expected
     * @return
     * @throws JsonProcessingException
     */
    private String writeValueAsString(final Marker expected) throws JsonProcessingException {
        final String str = writer.writeValueAsString(expected);
        // System.out.println(str);
        return str;
    }

    @Test
    void testTwoParents() throws IOException {
        final Marker expected = MarkerManager.getMarker("A");
        final Marker parent1 = MarkerManager.getMarker("PARENT_MARKER1");
        final Marker parent2 = MarkerManager.getMarker("PARENT_MARKER2");
        expected.addParents(parent1);
        expected.addParents(parent2);
        final String str = writeValueAsString(expected);
        assertTrue(str.contains("PARENT_MARKER1"));
        assertTrue(str.contains("PARENT_MARKER2"));
        final Marker actual = reader.readValue(str);
        assertEquals(expected, actual);
    }
}
