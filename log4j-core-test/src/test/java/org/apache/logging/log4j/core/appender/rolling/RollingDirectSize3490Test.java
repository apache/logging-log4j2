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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * This test attempts to validate that logging rolls after the max files are already written
 * will succeed on a restart.
 */
public class RollingDirectSize3490Test implements RolloverListener {

    private static final String CONFIG = "log4j-rolling-3490.xml";
    private static final String[] set1 = {"This is file 1"};
    private static final String LINE_2 = "This is file 2\n";

    // Note that the path is hardcoded in the configuration!
    private static final String DIR = "target/rolling-3490";

    private boolean rolloverTriggered = false;

    @BeforeAll
    public static void clean() throws Exception {
        final File dir = new File(DIR);
        if (dir.exists()) {
            final File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            dir.delete();
        }
    }

    @Test
    public void rolloverTest() throws Exception {
        final File parent = new File(DIR);
        parent.mkdirs();
        final Path app1 = new File(parent, "app-21.log").toPath();
        Files.write(app1, Arrays.asList(set1), StandardOpenOption.CREATE_NEW);
        final List<String> lines = new ArrayList<>();
        for (int count = 0; count < 1024; count += LINE_2.length()) {
            lines.add(LINE_2);
        }
        final File file2 = new File(parent, "app-22.log");
        final Path app2 = file2.toPath();
        Files.write(app2, lines, StandardOpenOption.CREATE_NEW);
        final LoggerContext context =
                Configurator.initialize("TestConfig", this.getClass().getClassLoader(), CONFIG);
        final RollingFileAppender app = context.getConfiguration().getAppender("RollingFile");
        app.getManager().addRolloverListener(this);
        final Logger logger = context.getLogger("Test");
        logger.info("Trigger rollover");
        assertTrue(rolloverTriggered, "Rollover was not triggered");
    }

    @Override
    public void rolloverTriggered(final String fileName) {}

    @Override
    public void rolloverComplete(final String fileName) {
        assertTrue(fileName.endsWith("app-22.log"), "File does not end with correct suffix");
        rolloverTriggered = true;
    }
}
