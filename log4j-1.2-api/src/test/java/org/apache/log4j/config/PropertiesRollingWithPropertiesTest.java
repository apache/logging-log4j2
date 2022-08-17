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
package org.apache.log4j.config;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test configuration from Properties.
 */
public class PropertiesRollingWithPropertiesTest {

    private static final String TEST_DIR = "target/PropertiesRollingWithPropertiesTest";

    @BeforeAll
    static void beforeAll() {
        System.setProperty("test.directory", TEST_DIR);
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("test.directory");
    }

    @Test
    @CleanUpDirectories(TEST_DIR)
    @LoggerContextSource(value = "log4j1-rolling-properties.properties", v1config = true)
    public void testProperties(final LoggerContext context) throws Exception {
        final Logger logger = context.getLogger("test");
        logger.debug("This is a test of the root logger");
        assertThat(Paths.get(TEST_DIR, "somefile.log")).exists().isNotEmptyFile();
    }

}
