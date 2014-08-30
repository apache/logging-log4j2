package org.apache.logging.log4j.core.async;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests LOG4J2-807.
 */
public class AsyncRootReloadTest {

    private static final String ISSUE = "LOG4J2-807";
    private static final String ISSUE_CONFIG = ISSUE + ".xml";
    private static final String LOG = "target/" + ISSUE + ".log";
    private static final String RESOURCE = "classpath:" + ISSUE_CONFIG;

    @Rule
    public RuleChain rules = RuleChain.outerRule(new CleanFiles(LOG)).around(new InitialLoggerContext(RESOURCE));

    @Test
    public void testLog4j2_807() throws InterruptedException, URISyntaxException {
        URL url = AsyncRootReloadTest.class.getResource("/" + ISSUE_CONFIG);
        final File configFile = FileUtils.fileFromUri(url.toURI());

        Logger logger = LogManager.getLogger(AsyncRootReloadTest.class);
        logger.info("Log4j configured, will be reconfigured in aprox. 5 sec");

        configFile.setLastModified(System.currentTimeMillis());

        for (int i = 0; i < 10; i++) {
            Thread.sleep(1000);
            logger.info("Log4j waiting for reconfiguration");
        }
    }
}
