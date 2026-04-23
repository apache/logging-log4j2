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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.core.config.NullConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RollingFileManagerCreateOnDemandTest {
    @Test
    void testCreateOnDemandDoesNotCreateDirectoryOrFile(@TempDir Path tempDir) throws Exception {
        Path logDir = tempDir.resolve("onDemand");
        String logFile = logDir.resolve("test.log").toString();
        File dir = logDir.toFile();
        File file = new File(logFile);
        assertFalse(dir.exists(), "Directory should not exist before logging");
        assertFalse(file.exists(), "File should not exist before logging");

        RollingFileManager manager = RollingFileManager.getFileManager(
                logFile,
                logFile + ".%d{yyyy-MM-dd}",
                true,
                false,
                NoOpTriggeringPolicy.INSTANCE,
                DefaultRolloverStrategy.newBuilder().build(),
                null,
                PatternLayout.createDefaultLayout(),
                0,
                true,
                true,
                null,
                null,
                null,
                new NullConfiguration());
        assertNotNull(manager);
        // Directory and file should still not exist
        assertFalse(dir.exists(), "Directory should not exist after manager creation");
        assertFalse(file.exists(), "File should not exist after manager creation");

        // Log a message
        manager.writeToDestination("Hello Log4j2".getBytes(), 0, "Hello Log4j2".length());
        manager.close();

        // Now directory and file should exist
        assertTrue(dir.exists(), "Directory should exist after first log event");
        assertTrue(file.exists(), "File should exist after first log event");
        String content = new String(Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("Hello Log4j2"));
    }
}
