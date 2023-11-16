/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.junit.Test;

/**
 * Test reconfiguring with an XML configuration.
 */
public class XmlReconfigurationTest {

    private static final String CONFIG = "target/test-classes/log4j1-file.xml";
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    private final CountDownLatch toggle = new CountDownLatch(1);

    @Test
    public void testReconfiguration() throws Exception {
        System.setProperty(Log4j1Configuration.MONITOR_INTERVAL, "1");
        final File file = new File(CONFIG);
        assertNotNull("No Config file", file);
        final long configMillis = file.lastModified();
        assertTrue("Unable to modified file time", file.setLastModified(configMillis - FIVE_MINUTES));
        final LoggerContext context = TestConfigurator.configure(file.toString());
        final Logger logger = LogManager.getLogger("test");
        logger.info("Hello");
        final Configuration original = context.getConfiguration();
        final TestListener listener = new TestListener();
        original.addListener(listener);
        file.setLastModified(System.currentTimeMillis());
        try {
            if (!toggle.await(3, TimeUnit.SECONDS)) {
                fail("Reconfiguration timed out");
            }
            // Allow reconfiguration to complete.
            Thread.sleep(500);
        } catch (InterruptedException ie) {
            fail("Reconfiguration interupted");
        }
        final Configuration updated = context.getConfiguration();
        assertTrue("Configurations are the same", original != updated);
    }

    private class TestListener implements ConfigurationListener {

        public synchronized void onChange(final Reconfigurable reconfigurable) {
            toggle.countDown();
        }
    }
}
