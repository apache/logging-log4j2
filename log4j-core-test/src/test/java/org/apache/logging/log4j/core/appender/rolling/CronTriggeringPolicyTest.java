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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CronExpression;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class CronTriggeringPolicyTest {

    private static final String CRON_EXPRESSION = "0 0 0 * * ?";

    private NullConfiguration configuration;

    // TODO Need a CleanRegexFiles("testcmd.\\.log\\..*");
    // @Rule
    // public CleanFiles cleanFiles = new CleanFiles("testcmd1.log", "testcmd2.log", "testcmd3.log");

    @BeforeEach
    void before() {
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
        final RollingFileAppender raf = RollingFileAppender.newBuilder()
                .setName("test1")
                .setFileName("target/testcmd1.log")
                .setFilePattern("target/testcmd1.log.%d{yyyy-MM-dd}")
                .setPolicy(createPolicy())
                .setStrategy(createStrategy())
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
    void testBuilderOnce() {
        testBuilder();
    }

    /**
     * An appender configured without a {@code fileName} writes directly to the file the pattern
     * resolves to, so {@code RollingFileManager#getFileTime()} reports 0 until that file exists.
     * Looking up the previous fire time relative to the epoch yields nothing useful and used to
     * cost roughly three seconds per appender, delaying startup.
     */
    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void testBuilderWithoutFileNameInitializesPromptly() {
        // @formatter:off
        final RollingFileAppender raf = RollingFileAppender.newBuilder()
                .setName("test4")
                .setFilePattern("target/testcmd4.log.%d{yyyy-MM-dd}")
                // A weekly schedule is the worst case for the backward search.
                .setPolicy(CronTriggeringPolicy.createPolicy(configuration, Boolean.TRUE.toString(), "0 0 0 ? * SUN"))
                // No strategy: without a file name the builder selects DirectWriteRolloverStrategy,
                // which is what a configuration that omits `fileName` ends up using.
                .setConfiguration(configuration)
                .build();
        // @formatter:on
        assertNotNull(raf);
    }

    /**
     * Without a {@code fileName} the appender writes directly to the file its pattern resolves to.
     * That file covers the whole rollover period, so it must be named after the period's start
     * rather than after the moment the appender happened to start. Otherwise a restart later in
     * the period opens a second file for a period that is supposed to have only one.
     */
    @Test
    void testDirectWriteFileNameUsesPeriodStart() throws Exception {
        final String schedule = "0 0 0 ? * SUN";
        // @formatter:off
        final RollingFileAppender raf = RollingFileAppender.newBuilder()
                .setName("test5")
                .setFilePattern("target/testcmd5.log-%d{yyyyMMdd}")
                .setPolicy(CronTriggeringPolicy.createPolicy(configuration, Boolean.FALSE.toString(), schedule))
                .setConfiguration(configuration)
                .build();
        // @formatter:on
        assertNotNull(raf);

        final Date periodStart = new CronExpression(schedule).getPrevFireTime(new Date());
        final String expected = "target/testcmd5.log-" + new SimpleDateFormat("yyyyMMdd").format(periodStart);
        assertEquals(expected, raf.getManager().getFileName());
    }

    /**
     * Tests LOG4J2-1740 Add CronTriggeringPolicy programmatically leads to NPE
     */
    @Test
    void testLoggerContextAndBuilder() {
        Configurator.initialize(configuration);
        testBuilder();
    }

    /**
     * Tests LOG4J2-1740 Add CronTriggeringPolicy programmatically leads to NPE
     */
    @Test
    void testRollingRandomAccessFileAppender() {
        // @formatter:off
        RollingRandomAccessFileAppender.newBuilder()
                .setName("test2")
                .setFileName("target/testcmd2.log")
                .setFilePattern("target/testcmd2.log.%d{yyyy-MM-dd}")
                .setPolicy(createPolicy())
                .setStrategy(createStrategy())
                .setConfiguration(configuration)
                .build();
        // @formatter:on
    }

    /**
     * Tests LOG4J2-1474 CronTriggeringPolicy raise exception and fail to rollover log file when evaluateOnStartup is
     * true.
     */
    @Test
    void testBuilderSequence() {
        testBuilder();
        testBuilder();
    }

    private void testFactoryMethod() {
        final CronTriggeringPolicy triggerPolicy = createPolicy();
        final DefaultRolloverStrategy rolloverStrategy = createStrategy();

        try (final RollingFileManager fileManager = RollingFileManager.getFileManager(
                "target/testcmd3.log",
                "target/testcmd3.log.%d{yyyy-MM-dd}",
                true,
                true,
                triggerPolicy,
                rolloverStrategy,
                null,
                PatternLayout.createDefaultLayout(),
                0,
                true,
                false,
                null,
                null,
                null,
                configuration)) {
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
    void testFactoryMethodOnce() {
        testFactoryMethod();
    }

    @Test
    void testFactoryMethodSequence() {
        testFactoryMethod();
        testFactoryMethod();
    }
}
