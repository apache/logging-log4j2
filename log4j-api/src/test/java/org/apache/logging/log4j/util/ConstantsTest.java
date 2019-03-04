package org.apache.logging.log4j.util;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

import static org.junit.Assert.*;

public class ConstantsTest {

    @Test
    public void testJdkVersionDetection() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        assertEquals(1, Constants.getMajorVersion("1.1.2"));
        assertEquals(8, Constants.getMajorVersion("1.8.2"));
        assertEquals(9, Constants.getMajorVersion("9.1.1"));
        assertEquals(11, Constants.getMajorVersion("11.1.1"));
    }
}