/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.log4j.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test reconfiguring with an XML configuration.
 */
public class PropertiesReconfigurationTest {

    private static final String CONFIG = "target/test-classes/log4j1-file.properties";
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    private CountDownLatch toggle = new CountDownLatch(1);

    @Test
    public void testReconfiguration() throws Exception {
        System.setProperty(Log4j1Configuration.MONITOR_INTERVAL, "1");
        File file = new File(CONFIG);
        assertNotNull("No Config file", file);
        long configMillis = file.lastModified();
        assertTrue("Unable to modified file time", file.setLastModified(configMillis - FIVE_MINUTES));
        LoggerContext context = configure(file);
        Logger logger = LogManager.getLogger("test");
        logger.info("Hello");
        Configuration original = context.getConfiguration();
        TestListener listener = new TestListener();
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
        Configuration updated = context.getConfiguration();
        assertTrue("Configurations are the same", original != updated);
    }

    private class TestListener implements ConfigurationListener {

        public synchronized void onChange(final Reconfigurable reconfigurable) {
            toggle.countDown();
        }

    }

    private LoggerContext configure(File configFile) throws Exception {
        InputStream is = new FileInputStream(configFile);
        ConfigurationSource source = new ConfigurationSource(is, configFile);
        LoggerContextFactory factory = org.apache.logging.log4j.LogManager.getFactory();
        LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Configuration configuration = new PropertiesConfigurationFactory().getConfiguration(context, source);
        assertNotNull("No configuration created", configuration);
        Configurator.reconfigure(configuration);
        return context;
    }
}
