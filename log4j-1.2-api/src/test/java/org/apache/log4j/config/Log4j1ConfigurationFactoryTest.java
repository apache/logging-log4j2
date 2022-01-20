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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.layout.Log4j1XmlLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Test;

public class Log4j1ConfigurationFactoryTest extends AbstractLog4j1ConfigurationTest {

    private static final String SUFFIX = ".properties";

    @Override
    protected Configuration getConfiguration(final String configResource) throws URISyntaxException {
        final URL configLocation = ClassLoader.getSystemResource(configResource + SUFFIX);
        assertNotNull(configResource, configLocation);
        final Configuration configuration = new Log4j1ConfigurationFactory().getConfiguration(null, "test", configLocation.toURI());
        assertNotNull(configuration);
        return configuration;
    }

    private Layout<?> testConsole(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final String name = "Console";
        final ConsoleAppender appender = configuration.getAppender(name);
        assertNotNull("Missing appender '" + name + "' in configuration " + configResource + " → " + configuration, appender);
        assertEquals(Target.SYSTEM_ERR, appender.getTarget());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

    private Layout<?> testFile(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final FileAppender appender = configuration.getAppender("File");
        assertNotNull(appender);
        assertEquals("target/mylog.txt", appender.getFileName());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

    @Test
    public void testConsoleEnhancedPatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-EnhancedPatternLayout");
        assertEquals("%d{ISO8601} [%t][%c] %-5p %properties %ndc: %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleHtmlLayout() throws Exception {
        final HtmlLayout layout = (HtmlLayout) testConsole("config-1.2/log4j-console-HtmlLayout");
        assertEquals("Headline", layout.getTitle());
        assertTrue(layout.isLocationInfo());
    }

    @Test
    public void testConsolePatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-PatternLayout");
        assertEquals("%d{ISO8601} [%t][%c] %-5p: %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-SimpleLayout");
        assertEquals("%level - %m%n", layout.getConversionPattern());
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

    @Test
    public void testFileSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testFile("config-1.2/log4j-file-SimpleLayout");
        assertEquals("%level - %m%n", layout.getConversionPattern());
    }

    @Test
    public void testNullAppender() throws Exception {
        final Configuration configuration = getConfiguration("config-1.2/log4j-NullAppender");
        final Appender appender = configuration.getAppender("NullAppender");
        assertNotNull(appender);
        assertEquals("NullAppender", appender.getName());
        assertTrue(appender.getClass().getName(), appender instanceof NullAppender);
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

}
