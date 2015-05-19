package org.apache.logging.log4j.core.lookup;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests {@link MarkerLookup} with a configuration file.
 * 
 * @since 2.4
 */
public class MarkerLookupConfigTest {

    @ClassRule
    public static InitialLoggerContext context = new InitialLoggerContext("log4j-marker-lookup.yaml");
    public static final Marker PAYLOAD = MarkerManager.getMarker("PAYLOAD");
    private static final String PAYLOAD_LOG = "Message in payload.log";

    public static final Marker PERFORMANCE = MarkerManager.getMarker("PERFORMANCE");

    private static final String PERFORMANCE_LOG = "Message in performance.log";
    public static final Marker SQL = MarkerManager.getMarker("SQL");
    private static final String SQL_LOG = "Message in sql.log";

    @Test
    public void test() throws IOException {
        final Logger logger = LogManager.getLogger();
        logger.info(SQL, SQL_LOG);
        logger.info(PAYLOAD, PAYLOAD_LOG);
        logger.info(PERFORMANCE, PERFORMANCE_LOG);
        {
            final String log = FileUtils.readFileToString(new File("target/logs/sql.log"));
            Assert.assertTrue(log.contains(SQL_LOG));
            Assert.assertFalse(log.contains(PAYLOAD_LOG));
            Assert.assertFalse(log.contains(PERFORMANCE_LOG));
        }
        {
            final String log = FileUtils.readFileToString(new File("target/logs/payload.log"));
            Assert.assertFalse(log.contains(SQL_LOG));
            Assert.assertTrue(log.contains(PAYLOAD_LOG));
            Assert.assertFalse(log.contains(PERFORMANCE_LOG));
        }
        {
            final String log = FileUtils.readFileToString(new File("target/logs/performance.log"));
            Assert.assertFalse(log.contains(SQL_LOG));
            Assert.assertFalse(log.contains(PAYLOAD_LOG));
            Assert.assertTrue(log.contains(PERFORMANCE_LOG));
        }
    }
}
