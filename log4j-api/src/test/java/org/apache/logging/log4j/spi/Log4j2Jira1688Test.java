package org.apache.logging.log4j.spi;

import java.util.Arrays;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Tests LOG4J2-1688 Multiple loggings of arguments are setting these arguments to null.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class Log4j2Jira1688Test {

    private static Object[] createArray(final int size) {
        final Object[] args = new Object[size];
        for (int i = 0; i < args.length; i++) {
            args[i] = i;
        }
        return args;
    }
    
    @Test
    public void testLog4j2Only() {
        final org.apache.logging.log4j.Logger log4JLogger = LogManager.getLogger(this.getClass());
        final int limit = 37;
        final Object[] args = createArray(limit);
        final Object[] originalArgs = Arrays.copyOf(args, args.length);

        ((ExtendedLogger)log4JLogger).logIfEnabled("test", Level.ERROR, null, "test {}", args);
        //System.out.println("args " + Arrays.toString(args));
        Assert.assertArrayEquals(originalArgs, args);
        
        ((ExtendedLogger)log4JLogger).logIfEnabled("test", Level.ERROR, null, "test {}", args);
        //System.out.println("args " + Arrays.toString(args));
        Assert.assertArrayEquals(originalArgs, args);
    }

}