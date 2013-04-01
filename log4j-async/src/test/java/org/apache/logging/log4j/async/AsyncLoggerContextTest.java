package org.apache.logging.log4j.async;

import static org.junit.Assert.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Test;

public class AsyncLoggerContextTest {

    @Test
    public void testNewInstanceReturnsAsyncLogger() {
        Logger logger = new AsyncLoggerContext("a").newInstance(
                new LoggerContext("a"), "a", null);
        assertTrue(logger instanceof AsyncLogger);
        
        ((LifeCycle) LogManager.getContext()).stop(); // stop async thread
    }
}
