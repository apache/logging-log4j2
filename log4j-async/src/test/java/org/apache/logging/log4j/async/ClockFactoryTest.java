package org.apache.logging.log4j.async;

import static org.junit.Assert.*;

import org.junit.Test;

public class ClockFactoryTest {

    @Test
    public void testDefaultIsSystemClock() {
        System.clearProperty(ClockFactory.PROPERTY_NAME);
        assertEquals(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifySystemClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "SystemClock");
        assertEquals(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifySystemClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, SystemClock.class.getName());
        assertEquals(SystemClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CachedClock");
        assertEquals(CachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CachedClock.class.getName());
        assertEquals(CachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCoarseCachedClockShort() {
        System.setProperty(ClockFactory.PROPERTY_NAME, "CoarseCachedClock");
        assertEquals(CoarseCachedClock.class, ClockFactory.getClock().getClass());
    }

    @Test
    public void testSpecifyCoarseCachedClockLong() {
        System.setProperty(ClockFactory.PROPERTY_NAME, CoarseCachedClock.class.getName());
        assertEquals(CoarseCachedClock.class, ClockFactory.getClock().getClass());
    }

    static class MyClock implements Clock {
        @Override
        public long currentTimeMillis() {
            return 42;
        }
    }
    
    @Test
    public void testCustomClock() {
        System.setProperty(ClockFactory.PROPERTY_NAME, MyClock.class.getName());
        assertEquals(MyClock.class, ClockFactory.getClock().getClass());
    }

}
