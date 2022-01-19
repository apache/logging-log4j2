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
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Test;

public abstract class AbstractLog4j1ConfigurationTest {

    abstract Configuration getConfiguration(String configResourcePrefix) throws URISyntaxException, IOException;

    public void testConsoleCapitalization() throws Exception {
        final Configuration config = getConfiguration("config-1.2/log4j-capitalization");
        final Appender capitalized = config.getAppender("ConsoleCapitalized");
        assertNotNull(capitalized);
        assertEquals(capitalized.getClass(), ConsoleAppender.class);
        final Appender javaStyle = config.getAppender("ConsoleJavaStyle");
        assertNotNull(javaStyle);
        assertEquals(javaStyle.getClass(), ConsoleAppender.class);
        testConsoleAppender((ConsoleAppender) capitalized, (ConsoleAppender) javaStyle);
    }

    private void testConsoleAppender(ConsoleAppender expected, ConsoleAppender actual) {
        assertEquals("immediateFlush", expected.getImmediateFlush(), actual.getImmediateFlush());
        assertEquals("target", expected.getTarget(), actual.getTarget());
        assertEquals("layoutClass", expected.getLayout().getClass(), actual.getLayout().getClass());
        if (expected.getLayout() instanceof PatternLayout) {
            patternLayoutEquals((PatternLayout) expected.getLayout(), (PatternLayout) actual.getLayout());
        }
    }

    private void patternLayoutEquals(PatternLayout expected, PatternLayout actual) {
        assertEquals(expected.getCharset(), actual.getCharset());
        assertEquals(expected.getConversionPattern(), actual.getConversionPattern());
    }

    private Layout<?> testConsole(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final String name = "Console";
        final ConsoleAppender appender = configuration.getAppender(name);
        assertNotNull("Missing appender '" + name + "' in configuration " + configResource + " → " + configuration,
                appender);
        assertEquals(Target.SYSTEM_ERR, appender.getTarget());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

    @Test
    public void testConsoleTtccLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-TTCCLayout");
        assertEquals("%r [%t] %p %notEmpty{%ndc }- %m%n", layout.getConversionPattern());
    }

}
