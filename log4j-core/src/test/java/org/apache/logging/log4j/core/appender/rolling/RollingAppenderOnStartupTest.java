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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class RollingAppenderOnStartupTest {

    private static final String SOURCE = "src/test/resources/__files";
    private static final String DIR = "target/onStartup";
    private static final String CONFIG = "log4j-rollOnStartup.xml";
    private static final String FILENAME = "onStartup.log";
    private static final String PREFIX = "This is test message number ";
    private static final String ROLLED = "onStartup-";

    private static LoggerContext loggerContext;

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (Files.exists(Paths.get("target/onStartup"))) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
                for (final Path path : directoryStream) {
                    Files.delete(path);
                }
                Files.delete(Paths.get(DIR));
            }
        }
        Files.createDirectory(new File(DIR).toPath());
        Path target = Paths.get(DIR, FILENAME);
        Files.copy(Paths.get(SOURCE, FILENAME), target, StandardCopyOption.COPY_ATTRIBUTES);
        FileTime newTime = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));
        Files.getFileAttributeView(target, BasicFileAttributeView.class).setTimes(newTime, newTime, newTime);
    }

    @Test
    public void performTest() throws Exception {
        boolean rolled = false;
        loggerContext = Configurator.initialize("Test", CONFIG);
        final Logger logger = loggerContext.getLogger(RollingAppenderOnStartupTest.class);
        for (int i = 3; i < 10; ++i) {
            logger.debug(PREFIX + i);
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                if (path.toFile().getName().startsWith(ROLLED)) {
                    rolled = true;
                    List<String> lines = Files.readAllLines(path);
                    assertTrue("No messages in " + path.toFile().getName(), lines.size() > 0);
                    assertTrue("Missing message for " + path.toFile().getName(),
                            lines.get(0).startsWith(PREFIX + "1"));
                }
            }
        }
        assertTrue("File did not roll", rolled);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        Configurator.shutdown(loggerContext);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
            for (final Path path : directoryStream) {
                Files.delete(path);
            }
        }
        Files.delete(Paths.get(DIR));
    }

}
