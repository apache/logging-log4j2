package org.apache.logging.log4j.core.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.AbstractSerializationTest;
import org.junit.runners.Parameterized.Parameters;

public class KeyValuePairSerializationTest extends AbstractSerializationTest {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { new KeyValuePair("testuser", "DEBUG") },                
                { new KeyValuePair("JohnDoe", "warn") } });
    }

    public KeyValuePairSerializationTest(Serializable serializable) {
        super(serializable);
    }

}
