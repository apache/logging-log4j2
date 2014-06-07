package org.apache.logging.log4j;

import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;

/**
 *
 */
public class LogRolloverTest {

    private static final String CONFIG = "src/test/resources/rollover-test.xml";


    public static void main(String[] args) throws Exception {
        File file = new File(CONFIG);
        Configurator.initialize("LogTest", LogRolloverTest.class.getClassLoader(), file.toURI());
        Logger logger = LogManager.getLogger("TestLogger");

        for (long i=0; ; i+=1) {
            logger.debug("Sequence: " + i);
            Thread.sleep(250);
        }
    }
}
