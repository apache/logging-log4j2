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

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test reconfiguring with an XML configuration.
 */
@Tag("sleepy")
public class XmlReconfigurationTest {

    private static final String CONFIG = "target/test-classes/log4j1-file.xml";
    private static final long FIVE_MINUTES = 5 * 60 * 1000;

    private final CountDownLatch toggle = new CountDownLatch(1);

    @Test
    public void testReconfiguration() throws Exception {
        System.setProperty(Log4j1Configuration.MONITOR_INTERVAL, "1");
        File file = new File(CONFIG);
        assertNotNull(file, "No Config file");
        long configMillis = file.lastModified();
        Assertions.assertTrue(file.setLastModified(configMillis - FIVE_MINUTES), "Unable to modified file time");
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
        assertNotSame(original, updated, "Configurations are the same");
    }

    private class TestListener implements ConfigurationListener {

        public synchronized void onChange(final Reconfigurable reconfigurable) {
            toggle.countDown();
        }

    }

    private LoggerContext configure(File configFile) throws Exception {
        InputStream is = new FileInputStream(configFile);
        ConfigurationSource source = new ConfigurationSource(is, configFile);
        LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        assertNotNull(configuration, "No configuration created");
        Configurator.reconfigure(configuration);
        return context;
    }
}
