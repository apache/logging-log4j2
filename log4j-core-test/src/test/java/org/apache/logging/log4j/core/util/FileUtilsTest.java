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
package org.apache.logging.log4j.core.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the FileUtils class.
 */
public class FileUtilsTest {

    private static final String LOG4J_CONFIG_WITH_PLUS = "log4j+config+with+plus+characters.xml";

    @Test
    public void testFileFromUriWithPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/log4j+config+with+plus+characters.xml";
        final URI uri = new URI(config);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue(file.exists(), "file exists");
    }

    @Test
    public void testAbsoluteFileFromUriWithPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/log4j+config+with+plus+characters.xml";
        final URI uri = new File(config).toURI();
        final File file = FileUtils.fileFromUri(uri);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue(file.exists(), "file exists");
    }

    @Test
    public void testAbsoluteFileFromUriWithSpacesInName() throws Exception {
        final String config = "target/test-classes/s p a c e s/log4j+config+with+plus+characters.xml";
        final URI uri = new File(config).toURI();
        final File file = FileUtils.fileFromUri(uri);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue(file.exists(), "file exists");
    }

    @Test
    public void testAbsoluteFileFromJBossVFSUri() throws Exception {
        final String config = "target/test-classes/log4j+config+with+plus+characters.xml";
        final String uriStr = new File(config).toURI().toString().replaceAll("^file:", "vfsfile:");
        assertTrue(uriStr.startsWith("vfsfile:"));
        final URI uri = URI.create(uriStr);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue(file.exists(), "file exists");
    }

    @Test
    public void testFileFromUriWithSpacesAndPlusCharactersInName() throws Exception {
        final String config = "target/test-classes/s%20p%20a%20c%20e%20s/log4j%2Bconfig%2Bwith%2Bplus%2Bcharacters.xml";
        final URI uri = new URI(config);
        final File file = FileUtils.fileFromUri(uri);
        assertEquals(LOG4J_CONFIG_WITH_PLUS, file.getName());
        assertTrue(file.exists(), "file exists");
    }

    @Nested
    class TestMkdir {
        @TempDir
        File testDir;

        @BeforeEach
        public void deleteTestDir() throws IOException {
            org.apache.commons.io.FileUtils.deleteDirectory(testDir);
        }

        @Test
        public void testMkdirDoesntExistDontCreate() {
            assertThrows(IOException.class, () -> FileUtils.mkdir(testDir, false));
        }

        @Test
        public void testMkdirFileAlreadyExistsNotDir() throws IOException {
            Files.createFile(testDir.toPath());
            assertThrows(IOException.class, () -> FileUtils.mkdir(testDir, true));
            Files.delete(testDir.toPath());
        }

        @Test
        public void testMkdirConcurrent() throws InterruptedException {
            final List<Thread> threads = new ArrayList<>();
            final AtomicBoolean anyThreadThrows = new AtomicBoolean(false);
            for (int i = 0; i < 10000; i++) {
                threads.add(new Thread(() -> {
                    try {
                        FileUtils.mkdir(testDir, true);
                    } catch (IOException e) {
                        anyThreadThrows.set(true);
                    }
                }));
            }

            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }

            assertFalse(anyThreadThrows.get());
        }
    }
}
