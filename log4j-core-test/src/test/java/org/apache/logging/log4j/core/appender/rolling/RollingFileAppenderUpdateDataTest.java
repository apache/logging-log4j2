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
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests LOG4J2-2009 Rolling appender managers broken on pattern/policy reconfiguration
 */
public class RollingFileAppenderUpdateDataTest {

    private ConfigurationBuilder<BuiltConfiguration> buildConfigA() {
        return buildConfigurationBuilder("target/rolling-update-date/foo.log.%i");
    }

    // rebuild config with date based rollover
    private ConfigurationBuilder<BuiltConfiguration> buildConfigB() {
        return buildConfigurationBuilder("target/rolling-update-date/foo.log.%d{yyyy-MM-dd-HH:mm:ss}.%i");
    }

    private ConfigurationBuilder<BuiltConfiguration> buildConfigurationBuilder(final String filePattern) {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setConfigurationName("LOG4J2-1964 demo");
        builder.setStatusLevel(Level.ERROR);
        // @formatter:off
        builder.add(
                builder.newAppender("consoleLog", "Console").addAttribute("target", ConsoleAppender.Target.SYSTEM_ERR));
        builder.add(builder.newAppender("fooAppender", "RollingFile")
                .addAttribute("fileName", "target/rolling-update-date/foo.log")
                .addAttribute("filePattern", filePattern)
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "10MB")));
        builder.add(builder.newRootLogger(Level.INFO)
                .add(builder.newAppenderRef("consoleLog"))
                .add(builder.newAppenderRef("fooAppender")));
        // @formatter:on
        return builder;
    }

    private LoggerContext loggerContext1 = null;
    private LoggerContext loggerContext2 = null;

    @After
    public void after() {
        if (loggerContext1 != null) {
            loggerContext1.close();
            loggerContext1 = null;
        }
        if (loggerContext2 != null) {
            loggerContext2.close();
            loggerContext2 = null;
        }
    }

    @Test
    public void testClosingLoggerContext() {
        // initial config with indexed rollover
        try (final LoggerContext loggerContext1 =
                Configurator.initialize(buildConfigA().build())) {
            validateAppender(loggerContext1, "target/rolling-update-date/foo.log.%i");
        }

        // rebuild config with date based rollover
        try (final LoggerContext loggerContext2 =
                Configurator.initialize(buildConfigB().build())) {
            validateAppender(loggerContext2, "target/rolling-update-date/foo.log.%d{yyyy-MM-dd-HH:mm:ss}.%i");
        }
    }

    @Test
    public void testNotClosingLoggerContext() {
        // initial config with indexed rollover
        loggerContext1 = Configurator.initialize(buildConfigA().build());
        validateAppender(loggerContext1, "target/rolling-update-date/foo.log.%i");

        // rebuild config with date based rollover
        loggerContext2 = Configurator.initialize(buildConfigB().build());
        Assert.assertNotNull("No LoggerContext", loggerContext2);
        Assert.assertTrue("Expected same logger context to be returned", loggerContext1 == loggerContext2);
        validateAppender(loggerContext1, "target/rolling-update-date/foo.log.%i");
    }

    @Test
    public void testReconfigure() {
        // initial config with indexed rollover
        loggerContext1 = Configurator.initialize(buildConfigA().build());
        validateAppender(loggerContext1, "target/rolling-update-date/foo.log.%i");

        // rebuild config with date based rollover
        loggerContext1.setConfiguration(buildConfigB().build());
        validateAppender(loggerContext1, "target/rolling-update-date/foo.log.%d{yyyy-MM-dd-HH:mm:ss}.%i");
    }

    private void validateAppender(final LoggerContext loggerContext, final String expectedFilePattern) {
        final RollingFileAppender appender = loggerContext.getConfiguration().getAppender("fooAppender");
        Assert.assertNotNull(appender);
        Assert.assertEquals(expectedFilePattern, appender.getFilePattern());
        LogManager.getLogger("root").info("just to show it works.");
    }
}
