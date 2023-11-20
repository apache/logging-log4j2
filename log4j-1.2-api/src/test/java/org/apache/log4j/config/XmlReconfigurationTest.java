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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
        final File file = new File(CONFIG);
        assertNotNull(file, "No Config file");
        final long configMillis = file.lastModified();
        Assertions.assertTrue(file.setLastModified(configMillis - FIVE_MINUTES), "Unable to modified file time");
        final LoggerContext context = configure(file);
        final Logger logger = LogManager.getLogger("test");
        logger.info("Hello");
        final Configuration original = context.getConfiguration();
        original.addListener(ignored -> toggle.countDown());
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
        assertNotSame(original, updated, "Configurations are the same");
    }

    private LoggerContext configure(final File configFile) throws Exception {
        final InputStream is = new FileInputStream(configFile);
        final ConfigurationSource source = new ConfigurationSource(is, configFile);
        final LoggerContext context = (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
        final Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
        assertNotNull(configuration, "No configuration created");
        Configurator.reconfigure(configuration);
        return context;
    }
}
