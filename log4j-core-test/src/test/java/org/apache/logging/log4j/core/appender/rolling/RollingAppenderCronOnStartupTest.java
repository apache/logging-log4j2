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

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RollingAppenderCronOnStartupTest {

    private static final String CONFIG = "log4j-rolling-cron-onStartup.xml";
    private static final String DIR = "target/rolling-cron-onStartup";
    private static final String FILE = "target/rolling-cron-onStartup/rollingtest.log";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private LoggerContext context;

    @AfterAll
    public static void after() {
        File dir = new File(DIR);
        if (dir.exists()) {
            cleanDir(dir);
            dir.delete();
        }

    }

    @AfterEach
    public void afterEach() {
        if (context != null) {
            context.stop();
        }
    }

    @Test
    public void testAppender() throws Exception {
        final File dir = new File(DIR);
        if (dir.exists()) {
            cleanDir(dir);
        } else {
            dir.mkdirs();
        }
        final File file = new File(FILE);
        String today = formatter.format(LocalDate.now());
        final File rolled = new File(DIR + "/test1-" + today + ".log");
        PrintStream ps = new PrintStream(new FileOutputStream(file));
        ps.println("This is a line2");
        ps.close();
        ps = new PrintStream(new FileOutputStream(rolled));
        ps.println("This is a line 1");
        ps.close();
        assertTrue("Log file does not exist", file.exists());
        assertTrue("Log file does not exist", rolled.exists());
        LoggerContext lc = Configurator.initialize("Test", CONFIG);
        final Logger logger = lc.getLogger(RollingAppenderCronOnStartupTest.class);
        logger.info("This is line 3");
        File[] files = dir.listFiles();
        assertNotNull("No files", files);
        assertEquals("Unexpected number of files. Expected 2 but found " + files.length, 2,
                files.length);
        List<String> lines = Files.readAllLines(file.toPath());
        assertEquals("Unexpected number of lines. Expected 2: Actual: " + lines.size(), 2, lines.size());
        lines = Files.readAllLines(rolled.toPath());
        assertEquals("Unexpected number of lines. Expected 1: Actual: " + lines.size(), 1, lines.size());
    }

    private static void cleanDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.stream(files).forEach(File::delete);
        }
    }

}
