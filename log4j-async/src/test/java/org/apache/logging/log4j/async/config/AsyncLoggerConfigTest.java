package org.apache.logging.log4j.async.config;

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

public class AsyncLoggerConfigTest {

    @BeforeClass
    public static void beforeClass() {
        System.setProperty(XMLConfigurationFactory.CONFIGURATION_FILE_PROPERTY,
                "AsyncLoggerConfigTest.xml");
    }

    @Test
    public void testAdditivity() throws Exception {
        File f = new File("AsyncLoggerConfigTest.log");
        // System.out.println(f.getAbsolutePath());
        f.delete();
        Logger log = LogManager.getLogger("com.foo.Bar");
        String msg = "Additive logging: 2 for the price of 1!";
        log.info(msg);
        ((LifeCycle) LogManager.getContext()).stop(); // stop async thread

        BufferedReader reader = new BufferedReader(new FileReader(f));
        String line1 = reader.readLine();
        String line2 = reader.readLine();
        reader.close();
        f.delete();
        assertNotNull("line1", line1);
        assertNotNull("line2", line2);
        assertTrue("line1 correct", line1.contains(msg));
        assertTrue("line2 correct", line2.contains(msg));

        String location = "testAdditivity";
        assertTrue("location",
                line1.contains(location) || line2.contains(location));
    }

}
