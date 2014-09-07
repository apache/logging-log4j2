package org.apache.logging.log4j.jul;

import java.util.Arrays;
import java.util.Collection;

import org.apache.logging.log4j.Level;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class LevelsTest {

    private final java.util.logging.Level level;
    private final Level expectedLevel;

    public LevelsTest(final java.util.logging.Level level, final Level expectedLevel) {
        this.level = level;
        this.expectedLevel = expectedLevel;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
            new Object[][]{
                {CustomJdkLevel.TEST, Level.INFO},
                {CustomJdkLevel.DEFCON_2, Level.ERROR},
                {CustomJdkLevel.DEFCON_1, Level.FATAL},
                {java.util.logging.Level.OFF, Level.OFF},
                {java.util.logging.Level.ALL, Level.ALL},
                {java.util.logging.Level.SEVERE, Level.ERROR},
                {java.util.logging.Level.WARNING, Level.WARN},
                {java.util.logging.Level.INFO, Level.INFO},
                {java.util.logging.Level.CONFIG, Level.INFO},
                {java.util.logging.Level.FINE, Level.DEBUG},
                {java.util.logging.Level.FINER, Level.DEBUG},
                {java.util.logging.Level.FINEST, Level.TRACE}
            }
        );
    }

    @Test
    public void testToLevel() throws Exception {
        final Level actualLevel = Levels.toLevel(level);
        assertEquals(expectedLevel, actualLevel);
    }
}