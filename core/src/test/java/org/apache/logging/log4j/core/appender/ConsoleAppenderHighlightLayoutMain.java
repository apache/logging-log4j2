package org.apache.logging.log4j.core.appender;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

/**
 * Shows how to use ANSI escape codes to color messages. Each message is printed to the console in color, but the rest of the log entry
 * (time stamp for example) is in the default color for that console.
 */
public class ConsoleAppenderHighlightLayoutMain {

    private static final Logger LOG = LogManager.getLogger(ConsoleAppenderHighlightLayoutMain.class);

    public static void main(String[] args) {
        LoggerContext ctx = Configurator.initialize(ConsoleAppenderAnsiMessagesMain.class.getName(), null,
                "target/test-classes/log4j2-console-highlight.xml");
        try {
            LOG.fatal("Fatal message.");
            LOG.error("Error message.");
            LOG.warn("Warning message.");
            LOG.info("Information message.");
            LOG.debug("Debug message.");
            LOG.trace("Trace message.");
            LOG.error("Error message.", new IOException("test"));
        } finally {
            Configurator.shutdown(ctx);
        }
    }

}
