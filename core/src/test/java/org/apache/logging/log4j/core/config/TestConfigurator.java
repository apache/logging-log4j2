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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestConfigurator {

    private static final String CONFIG_NAME = "ConfigTest";


    @Test
    public void testFromFile() throws Exception {
        LoggerContext ctx = Configurator.intitalize("Test1", null, "target/test-classes/log4j2-config.xml");
        Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        Map<String, Appender> map = config.getAppenders();
        assertNotNull("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + DefaultConfiguration.DEFAULT_NAME + " but found " +
            config.getName(), DefaultConfiguration.DEFAULT_NAME.equals(config.getName()));
    }

    @Test
    public void testFromClassPath() throws Exception {
        LoggerContext ctx = Configurator.intitalize("Test1", null, "log4j2-config.xml");
        Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        Map<String, Appender> map = config.getAppenders();
        assertNotNull("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + DefaultConfiguration.DEFAULT_NAME + " but found " +
            config.getName(), DefaultConfiguration.DEFAULT_NAME.equals(config.getName()));
    }

    @Test
    public void testByName() throws Exception {
        LoggerContext ctx = Configurator.intitalize("-config", null, null);
        Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        Map<String, Appender> map = config.getAppenders();
        assertNotNull("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + DefaultConfiguration.DEFAULT_NAME + " but found " +
            config.getName(), DefaultConfiguration.DEFAULT_NAME.equals(config.getName()));
    }

    @Test
    public void testReconfiguration() throws Exception {
        File file = new File("target/test-classes/log4j2-config.xml");
        file.setLastModified(System.currentTimeMillis() - 120000);
        LoggerContext ctx = Configurator.intitalize("Test1", null, "target/test-classes/log4j2-config.xml");
        Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertTrue("Incorrect Configuration. Expected " + CONFIG_NAME + " but found " + config.getName(),
            CONFIG_NAME.equals(config.getName()));
        Map<String, Appender> map = config.getAppenders();
        assertNotNull("No Appenders", map != null && map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));

        Thread.sleep(500);
        file.setLastModified(System.currentTimeMillis());
        for (int i = 0; i < 17; ++i) {
            logger.debug("Test message " + i);
        }
        Configuration newConfig = ctx.getConfiguration();
        assertTrue("Configuration not reset", newConfig != config);
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertTrue("Incorrect Configuration. Expected " + DefaultConfiguration.DEFAULT_NAME + " but found " +
            config.getName(), DefaultConfiguration.DEFAULT_NAME.equals(config.getName()));
    }
}
