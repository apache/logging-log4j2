package org.apache.logging.log4j.core.jackson;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Tests {@link LevelMixIn}.
 */
public class LevelMixInTest {

    static class Fixture {
        @JsonProperty
        private final Level level = Level.DEBUG;

        @Override
        public boolean equals(Object obj) {
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
    private Log4jJsonObjectMapper log4jObjectMapper;

    private ObjectReader reader;

    private ObjectWriter writer;
    
    @Before
    public void setUp() {
        log4jObjectMapper = new Log4jJsonObjectMapper();
        writer = log4jObjectMapper.writer();
        reader = log4jObjectMapper.reader(Level.class);
    }

    @Test
    public void testContainer() throws IOException {
        final Fixture expected = new Fixture();
        final String str = writer.writeValueAsString(expected);
        Assert.assertTrue(str.contains("DEBUG"));
        final ObjectReader fixtureReader = log4jObjectMapper.reader(Fixture.class);
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
