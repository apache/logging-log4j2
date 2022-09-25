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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * Tests that zero-padding in rolled files works correctly.
 */
public class RolloverWithPaddingTest {
  private static final String CONFIG = "log4j-rolling-with-padding.xml";
  private static final String DIR = "target/rolling-with-padding";

  private final LoggerContextRule loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

  @Rule
  public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR);

  @Test
  public void testAppender() throws Exception {
    final Logger logger = loggerContextRule.getLogger();
    for (int i = 0; i < 10; ++i) {
      // 30 chars per message: each message triggers a rollover
      logger.fatal("This is a test message number " + i); // 30 chars:
    }
    Thread.sleep(100); // Allow time for rollover to complete

    final File dir = new File(DIR);
    assertTrue("Dir " + DIR + " should exist", dir.exists());
    assertTrue("Dir " + DIR + " should contain files", dir.listFiles().length == 6);

    final File[] files = dir.listFiles();
    final List<String> expected = Arrays.asList("rollingtest.log", "test-001.log", "test-002.log", "test-003.log", "test-004.log", "test-005.log");
    assertEquals("Unexpected number of files", expected.size(), files.length);
    for (final File file : files) {
      if (!expected.contains(file.getName())) {
        fail("unexpected file" + file);
      }
    }
  }
}
