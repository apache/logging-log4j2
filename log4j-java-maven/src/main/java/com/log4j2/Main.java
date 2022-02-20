package com.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {

        logger.debug("Log4j 2 hello");
        // with Java 8, we can do this, no need to check the log level
        while (true)//test rolling file
            logger.debug("hello {}", () -> getValue());

    }

    static int getValue() {
        return 5;
    }

}
