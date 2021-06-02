package liquibase.ext.logging.log4j2;

import liquibase.logging.Logger;
import liquibase.logging.core.AbstractLogService;

import java.util.HashMap;
import java.util.Map;

public class Log4j2LoggerService extends AbstractLogService {

    private static Map<Class, Log4j2Logger> loggers = new HashMap<>();

    @Override
    public int getPriority() {
        return PRIORITY_SPECIALIZED;
    }

    @Override
    public Logger getLog(Class aClass) {
        Log4j2Logger logger = loggers.get(aClass);
        if (logger == null) {
            logger = new Log4j2Logger(aClass, this.filter);
            loggers.put(aClass, logger);
        }

        return logger;
    }
}
