package org.apache.logging.log4j.async;

import static org.junit.Assert.*;

import org.junit.Test;

public class CachedClockTest {

    @Test
    public void testLessThan17Millis() {
        long millis1 = CachedClock.instance().currentTimeMillis();
        long sysMillis = System.currentTimeMillis();
        
        long diff = sysMillis - millis1;
        
        assertTrue("diff too large: " + diff, diff <= 16);
    }

    @Test
    public void testAfterWaitStillLessThan17Millis() throws Exception {
        Thread.sleep(100);
        long millis1 = CachedClock.instance().currentTimeMillis();
        long sysMillis = System.currentTimeMillis();
        
        long diff = sysMillis - millis1;
        
        assertTrue("diff too large: " + diff, diff <= 16);
    }

}
