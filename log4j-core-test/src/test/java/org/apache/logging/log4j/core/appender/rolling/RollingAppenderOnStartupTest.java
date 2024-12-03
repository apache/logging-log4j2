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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.test.junit.TempLoggingDir;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@UsingStatusListener
public class RollingAppenderOnStartupTest {

    private static final String SOURCE = "src/test/resources/__files";
    private static final String FILENAME = "onStartup.log";
    private static final String PREFIX = "This is test message number ";
    private static final String ROLLED = "onStartup-";

    @TempLoggingDir
    private static Path loggingPath;

    @BeforeAll
    public static void setup() throws Exception {
        final Path target = loggingPath.resolve(FILENAME);
        Files.copy(Paths.get(SOURCE, FILENAME), target, StandardCopyOption.REPLACE_EXISTING);
        final FileTime newTime = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));
        final BasicFileAttributeView attrs = Files.getFileAttributeView(target, BasicFileAttributeView.class);
        attrs.setTimes(newTime, newTime, newTime);
        /*
         * POSIX does not define a file creation timestamp.
         * Depending on the system `creationTime` might be:
         *  * 0,
         *  * the last modification time
         *  * or the time the file was actually created.
         *
         * This test fails if the latter occurs, since the file is created after the JVM.
         */
        final FileTime creationTime = attrs.readAttributes().creationTime();
        assumeTrue(creationTime.equals(newTime) || creationTime.toMillis() == 0L);
    }

    @Test
    @LoggerContextSource
    public void performTest(final LoggerContext loggerContext) throws Exception {
        boolean rolled = false;
        final Logger logger = loggerContext.getLogger(RollingAppenderOnStartupTest.class);
        for (int i = 3; i < 10; ++i) {
            logger.debug(PREFIX + i);
        }
        try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(loggingPath)) {
            for (final Path path : directoryStream) {
                if (path.toFile().getName().startsWith(ROLLED)) {
                    rolled = true;
                    final List<String> lines = Files.readAllLines(path);
                    assertTrue(
                            lines.size() > 0, "No messages in " + path.toFile().getName());
                    assertTrue(
                            lines.get(0).startsWith(PREFIX + "1"),
                            "Missing message for " + path.toFile().getName());
                }
            }
        }
        assertTrue(rolled, "File did not roll");
    }
}
