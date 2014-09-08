package org.apache.logging.log4j.core.util;

import java.io.Serializable;

import org.apache.logging.log4j.AbstractSerializationTest;

public class KeyValuePairTest extends AbstractSerializationTest {

    @Override
    protected Serializable[] createSerializationTestFixtures() {
        return new KeyValuePair[] {
                new KeyValuePair("testuser", "DEBUG"),
                new KeyValuePair("JohnDoe", "warn") };
    }

}
