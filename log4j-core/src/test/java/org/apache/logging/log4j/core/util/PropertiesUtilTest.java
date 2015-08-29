package org.apache.logging.log4j.core.util;

import org.junit.Test;

import java.io.FileInputStream;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class PropertiesUtilTest {

    @Test
    public void testSubset() throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream("target/test-classes/log4j2-properties.properties"));
        Properties subset = PropertiesUtil.extractSubset(props, "appender.Stdout.filter.marker");
        assertNotNull("No subset returned", subset);
        assertTrue("Incorrect number of items. Expected 4, actual " + subset.size(), subset.size() == 4);
        assertTrue("Missing propertu", subset.containsKey("type"));
    }
}
