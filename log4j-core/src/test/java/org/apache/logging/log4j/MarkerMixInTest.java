package org.apache.logging.log4j;

import java.io.IOException;

import org.apache.logging.log4j.MarkerManager.Log4jMarker;
import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * Tests {@link MarkerMixIn}.
 * 
 * This class is in this package to let {@link Log4jMarker} have the least visibility.
 */
public class MarkerMixInTest {

    private ObjectReader reader;
    private ObjectWriter writer;

    @Before
    public void setUp() {
        final Log4jJsonObjectMapper log4jObjectMapper = new Log4jJsonObjectMapper();
        writer = log4jObjectMapper.writer();
        reader = log4jObjectMapper.reader(Log4jMarker.class);
        MarkerManager.clear();
    }

    @Test
    public void testNameOnly() throws IOException {
        final Marker expected = MarkerManager.getMarker("A");
        final String str = writeValueAsString(expected);
        Assert.assertFalse(str.contains("parents"));
        final Marker actual = reader.readValue(str);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testOneParent() throws IOException {
        final Marker expected = MarkerManager.getMarker("A");
        final Marker parent = MarkerManager.getMarker("PARENT_MARKER");
        expected.addParents(parent);
        final String str = writeValueAsString(expected);
        Assert.assertTrue(str.contains("PARENT_MARKER"));
        final Marker actual = reader.readValue(str);
        Assert.assertEquals(expected, actual);
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
    public void testTwoParents() throws IOException {
        final Marker expected = MarkerManager.getMarker("A");
        final Marker parent1 = MarkerManager.getMarker("PARENT_MARKER1");
        final Marker parent2 = MarkerManager.getMarker("PARENT_MARKER2");
        expected.addParents(parent1);
        expected.addParents(parent2);
        final String str = writeValueAsString(expected);
        Assert.assertTrue(str.contains("PARENT_MARKER1"));
        Assert.assertTrue(str.contains("PARENT_MARKER2"));
        final Marker actual = reader.readValue(str);
        Assert.assertEquals(expected, actual);
    }
}
