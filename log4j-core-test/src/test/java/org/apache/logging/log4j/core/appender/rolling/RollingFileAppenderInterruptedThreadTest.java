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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.test.junit.CleanFolders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests https://issues.apache.org/jira/browse/LOG4J2-1798
 */
public class RollingFileAppenderInterruptedThreadTest {

    private static final String ROLLING_APPENDER_FILES_DIR =
            "target/" + RollingFileAppenderInterruptedThreadTest.class.getSimpleName();

    LoggerContext loggerContext;

    @RegisterExtension
    private CleanFolders cleanFolders = new CleanFolders(true, false, 3, ROLLING_APPENDER_FILES_DIR);

    @BeforeEach
    public void setUp() {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setConfigurationName("LOG4J2-1798 test");

        builder.add(
                builder.newAppender("consoleLog", "Console").addAttribute("target", ConsoleAppender.Target.SYSTEM_ERR));

        builder.add(builder.newAppender("fileAppender", "RollingFile")
                .addAttribute("filePattern", ROLLING_APPENDER_FILES_DIR + "/file-%i.log")
                .add(builder.newLayout("PatternLayout").addAttribute("pattern", "%msg%n"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy")
                        .addAttribute("size", "20B"))); // relatively small amount to trigger rotation quickly

        builder.add(builder.newRootLogger(Level.INFO)
                .add(builder.newAppenderRef("consoleLog"))
                .add(builder.newAppenderRef("fileAppender")));

        loggerContext = Configurator.initialize(builder.build());
    }

    @AfterEach
    public void tearDown() {
        Configurator.shutdown(loggerContext);
        loggerContext = null;
    }

    @Test
    public void testRolloverInInterruptedThread() {
        final Logger logger = loggerContext.getLogger(getClass().getName());

        assertThat(logger.getAppenders().values(), hasItem(instanceOf(RollingFileAppender.class)));

        logger.info("Sending logging event 1"); // send first event to initialize rollover system

        Thread.currentThread().interrupt(); // mark thread as interrupted
        logger.info("Sending logging event 2"); // send second event to trigger rotation, expecting 2 files in result

        assertTrue(new File(ROLLING_APPENDER_FILES_DIR, "file-1.log").exists());
        assertTrue(new File(ROLLING_APPENDER_FILES_DIR, "file-2.log").exists());
    }
}
