package org.apache.logging.log4j.core.config;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.core.util.CronExpression;

public class CronSchedulerNpeTest {

    @Test
    public void testCronSchedulerDoesNotThrowNpe() throws Exception {
        ConfigurationScheduler scheduler = new ConfigurationScheduler();
        scheduler.incrementScheduledItems(); // ensure a pool exists
        scheduler.start();

        // fire every second
        CronExpression cron = new CronExpression("* * * * * ?");
        AtomicBoolean ran = new AtomicBoolean(false);

        scheduler.scheduleWithCron(cron, () -> ran.set(true));

        // wait up to 2 seconds for the first fire; if an NPE happens, the test will fail
        Thread.sleep(2000L);

        scheduler.stop(0, TimeUnit.MILLISECONDS);

        assertTrue("cron task should have executed at least once", ran.get());
    }
}
