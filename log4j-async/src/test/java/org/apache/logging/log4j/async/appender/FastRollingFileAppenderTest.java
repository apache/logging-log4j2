package org.apache.logging.log4j.async.appender;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.config.XMLConfigurationFactory;
import org.junit.BeforeClass;
import org.junit.Test;

public class FastRollingFileAppenderTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "FastRollingFileAppenderTest.xml");
    }

    @Test
    public void testFlushAtEndOfBatch() throws Exception {
        File f = new File("FastRollingFileAppenderTest.log");
        // System.out.println(f.getAbsolutePath());
        f.delete();
        Logger log = LogManager.getLogger("com.foo.Bar");
        String msg = "Message flushed with immediate flush=false";
        log.info(msg);
        ((LifeCycle) LogManager.getContext()).stop(); // stop async thread

        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line1 = reader.readLine();
        reader.close();
        f.delete();
        assertNotNull("line1", line1);
        assertTrue("line1 correct", line1.contains(msg));

        String location = "testFlushAtEndOfBatch";
        assertTrue("no location", !line1.contains(location));
    }
}
