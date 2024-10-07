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
package org.apache.log4j.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test configuration from Properties.
 */
public class PropertiesRollingWithPropertiesTest {

    private static final String TEST_DIR = "target/" + PropertiesRollingWithPropertiesTest.class.getSimpleName();

    @BeforeAll
    public static void setupSystemProperties() {
        // final File file = new File(tempDir, TEST_DIR);

        // Set system properties as a replacement for SystemPropertyTestRule
        System.setProperty("test.directory", TEST_DIR);
        System.setProperty("log4j.configuration", "target/test-classes/log4j1-rolling-properties.properties");
    }

    @Test
    public void testProperties() throws Exception {
        final Path path = Paths.get(TEST_DIR, "somefile.log");
        Files.deleteIfExists(path);
        final Logger logger = LogManager.getLogger("test");
        logger.debug("This is a test of the root logger");
        assertTrue(Files.exists(path), "Log file was not created");
        assertTrue(Files.size(path) > 0, "Log file is empty");
    }
}
