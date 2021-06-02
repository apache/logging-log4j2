package liquibase.ext.logging.log4j2;

import liquibase.logging.LogMessageFilter;
import liquibase.logging.core.AbstractLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.ExtendedLogger;

import java.util.logging.Level;

public class Log4j2Logger extends AbstractLogger {

    private final ExtendedLogger logger;
    private final String FQCN;

    public Log4j2Logger(Class aClass, LogMessageFilter filter) {
        super(filter);
        logger = LogManager.getContext(false).getLogger(aClass);
        FQCN = getClass().getName();
    }

    @Override
    public void log(Level level, String message, Throwable throwable) {
        org.apache.logging.log4j.Level log4jLevel;
        if (level.equals(Level.ALL)) {
            log4jLevel = org.apache.logging.log4j.Level.ALL;
        } else if (level.equals(Level.SEVERE)) {
            log4jLevel = org.apache.logging.log4j.Level.FATAL;
        } else if (level.equals(Level.WARNING)) {
            log4jLevel = org.apache.logging.log4j.Level.WARN;
        } else if (level.equals(Level.INFO)) {
            log4jLevel = org.apache.logging.log4j.Level.INFO;
        } else if (level.equals(Level.FINE)) {
            log4jLevel = org.apache.logging.log4j.Level.DEBUG;
        } else {
            log4jLevel = org.apache.logging.log4j.Level.TRACE;
        }

        logger.logIfEnabled(FQCN, log4jLevel, null, message, throwable);
    }

    @Override
    public void close() throws Exception {

    }
}
