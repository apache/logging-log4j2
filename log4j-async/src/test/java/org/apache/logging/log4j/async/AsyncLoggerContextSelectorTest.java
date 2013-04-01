package org.apache.logging.log4j.async;

import static org.junit.Assert.*;

import java.util.List;

import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Test;

public class AsyncLoggerContextSelectorTest {

    @Test
    public void testContextReturnsAsyncLoggerContext() {
        AsyncLoggerContextSelector selector = new AsyncLoggerContextSelector();
        LoggerContext context = selector.getContext(null, null, false);
        
        assertTrue(context instanceof AsyncLoggerContext);
    }

    @Test
    public void testContext2ReturnsAsyncLoggerContext() {
        AsyncLoggerContextSelector selector = new AsyncLoggerContextSelector();
        LoggerContext context = selector.getContext(null, null, false, null);
        
        assertTrue(context instanceof AsyncLoggerContext);
    }

    @Test
    public void testLoggerContextsReturnsAsyncLoggerContext() {
        AsyncLoggerContextSelector selector = new AsyncLoggerContextSelector();
        List<LoggerContext> list = selector.getLoggerContexts();
        
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof AsyncLoggerContext);
    }

}
