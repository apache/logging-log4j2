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
package org.apache.logging.log4j.core.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.IOException;
import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link LevelMixIn}.
 */
public abstract class LevelMixInTest {

    static class Fixture {
        @JsonProperty
        private final Level level = Level.DEBUG;

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Fixture other = (Fixture) obj;
            if (!Objects.equals(this.level, other.level)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return 31 + Objects.hashCode(level);
        }
    }

    private ObjectMapper log4jObjectMapper;

    private ObjectReader reader;

    private ObjectWriter writer;

    protected abstract ObjectMapper newObjectMapper();

    @BeforeEach
    public void setUp() {
        log4jObjectMapper = newObjectMapper();
        writer = log4jObjectMapper.writer();
        reader = log4jObjectMapper.readerFor(Level.class);
    }

    @Test
    public void testContainer() throws IOException {
        final Fixture expected = new Fixture();
        final String str = writer.writeValueAsString(expected);
        assertTrue(str.contains("DEBUG"));
        final ObjectReader fixtureReader = log4jObjectMapper.readerFor(Fixture.class);
        final Fixture actual = fixtureReader.readValue(str);
        assertEquals(expected, actual);
    }

    @Test
    public void testNameOnly() throws IOException {
        final Level expected = Level.getLevel("DEBUG");
        final String str = writer.writeValueAsString(expected);
        assertTrue(str.contains("DEBUG"));
        final Level actual = reader.readValue(str);
        assertEquals(expected, actual);
    }
}
