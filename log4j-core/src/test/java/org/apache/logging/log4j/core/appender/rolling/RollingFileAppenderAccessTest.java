package org.apache.logging.log4j.core.appender.rolling;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Test;

public class RollingFileAppenderAccessTest {

    /**
     * Not a real test, just make sure we can compile access to the typed manager.
     * 
     * @throws IOException
     */
    @Test
    public void testAccessManager() throws IOException {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final File file = File.createTempFile("RollingFileAppenderAccessTest", ".tmp");
        file.deleteOnExit();
        final RollingFileAppender appender = RollingFileAppender.createAppender(file.getCanonicalPath(), "FilePattern",
                null, "Name", null, null, OnStartupTriggeringPolicy.createPolicy(), null, null, null, null, null, null,
                config);
        final RollingFileManager manager = appender.getManager();
        // Since the RolloverStrategy and TriggeringPolicy are immutable, we could also use generics to type their
        // access.
        manager.getRolloverStrategy();
        manager.getTriggeringPolicy();
    }
}
