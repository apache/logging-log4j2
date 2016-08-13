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

import org.apache.log4j.layout.TTCCLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.layout.XmlLayout;
import org.junit.Assert;
import org.junit.Test;

public class Log4j1ConfigurationFactoryTest {

    private Layout<?> testConsole(final String configResource) throws Exception {
        final URL configLocation = ClassLoader.getSystemResource(configResource);
        Assert.assertNotNull(configLocation);
        final Configuration configuration = new Log4j1ConfigurationFactory().getConfiguration("test",
                configLocation.toURI());
        Assert.assertNotNull(configuration);
        final ConsoleAppender appender = configuration.getAppender("Console");
        Assert.assertNotNull(appender);
        // Can't set ImmediateFlush for a Console Appender in Log4j 2 like you can in 1.2
        Assert.assertTrue(appender.getImmediateFlush());
        Assert.assertEquals(Target.SYSTEM_ERR, appender.getTarget());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        Assert.assertNotNull(loggerConfig);
        Assert.assertEquals(Level.DEBUG, loggerConfig.getLevel());
        return appender.getLayout();
    }

    @Test
    public void testConsoleEnhancedPatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-EnhancedPatternLayout.properties");
        Assert.assertEquals("%d{ISO8601} [%t][%c] %-5p: %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleHtmlLayout() throws Exception {
        final Layout<?> layout = testConsole("config-1.2/log4j-console-HtmlLayout.properties");
        Assert.assertTrue(layout instanceof HtmlLayout);
    }

    @Test
    public void testConsolePatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-PatternLayout.properties");
        Assert.assertEquals("%d{ISO8601} [%t][%c] %-5p: %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-SimpleLayout.properties");
        Assert.assertEquals("%level - %m%n", layout.getConversionPattern());
    }

    @Test
    public void testConsoleTtccLayout() throws Exception {
        final Layout<?> layout = testConsole("config-1.2/log4j-console-TTCCLayout.properties");
        Assert.assertTrue(layout instanceof TTCCLayout);
    }

    @Test
    public void testConsoleXmlLayout() throws Exception {
        final Layout<?> layout = testConsole("config-1.2/log4j-console-XmlLayout.properties");
        Assert.assertTrue(layout instanceof XmlLayout);
    }
}
