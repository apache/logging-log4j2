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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.pattern.ArrayPatternConverter;
import org.apache.logging.log4j.core.pattern.FormattingInfo;
import org.apache.logging.log4j.core.pattern.IntegerPatternConverter;
import org.apache.logging.log4j.core.test.junit.CleanFolders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 *
 */
public abstract class RollingAppenderSizeMaxWidthTest implements RolloverListener {

    private static final String DIR = "target/rolling-max-width/archive";
    private static final String MESSAGE = "This is test message number ";
    private static final int[] POWERS_OF_10 = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    };

    private LoggerContext context;

    @RegisterExtension
    private CleanFolders cleanFolders;

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

    public RollingAppenderSizeMaxWidthTest(final LoggerContext context) {
        this.context = context;
        this.cleanFolders = new CleanFolders(DIR);
    }

    @BeforeEach
    public void setUp() {
        this.logger = context.getLogger(RollingAppenderSizeMaxWidthTest.class.getName());
        final RollingFileAppender app =
                (RollingFileAppender) context.getConfiguration().getAppender("RollingFile");
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
        assertNotNull(policy, "No SizeBasedTriggeringPolicy");
        rolloverSize = policy.getMaxFileSize();
    }

    @Test
    public void testAppender() {
        if (minWidth > 0) {
            assertTrue(min > -powerOfTen(minWidth), "min must be greater than or equal to the minimum width");
        }
        if (maxWidth < Integer.MAX_VALUE) {
            assertTrue(max <= powerOfTen(maxWidth), "max must be less than or equal to the maximum width");
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
        assertTrue(dir.exists(), "Directory not created");
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertTrue(
                rolloverCount + 1 >= minExpected,
                "Not enough rollovers: expected: " + minExpected + ", actual: " + rolloverCount);
        assertTrue(
                rolloverCount <= maxExpected,
                "Too many rollovers: expected: " + maxExpected + ", actual: " + rolloverCount);
        final int maxFiles = max - min + 1;
        final int maxExpectedFiles = Math.min(maxFiles, rolloverCount);
        assertEquals(
                maxExpectedFiles,
                files.length,
                "More files than expected. expected: " + maxExpectedFiles + ", actual: " + files.length);
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
