package org.apache.logging.log4j.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConstantsTest {

    @Test
    public void testJdkVersionDetection() {
        assertEquals(1, Constants.getMajorVersion("1.1.2"));
        assertEquals(8, Constants.getMajorVersion("1.8.2"));
        assertEquals(9, Constants.getMajorVersion("9.1.1"));
        assertEquals(11, Constants.getMajorVersion("11.1.1"));
    }
}
