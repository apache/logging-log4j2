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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.log4j.layout.Log4j1XmlLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Test;

public class Log4j1ConfigurationFactoryTest extends AbstractLog4j1ConfigurationTest {

    private static final String SUFFIX = ".properties";

    @Override
    protected Configuration getConfiguration(final String configResource) throws URISyntaxException {
        final URL configLocation = ClassLoader.getSystemResource(configResource + SUFFIX);
        assertNotNull(configResource, configLocation);
        final Configuration configuration =
                new Log4j1ConfigurationFactory().getConfiguration(null, "test", configLocation.toURI());
        assertNotNull(configuration);
        return configuration;
    }

    private Layout<?> testConsole(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final String name = "Console";
        final ConsoleAppender appender = configuration.getAppender(name);
        assertNotNull(
                "Missing appender '" + name + "' in configuration " + configResource + " → " + configuration, appender);
        assertEquals(Target.SYSTEM_ERR, appender.getTarget());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

    @Override
    @Test
    public void testConsoleEnhancedPatternLayout() throws Exception {
        super.testConsoleEnhancedPatternLayout();
    }

    @Override
    @Test
    public void testConsoleHtmlLayout() throws Exception {
        super.testConsoleHtmlLayout();
    }

    @Test
    public void testConsolePatternLayout() throws Exception {
        super.testConsolePatternLayout();
    }

    @Override
    @Test
    public void testConsoleSimpleLayout() throws Exception {
        super.testConsoleSimpleLayout();
    }

    @Override
    @Test
    public void testConsoleTtccLayout() throws Exception {
        super.testConsoleTtccLayout();
    }

    @Test
    public void testConsoleXmlLayout() throws Exception {
        final Log4j1XmlLayout layout = (Log4j1XmlLayout) testConsole("config-1.2/log4j-console-XmlLayout");
        assertTrue(layout.isLocationInfo());
        assertFalse(layout.isProperties());
    }

    @Override
    @Test
    public void testFileSimpleLayout() throws Exception {
        super.testFileSimpleLayout();
    }

    @Override
    @Test
    public void testNullAppender() throws Exception {
        super.testNullAppender();
    }

    @Override
    @Test
    public void testRollingFileAppender() throws Exception {
        super.testRollingFileAppender();
    }

    @Override
    @Test
    public void testDailyRollingFileAppender() throws Exception {
        super.testDailyRollingFileAppender();
    }

    @Test
    public void testRollingFileAppenderWithProperties() throws Exception {
        super.testRollingFileAppenderWithProperties();
    }

    @Override
    @Test
    public void testSystemProperties1() throws Exception {
        super.testSystemProperties1();
    }

    @Override
    @Test
    public void testSystemProperties2() throws Exception {
        super.testSystemProperties2();
    }

    @Override
    @Test
    public void testConsoleCapitalization() throws Exception {
        super.testConsoleCapitalization();
    }

    @Override
    @Test
    public void testDefaultValues() throws Exception {
        super.testDefaultValues();
    }

    @Test
    public void testUntrimmedValues() throws Exception {
        try {
            final Configuration config = getConfiguration("config-1.2/log4j-untrimmed");
            final LoggerConfig rootLogger = config.getRootLogger();
            assertEquals(Level.DEBUG, rootLogger.getLevel());
            final Appender appender = config.getAppender("Console");
            assertTrue(appender instanceof ConsoleAppender);
            final Layout<? extends Serializable> layout = appender.getLayout();
            assertTrue(layout instanceof PatternLayout);
            assertEquals("%v1Level - %m%n", ((PatternLayout) layout).getConversionPattern());
            // No filter support
            config.start();
            config.stop();
        } catch (NoClassDefFoundError e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testGlobalThreshold() throws Exception {
        try (final LoggerContext ctx = configure("config-1.2/log4j-global-threshold")) {
            final Configuration config = ctx.getConfiguration();
            final Filter filter = config.getFilter();
            assertTrue(filter instanceof ThresholdFilter);
            final ThresholdFilter thresholdFilter = (ThresholdFilter) filter;
            assertEquals(Level.INFO, thresholdFilter.getLevel());
            assertEquals(Filter.Result.NEUTRAL, thresholdFilter.getOnMatch());
            assertEquals(Filter.Result.DENY, thresholdFilter.getOnMismatch());
        }
    }
}
