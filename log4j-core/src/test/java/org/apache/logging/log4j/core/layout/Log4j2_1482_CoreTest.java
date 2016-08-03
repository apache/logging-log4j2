package org.apache.logging.log4j.core.layout;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log4j2_1482_CoreTest extends Log4j2_1482_Test {

    @Override
    protected void log(int runNumber) {
        if (runNumber == 2) {
            // System.out.println("Set a breakpoint here.");
        }
        final Logger logger = LogManager.getLogger("auditcsvfile");
        final int val1 = 9, val2 = 11, val3 = 12;
        logger.info("Info Message!", val1, val2, val3);
        logger.info("Info Message!", val1, val2, val3);
        logger.info("Info Message!", val1, val2, val3);
    }

}
