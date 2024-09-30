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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.logging.log4j.core.appender.rolling.action.CompressActionFactoryProvider;
import org.apache.logging.log4j.core.pattern.NotANumber;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test getEligibleFiles method.
 */
class EligibleFilesTest {

    private static final int COUNT_1 = 20;
    private static final int COUNT_2 = 30;
    private static final String PREFIX_1 = "log4j.txt.20170112_09-";
    private static final String PREFIX_2 = "log4j.20211028T194500+0200.";

    @TempLoggingDir
    private static Path loggingPath;

    @BeforeAll
    static void setup() throws IOException {
        int i;
        // Create files for tests
        for (i = 1; i < COUNT_1; i++) {
            Files.createFile(loggingPath.resolve(String.format("%s%02d%s", PREFIX_1, i, ".gz")));
        }
        Files.createFile(loggingPath.resolve(String.format("%s%02d", PREFIX_1, i)));

        for (i = 1; i < COUNT_2; i++) {
            Files.createFile(loggingPath.resolve(String.format("%s%d%s", PREFIX_2, i, ".log.gz")));
        }
        Files.createFile(loggingPath.resolve(String.format("%s%d%s", PREFIX_2, i, ".log")));
    }

    @Test
    void runTest() throws Exception {
        final String path = loggingPath + "/" + PREFIX_1 + NotANumber.VALUE + ".gz";
        final TestRolloverStrategy strategy = new TestRolloverStrategy();
        final Map<Integer, Path> files = strategy.findFilesInPath(path);
        assertThat(files).isNotEmpty().hasSize(COUNT_1);
    }

    @Test
    void runTestWithPlusCharacter() throws Exception {
        final String path = loggingPath + "/" + PREFIX_2 + NotANumber.VALUE + ".log.gz";
        final TestRolloverStrategy strategy = new TestRolloverStrategy();
        final Map<Integer, Path> files = strategy.findFilesWithPlusInPath(path);
        assertThat(files).isNotEmpty().hasSize(COUNT_2);
    }

    private static class TestRolloverStrategy extends AbstractRolloverStrategy {

        public TestRolloverStrategy() {
            super(CompressActionFactoryProvider.newInstance(null), null);
        }

        @Override
        public RolloverDescription rollover(final RollingFileManager manager) throws SecurityException {
            return null;
        }

        public Map<Integer, Path> findFilesInPath(final String path) {
            return getEligibleFiles(path, "log4j.txt.%d{yyyyMMdd}-%i.gz");
        }

        public Map<Integer, Path> findFilesWithPlusInPath(final String path) {
            // timezone might expand to "+0200", because of '+' we have to be careful when working with regex
            return getEligibleFiles(path, "log4j.txt.%d{yyyyMMdd'T'HHmmssZ}.%i.log.gz");
        }
    }
}
