package org.apache.logging.log4j.core.lookup;

import org.junit.Test;
import static org.junit.Assert.*;

public class CustomLookupTest {

    @Test
    public void testCustomLookup() {

        CustomLookup customLookup = new CustomLookup();

        assertEquals("this is an anole test", customLookup.lookup("any"));
        assertEquals("this is an anole test", customLookup.lookup(null, "any"));

    }
}
