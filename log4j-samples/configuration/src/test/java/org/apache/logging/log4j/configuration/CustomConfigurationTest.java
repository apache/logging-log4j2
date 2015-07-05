package org.apache.logging.log4j.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 *
 */
public class CustomConfigurationTest {
    private Logger logger = LogManager.getLogger(CustomConfiguration.class);

    @Test
    public void testLogging() {
        logger.error("This is a test");
    }
}
