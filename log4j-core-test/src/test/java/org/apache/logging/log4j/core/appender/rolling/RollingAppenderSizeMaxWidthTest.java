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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.pattern.ArrayPatternConverter;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import org.apache.logging.log4j.core.pattern.IntegerPatternConverter;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 */
@RunWith(Parameterized.class)
public class RollingAppenderSizeMaxWidthTest implements RolloverListener {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {
                new LoggerContextRule("log4j-rolling-size-max-width-1.xml"),
            },
            {
                new LoggerContextRule("log4j-rolling-size-max-width-2.xml"),
            },
            {
                new LoggerContextRule("log4j-rolling-size-max-width-3.xml"),
            },
            {
                new LoggerContextRule("log4j-rolling-size-max-width-4.xml"),
            },
        });
    }

    private static final String DIR = "target/rolling-max-width/archive";
    private static final String MESSAGE = "This is test message number ";
    private static final int COUNT = 10000;
    private static final int[] POWERS_OF_10 = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    };
    public LoggerContextRule loggerContextRule;

    @Rule
    public RuleChain chain;

    List<String> rolledFileNames = new ArrayList<>();
    int min;
    int max;
    boolean isZeroPad;
    int minWidth;
    int maxWidth;
    long rolloverSize;
    private Logger logger;
    private int rolloverCount = 0;

    private static int powerOfTen(final int pow) {
        if (pow > POWERS_OF_10.length) {
            throw new IllegalArgumentException("Max width is too large");
        }
        return POWERS_OF_10[pow];
    }

    public RollingAppenderSizeMaxWidthTest(final LoggerContextRule loggerContextRule) {
        this.loggerContextRule = loggerContextRule;
        this.chain = loggerContextRule.withCleanFoldersRule(DIR);
    }

    @Before
    public void setUp() throws Exception {
        this.logger = loggerContextRule.getLogger(RollingAppenderSizeMaxWidthTest.class.getName());
        final RollingFileAppender app = (RollingFileAppender) loggerContextRule.getRequiredAppender("RollingFile");
        app.getManager().addRolloverListener(this);
        final ArrayPatternConverter[] patternConverters =
                app.getManager().getPatternProcessor().getPatternConverters();
        final int index = IntStream.range(0, patternConverters.length)
                .filter(i -> patternConverters[i] instanceof IntegerPatternConverter)
                .findFirst()
                .orElse(-1);
        if (index < 0) {
            fail("Could not find integer pattern converter in " + app.getFilePattern());
        }
        final FormattingInfo formattingInfo =
                app.getManager().getPatternProcessor().getPatternFields()[index];
        minWidth = formattingInfo.getMinLength();
        maxWidth = formattingInfo.getMaxLength();
        isZeroPad = formattingInfo.isZeroPad();
        final DefaultRolloverStrategy strategy =
                (DefaultRolloverStrategy) app.getManager().getRolloverStrategy();
        min = strategy.getMinIndex();
        max = strategy.getMaxIndex();
        SizeBasedTriggeringPolicy policy;
        if (app.getTriggeringPolicy() instanceof CompositeTriggeringPolicy) {
            policy = (SizeBasedTriggeringPolicy)
                    Arrays.stream(((CompositeTriggeringPolicy) app.getTriggeringPolicy()).getTriggeringPolicies())
                            .filter((p) -> p instanceof SizeBasedTriggeringPolicy)
                            .findFirst()
                            .orElse(null);
        } else {
            policy = app.getTriggeringPolicy();
        }
        assertNotNull("No SizeBasedTriggeringPolicy", policy);
        rolloverSize = policy.getMaxFileSize();
    }

    @Test
    public void testAppender() throws Exception {
        if (minWidth > 0) {
            assertTrue("min must be greater than or equal to the minimum width", min > -powerOfTen(minWidth));
        }
        if (maxWidth < Integer.MAX_VALUE) {
            assertTrue("max must be less than or equal to the maximum width", max <= powerOfTen(maxWidth));
        }
        long bytes = 0;
        for (int i = 0; i < 10000; ++i) {
            final String message = MESSAGE + i;
            logger.debug(message);
            bytes += message.length() + 1;
        }
        final long minExpected = ((bytes / rolloverSize) * 95) / 100;
        final long maxExpected = ((bytes / rolloverSize) * 105) / 100;
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists());
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertTrue(
                "Not enough rollovers: expected: " + minExpected + ", actual: " + rolloverCount,
                rolloverCount + 1 >= minExpected);
        assertTrue(
                "Too many rollovers: expected: " + maxExpected + ", actual: " + rolloverCount,
                rolloverCount <= maxExpected);
        final int maxFiles = max - min + 1;
        final int maxExpectedFiles = Math.min(maxFiles, rolloverCount);
        assertEquals(
                "More files than expected. expected: " + maxExpectedFiles + ", actual: " + files.length,
                maxExpectedFiles,
                files.length);
    }

    @Override
    public void rolloverTriggered(final String fileName) {
        ++rolloverCount;
    }

    @Override
    public void rolloverComplete(final String fileName) {
        rolledFileNames.add(fileName);
    }
}
