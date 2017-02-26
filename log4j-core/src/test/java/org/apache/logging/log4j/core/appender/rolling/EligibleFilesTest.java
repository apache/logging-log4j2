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

import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.core.pattern.NotANumber;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test getEligibleFiles method.
 */
public class EligibleFilesTest {

    @Test
    public void runTest() throws Exception {
        String path = "target/test-classes/rolloverPath/log4j.txt.20170112_09-" + NotANumber.VALUE + ".gz";
        TestRolloverStrategy strategy = new TestRolloverStrategy();
        Map<Integer, Path> files = strategy.findFilesInPath(path);
        assertTrue("No files found", files.size() > 0);
        assertTrue("Incorrect number of files found. Should be 30, was " + files.size(), files.size() == 30);
    }

    private class TestRolloverStrategy extends AbstractRolloverStrategy {

        public TestRolloverStrategy() {
            super(null);
        }

        @Override
        public RolloverDescription rollover(RollingFileManager manager) throws SecurityException {
            return null;
        }

        public Map<Integer, Path> findFilesInPath(String path) {
            return getEligibleFiles(path, "log4j.txt.%d{yyyyMMdd}-%i.gz");
        }
    }
}
