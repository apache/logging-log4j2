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
public class ConsoleAppenderAnsiMessagesMain {

    private static final Logger LOG = LogManager.getLogger(ConsoleAppenderAnsiMessagesMain.class);

    public static void main(String[] args) {
        LoggerContext ctx = Configurator.initialize(ConsoleAppenderAnsiMessagesMain.class.getName(), null,
                "target/test-classes/log4j2-console.xml");
        try {
            LOG.fatal("\u001b[1;35mFatal message.\u001b[0m");
            LOG.error("\u001b[1;31mError message.\u001b[0m");
            LOG.warn("\u001b[0;33mWarning message.\u001b[0m");
            LOG.info("\u001b[0;32mInformation message.\u001b[0m");
            LOG.debug("\u001b[0;36mDebug message.\u001b[0m");
            LOG.trace("\u001b[0;30mTrace message.\u001b[0m");
            LOG.error("\u001b[1;31mError message.\u001b[0m", new IOException("test"));
        } finally {
            Configurator.shutdown(ctx);
        }
    }

}
