package org.apache.logging.log4j.core;

import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.ClassRule;
import org.junit.Test;

/**
 *
 */
public class DeadlockTest {

    private static final String CONFIG = "log4j-deadlock.xml";
    @ClassRule
    public static InitialLoggerContext context = new InitialLoggerContext(CONFIG);

    @Test
    public void deadlockOnReconfigure() {
        context.getContext().reconfigure();
    }
}
