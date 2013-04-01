package org.apache.logging.log4j.async;

import static org.junit.Assert.*;

import org.junit.Test;

public class SystemClockTest {

    @Test
    public void testLessThan2Millis() {
        long millis1 = new SystemClock().currentTimeMillis();
        long sysMillis = System.currentTimeMillis();
        
        long diff = sysMillis - millis1;
        
        assertTrue("diff too large: " + diff, diff <= 1);
    }

    @Test
    public void testAfterWaitStillLessThan2Millis() throws Exception {
        Thread.sleep(100);
        long millis1 = new SystemClock().currentTimeMillis();
        long sysMillis = System.currentTimeMillis();
        
        long diff = sysMillis - millis1;
        
        assertTrue("diff too large: " + diff, diff <= 1);
    }

}
