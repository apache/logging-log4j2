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
package org.apache.logging.log4j.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.CoreLoggerContexts;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for LOG4J2-2368.
 */
@LoggerContextSource("log4j-nested-logging-throwable-message.xml")
public class NestedLoggingFromThrowableMessageTest {

    private static final File file1 = new File("target/NestedLoggerTest1.log");
    private static final File file2 = new File("target/NestedLoggerTest2.log");

    @BeforeAll
    public static void beforeAll() {
        System.setProperty("log4j2.enableThreadlocals", "true");
    }

    private Logger logger;

    @BeforeEach
    public void before() {
        logger = LogManager.getLogger(NestedLoggingFromThrowableMessageTest.class);
    }

    class ThrowableLogsInGetMessage extends RuntimeException {

        @Override
        public String getMessage() {
            logger.info("Logging in getMessage");
            return "message";
        }
    }

    @Test
    public void testNestedLoggingInLastArgument() throws Exception {
        logger.error("Test", new ThrowableLogsInGetMessage());
        // stop async thread
        CoreLoggerContexts.stopLoggerContext(false, file1);
        CoreLoggerContexts.stopLoggerContext(false, file2);

        final Set<String> lines1 = readUniqueLines(file1);
        final Set<String> lines2 = readUniqueLines(file2);

        assertEquals(lines1, lines2, "Expected the same data from both appenders");
        assertEquals(2, lines1.size());
        assertTrue(lines1.contains("INFO NestedLoggingFromThrowableMessageTest Logging in getMessage "));
        assertTrue(lines1.contains("ERROR NestedLoggingFromThrowableMessageTest Test message"));
    }

    private static Set<String> readUniqueLines(final File input) throws IOException {
        final Set<String> lines = new HashSet<>();
        try (final BufferedReader reader =
                new BufferedReader(new InputStreamReader(Files.newInputStream(input.toPath())))) {
            String line;
            while ((line = reader.readLine()) != null) {
                assertTrue(lines.add(line), "Read duplicate line: " + line);
            }
        }
        return lines;
    }
}
