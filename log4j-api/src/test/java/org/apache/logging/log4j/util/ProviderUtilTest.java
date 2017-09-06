package org.apache.logging.log4j.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.TestLoggerContext;
import org.apache.logging.log4j.spi.LoggerContext;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProviderUtilTest {

    @Test
    public void complexTest() throws Exception {
        File file = new File("target/classes");
        ClassLoader classLoader = new URLClassLoader(new URL[] {file.toURI().toURL()});
        Worker worker = new Worker();
        worker.setContextClassLoader(classLoader);
        worker.start();
        worker.join();
        assertTrue("Incorrect LoggerContext", worker.context instanceof TestLoggerContext);
    }

    private class Worker extends Thread {
        LoggerContext context = null;

        public void run() {
            context = LogManager.getContext(false);
        }
    }
}
