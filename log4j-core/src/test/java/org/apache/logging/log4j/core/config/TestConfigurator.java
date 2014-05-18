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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 */
@RunWith(JUnit4.class)
public class TestConfigurator {

    private static final String CONFIG_NAME = "ConfigTest";

    private static final String FILESEP = System.getProperty("file.separator");


    private LoggerContext ctx = null;

    private static final String[] CHARS = new String[] {
        "aaaaaaaaaa",
        "bbbbbbbbbb",
        "cccccccccc",
        "dddddddddd",
        "eeeeeeeeee",
        "ffffffffff",
        "gggggggggg",
        "hhhhhhhhhh",
        "iiiiiiiiii",
        "jjjjjjjjjj",
        "kkkkkkkkkk",
        "llllllllll",
        "mmmmmmmmmm",
    };

    @After
    public void cleanup() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (ctx != null) {
            Configurator.shutdown(ctx);
            ctx = null;
        }
    }


    @Test
    public void testFromFile() throws Exception {
        ctx = Configurator.initialize("Test1", "target/test-classes/log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Unexpected Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testFromStream() throws Exception {
        final InputStream is = new FileInputStream("target/test-classes/log4j2-config.xml");
        final ConfigurationFactory.ConfigurationSource source =
            new ConfigurationFactory.ConfigurationSource(is, "target/test-classes/log4j2-config.xml");
        ctx = Configurator.initialize(null, source);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Unexpected Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testFromStreamNoId() throws Exception {
        final InputStream is = new FileInputStream("target/test-classes/log4j2-config.xml");
        final ConfigurationFactory.ConfigurationSource source =
            new ConfigurationFactory.ConfigurationSource(is);
        ctx = Configurator.initialize(null, source);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Unexpected Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testFromClassPath() throws Exception {
        ctx = Configurator.initialize("Test1", "log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Unexpected Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testFromClassPathProperty() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "classpath:log4j2-config.xml");
        ctx = Configurator.initialize("Test1", null);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Unexpected Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testFromClassPathWithClassPathPrefix() throws Exception {
        ctx = Configurator.initialize("Test1", "classpath:log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Incorrect Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testFromClassPathWithClassLoaderPrefix() throws Exception {
        ctx = Configurator.initialize("Test1", "classloader:log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Incorrect Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testByName() throws Exception {
        ctx = Configurator.initialize("-config", null);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Unexpected Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }

    @Test
    public void testReconfiguration() throws Exception {
        final File file = new File("target/test-classes/log4j2-config.xml");
        assertTrue("setLastModified should have succeeded.", file.setLastModified(System.currentTimeMillis() - 120000));
        ctx = Configurator.initialize("Test1", "target/test-classes/log4j2-config.xml");
        final Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        assertTrue("Wrong configuration", map.containsKey("List"));

        Thread.sleep(500);
        assertTrue("setLastModified should have succeeded.", file.setLastModified(System.currentTimeMillis()));
        for (int i = 0; i < 17; ++i) {
            logger.debug("Test message " + i);
        }
        final Configuration newConfig = ctx.getConfiguration();
        assertTrue("Configuration not reset", newConfig != config);
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertEquals("Unexpected Configuration.", NullConfiguration.NULL_NAME, config.getName());
    }


    @Test
    public void testEnvironment() throws Exception {
        ctx = Configurator.initialize("-config", null);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Incorrect Configuration.", CONFIG_NAME, config.getName());
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders map should not be null.", map);
        assertTrue("Appenders map should not be empty.", map.size() > 0);
        Appender app = null;
        for (final Map.Entry<String, Appender> entry: map.entrySet()) {
            if (entry.getKey().equals("List2")) {
                app = entry.getValue();
                break;
            }
        }
        assertNotNull("No ListAppender named List2", app);
        final Layout<? extends Serializable> layout = app.getLayout();
        assertNotNull("Appender List2 does not have a Layout", layout);
        assertTrue("Appender List2 is not configured with a PatternLayout", layout instanceof PatternLayout);
        final String pattern = ((PatternLayout) layout).getConversionPattern();
        assertNotNull("No conversion pattern for List2 PatternLayout", pattern);
        assertFalse("Environment variable was not substituted", pattern.startsWith("${env:PATH}"));
    }

    @Test
    public void testNoLoggers() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-loggers.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Unexpected Configuration.", DefaultConfiguration.DEFAULT_NAME, config.getName());
    }

    @Test
    public void testBadStatus() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-status.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Unexpected Configuration", "XMLConfigTest", config.getName());
        final LoggerConfig root = config.getLoggerConfig("");
        assertNotNull("No Root Logger", root);
        assertTrue("Expected error level, was " + root.getLevel(), Level.ERROR == root.getLevel());
    }

    @Test
    public void testBadFilterParam() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-badfilterparam.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Unexpected Configuration", "XMLConfigTest", config.getName());
        final LoggerConfig lcfg = config.getLoggerConfig("org.apache.logging.log4j.test1");
        assertNotNull("No Logger", lcfg);
        final Filter filter = lcfg.getFilter();
        assertNull("Unexpected Filter", filter);
    }

    @Test
    public void testNoFilters() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-nofilter.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Unexpected Configuration", "XMLConfigTest", config.getName());
        final LoggerConfig lcfg = config.getLoggerConfig("org.apache.logging.log4j.test1");
        assertNotNull("No Logger", lcfg);
        final Filter filter = lcfg.getFilter();
        assertNotNull("No Filter", filter);
        assertTrue("Incorrect filter", filter instanceof CompositeFilter);
        assertFalse("Unexpected filters", ((CompositeFilter) filter).hasFilters());
    }

    @Test
    public void testBadLayout() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-badlayout.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Unexpected Configuration", "XMLConfigTest", config.getName());
    }

    @Test
    public void testBadFileName() throws Exception {
        final StringBuilder dir = new StringBuilder("/VeryLongDirectoryName");

        for (final String element : CHARS) {
            dir.append(element);
            dir.append(element.toUpperCase());
        }
        final String value = FILESEP.equals("/") ? dir.toString() + "/test.log" : "1:/target/bad:file.log";
        System.setProperty("testfile", value);
        ctx = Configurator.initialize("Test1", "bad/log4j-badfilename.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertNotNull("No configuration", config);
        assertEquals("Unexpected Configuration", "XMLConfigTest", config.getName());
        assertTrue("Create bad appender", config.getAppenders().size() == 2);
    }

}
