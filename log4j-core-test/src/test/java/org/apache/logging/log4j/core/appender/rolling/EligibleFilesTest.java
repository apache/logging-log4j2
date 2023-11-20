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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Map;
import org.apache.logging.log4j.core.pattern.NotANumber;
import org.junit.jupiter.api.Test;

/**
 * Test getEligibleFiles method.
 */
public class EligibleFilesTest {

    @Test
    public void runTest() throws Exception {
        final String path = "target/test-classes/rolloverPath/log4j.txt.20170112_09-" + NotANumber.VALUE + ".gz";
        final TestRolloverStrategy strategy = new TestRolloverStrategy();
        final Map<Integer, Path> files = strategy.findFilesInPath(path);
        assertTrue(files.size() > 0, "No files found");
        assertEquals(30, files.size(), "Incorrect number of files found. Should be 30, was " + files.size());
    }

    @Test
    public void runTestWithPlusCharacter() throws Exception {
        final String path =
                "target/test-classes/rolloverPath/log4j.20211028T194500+0200." + NotANumber.VALUE + ".log.gz";
        final TestRolloverStrategy strategy = new TestRolloverStrategy();
        final Map<Integer, Path> files = strategy.findFilesWithPlusInPath(path);
        assertTrue(files.size() > 0, "No files found");
        assertEquals(30, files.size(), "Incorrect number of files found. Should be 30, was " + files.size());
    }

    private static class TestRolloverStrategy extends AbstractRolloverStrategy {

        public TestRolloverStrategy() {
            super(null);
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
