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

import java.net.URL;

import org.apache.log4j.layout.Log4j1XmlLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Test;

import static org.junit.Assert.*;

public class Log4j1ConfigurationFactoryTest {

    private Layout<?> testConsole(final String configResource) throws Exception {
        final URL configLocation = ClassLoader.getSystemResource(configResource);
        assertNotNull(configLocation);
        final Configuration configuration = new Log4j1ConfigurationFactory().getConfiguration("test",
                configLocation.toURI());
        assertNotNull(configuration);
        final ConsoleAppender appender = configuration.getAppender("Console");
        assertNotNull(appender);
        // Can't set ImmediateFlush for a Console Appender in Log4j 2 like you can in 1.2
        assertTrue(appender.getImmediateFlush());
        assertEquals(Target.SYSTEM_ERR, appender.getTarget());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        return appender.getLayout();
    }

    @Test
    public void testConsoleEnhancedPatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-EnhancedPatternLayout.properties");
        assertEquals("%d{ISO8601} [%t][%c] %-5p %properties %ndc: %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleHtmlLayout() throws Exception {
        final HtmlLayout layout = (HtmlLayout) testConsole("config-1.2/log4j-console-HtmlLayout.properties");
        assertEquals("Headline", layout.getTitle());
        assertTrue(layout.isLocationInfo());
    }

    @Test
    public void testConsolePatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-PatternLayout.properties");
        assertEquals("%d{ISO8601} [%t][%c] %-5p: %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-SimpleLayout.properties");
        assertEquals("%level - %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleTtccLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-TTCCLayout.properties");
        assertEquals("%r [%t] %p %notEmpty{%ndc }- %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleXmlLayout() throws Exception {
        final Log4j1XmlLayout layout = (Log4j1XmlLayout) testConsole("config-1.2/log4j-console-XmlLayout.properties");
        assertTrue(layout.isLocationInfo());
        assertFalse(layout.isProperties());
    }
}
