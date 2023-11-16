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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.time.LocalTime;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Test;

@CleanUpDirectories(RollingRandomAppenderDirectWriteAndSwitchDirectorTest.DIR)
public class RollingRandomAppenderDirectWriteAndSwitchDirectorTest {
    public static final String DIR = "target/rolling-random-direct-switch-director";

    @Test
    @LoggerContextSource(value = "log4j-rolling-random-direct-switch-director.xml", timeout = 10)
    public void testAppender(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger(RollingRandomAppenderDirectWriteAndSwitchDirectorTest.class.getName());
        final LocalTime start = LocalTime.now();
        LocalTime end;
        do {
            end = LocalTime.now();
            logger.info("test log");
            Thread.sleep(100);
        } while (start.getSecond() == end.getSecond());

        final File nextLogFile = new File(String.format("%s/%d/%d.log", DIR, end.getSecond(), end.getSecond()));
        assertTrue(nextLogFile.exists(), "nextLogFile not created");
    }
}
