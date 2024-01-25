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
package org.apache.logging.log4j.core.appender;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.Test;

/**
 * Simple tests for both the RandomAccessFileAppender and RollingRandomAccessFileAppender.
 */
public class RandomAccessFileAppenderTest {

    public static final String MESSAGE = "This is a test log message brought to you by Slurm.";

    @TempLoggingDir
    private static Path loggingPath;

    @Test
    @LoggerContextSource
    public void testRandomAccessConfiguration(final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger(getClass());
        logger.info(MESSAGE);
        ctx.stop();

        for (final String fileName : List.of("RandomAccessFile.log", "RollingRandomAccessFile.log")) {
            final Path file = loggingPath.resolve(fileName);
            assertThat(file).isNotEmptyFile();
            assertThat(Files.readAllLines(file)).hasSize(1).containsExactly(MESSAGE);
        }
    }
}
