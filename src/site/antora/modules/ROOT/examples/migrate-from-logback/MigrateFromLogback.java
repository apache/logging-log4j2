package example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class MigrateFromLogback {
    private static Logger logger = LogManager.getLogger();

    doLogWrong() {
        try {
            // do something
            // tag::wrong[]
        } catch (Exception e) {
            logger.error("The foo process exited with an error: {}", e);
        }
        // end::wrong[]
    }

    doLogRight() {
        try {
            // do something
            // tag::right[]
        } catch (Exception e) {
            logger.error("The foo process exited with an error.", e);
        }
        // end::right[]
    }
}