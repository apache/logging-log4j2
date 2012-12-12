package org.apache.logging.log4j;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

public class FormatterLoggerManualExample {

    private static class User {
        Calendar getBirthdayCalendar() {
            return new GregorianCalendar(1995, Calendar.MAY, 23);
        }

        String getName() {
            return "John Smith";
        }
    }

    public static Logger logger = LogManager.getFormatterLogger("Foo");

    /**
     * @param args
     */
    public static void main(final String[] args) {
        final LoggerContext ctx = Configurator.initialize(FormatterLoggerManualExample.class.getName(), null,
                "target/test-classes/log4j2-console.xml");
        try {
            final User user = new User();
            logger.debug("User %s with birthday %s", user.getName(), user.getBirthdayCalendar());
            logger.debug("User %1$s with birthday %2$tm %2$te, %2$tY", user.getName(), user.getBirthdayCalendar());
            logger.debug("Integer.MAX_VALUE = %,d", Integer.MAX_VALUE);
            logger.debug("Long.MAX_VALUE = %,d", Long.MAX_VALUE);
        } finally {
            Configurator.shutdown(ctx);
        }

    }

}
