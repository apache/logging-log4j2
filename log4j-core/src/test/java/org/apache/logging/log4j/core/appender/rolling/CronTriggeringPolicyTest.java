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

package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CronTriggeringPolicyTest {

    private static final String CRON_EXPRESSION = "0 0 0 * * ?";

    private NullConfiguration configuration;

     // TODO Need a CleanRegexFiles("testcmd.\\.log\\..*");
     //@Rule
     //public CleanFiles cleanFiles = new CleanFiles("testcmd1.log", "testcmd2.log", "testcmd3.log");

    @BeforeEach
    public void before() {
        configuration = new NullConfiguration();
    }

    private CronTriggeringPolicy createPolicy() {
        return CronTriggeringPolicy.createPolicy(configuration, Boolean.TRUE.toString(), CRON_EXPRESSION);
    }

    private DefaultRolloverStrategy createStrategy() {
        return DefaultRolloverStrategy.createStrategy("7", "1", "max", null, null, false, configuration);
    }

    private void testBuilder() {
        // @formatter:off
        final RollingFileAppender raf = RollingFileAppender.newBuilder().setName("test1")
            .withFileName("target/testcmd1.log")
            .withFilePattern("target/testcmd1.log.%d{yyyy-MM-dd}")
            .withPolicy(createPolicy())
            .withStrategy(createStrategy())
            .setConfiguration(configuration)
            .build();
        // @formatter:on
        assertNotNull(raf);
    }

    /**
     * Tests LOG4J2-1474 CronTriggeringPolicy raise exception and fail to rollover log file when evaluateOnStartup is
     * true.
     */
    @Test
    public void testBuilderOnce() {
        testBuilder();
    }

    /**
     * Tests LOG4J2-1740 Add CronTriggeringPolicy programmatically leads to NPE
     */
    @Test
    public void testLoggerContextAndBuilder() {
        Configurator.initialize(configuration);
        testBuilder();
    }

    /**
     * Tests LOG4J2-1740 Add CronTriggeringPolicy programmatically leads to NPE
     */
    @Test
    public void testRollingRandomAccessFileAppender() {
        // @formatter:off
        RollingRandomAccessFileAppender.newBuilder().setName("test2")
            .withFileName("target/testcmd2.log")
            .withFilePattern("target/testcmd2.log.%d{yyyy-MM-dd}")
            .withPolicy(createPolicy())
            .withStrategy(createStrategy())
            .setConfiguration(configuration)
            .build();
        // @formatter:on
    }


    /**
     * Tests LOG4J2-1474 CronTriggeringPolicy raise exception and fail to rollover log file when evaluateOnStartup is
     * true.
     */
    @Test
    public void testBuilderSequence() {
        testBuilder();
        testBuilder();
    }

    private void testFactoryMethod() {
        final CronTriggeringPolicy triggerPolicy = createPolicy();
        final DefaultRolloverStrategy rolloverStrategy = createStrategy();

        try (RollingFileManager fileManager = RollingFileManager.getFileManager("target/testcmd3.log",
                "target/testcmd3.log.%d{yyyy-MM-dd}", true, true, triggerPolicy, rolloverStrategy, null,
                PatternLayout.createDefaultLayout(), 0, true, false, null, null, null, configuration)) {
            // trigger rollover
            fileManager.initialize();
            fileManager.rollover();
        }
    }

    /**
     * Tests LOG4J2-1474 CronTriggeringPolicy raise exception and fail to rollover log file when evaluateOnStartup is
     * true.
     */
    @Test
    public void testFactoryMethodOnce() {
        testFactoryMethod();
    }

    @Test
    public void testFactoryMethodSequence() {
        testFactoryMethod();
        testFactoryMethod();
    }
}
