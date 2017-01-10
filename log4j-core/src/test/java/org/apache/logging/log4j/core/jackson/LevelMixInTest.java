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
package org.apache.logging.log4j.core.jackson;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.categories.Layouts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.experimental.categories.Category;

/**
 * Tests {@link LevelMixIn}.
 */
@Category(Layouts.Json.class)
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
            if (this.level == null) {
                if (other.level != null) {
                    return false;
                }
            } else if (!this.level.equals(other.level)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((this.level == null) ? 0 : this.level.hashCode());
            return result;
        }
    }
    private ObjectMapper log4jObjectMapper;

    private ObjectReader reader;

    private ObjectWriter writer;

    @Before
    public void setUp() {
        log4jObjectMapper = newObjectMapper();
        writer = log4jObjectMapper.writer();
        reader = log4jObjectMapper.readerFor(Level.class);
    }

    protected abstract ObjectMapper newObjectMapper();

    @Test
    public void testContainer() throws IOException {
        final Fixture expected = new Fixture();
        final String str = writer.writeValueAsString(expected);
        Assert.assertTrue(str.contains("DEBUG"));
        final ObjectReader fixtureReader = log4jObjectMapper.readerFor(Fixture.class);
        final Fixture actual = fixtureReader.readValue(str);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testNameOnly() throws IOException {
        final Level expected = Level.getLevel("DEBUG");
        final String str = writer.writeValueAsString(expected);
        Assert.assertTrue(str.contains("DEBUG"));
        final Level actual = reader.readValue(str);
        Assert.assertEquals(expected, actual);
    }
}
