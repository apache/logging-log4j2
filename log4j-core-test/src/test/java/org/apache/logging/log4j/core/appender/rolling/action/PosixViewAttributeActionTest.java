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
package org.apache.logging.log4j.core.appender.rolling.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.test.BasicConfigurationFactory;
import org.apache.logging.log4j.core.util.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the {@code PosixViewAttributeAction} class.
 */
class PosixViewAttributeActionTest {

    @BeforeAll
    static void beforeClass() {
        assumeTrue(FileUtils.isFilePosixAttributeViewSupported());
    }

    @Test
    void testSymbolicLinksAreNotFollowed(@TempDir final Path tempDir) throws Exception {
        // A file outside the scanned directory, that the action must not touch.
        final Path outsider = tempDir.resolve("outsider.txt");
        Files.write(outsider, "secret".getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(outsider, PosixFilePermissions.fromString("rw-------"));

        final Path baseDir = Files.createDirectory(tempDir.resolve("logs"));
        final Path regularFile = baseDir.resolve("app-1.log");
        Files.write(regularFile, "log".getBytes(StandardCharsets.UTF_8));
        Files.setPosixFilePermissions(regularFile, PosixFilePermissions.fromString("rw-------"));
        Files.createSymbolicLink(baseDir.resolve("app-2.log"), outsider);

        final Configuration config = new BasicConfigurationFactory().new BasicConfiguration();
        final PosixViewAttributeAction action = PosixViewAttributeAction.newBuilder()
                .setBasePath(baseDir.toString())
                .setFollowLinks(false)
                .setMaxDepth(1)
                .setPathConditions(PathCondition.EMPTY_ARRAY)
                .setConfiguration(config)
                .setFilePermissionsString("rw-rw-rw-")
                .build();

        action.execute();

        assertEquals(
                "rw-rw-rw-",
                PosixFilePermissions.toString(Files.getPosixFilePermissions(regularFile)),
                "regular file should have been updated");
        assertEquals(
                "rw-------",
                PosixFilePermissions.toString(Files.getPosixFilePermissions(outsider)),
                "symbolic link target should have been left alone");
    }
}
