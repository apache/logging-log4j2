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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.condition.OS;

/**
 * Test the WatchManager
 */
@DisabledOnOs(OS.WINDOWS)
@EnabledIfSystemProperty(named = "WatchManagerTest.forceRun", matches = "true")
class WatchManagerTest {

    private final String testFile = "target/testWatchFile";
    private final String originalFile = "target/test-classes/log4j-test1.xml";
    private final String newFile = "target/test-classes/log4j-test1.yaml";

    private ConfigurationScheduler scheduler;
    private WatchManager watchManager;

    @BeforeEach
    void setUp() {
        scheduler = new ConfigurationScheduler();
        scheduler.incrementScheduledItems();
        watchManager = new WatchManager(scheduler);
        watchManager.setIntervalSeconds(1);
        scheduler.start();
        watchManager.start();
    }

    @AfterEach
    void tearDown() {
        watchManager.stop();
        scheduler.stop();
        watchManager = null;
        scheduler = null;
    }

    @Test
    void testWatchManager() throws Exception {
        final File sourceFile = new File(originalFile);
        Path source = Paths.get(sourceFile.toURI());
        try (final FileOutputStream targetStream = new FileOutputStream(testFile)) {
            Files.copy(source, targetStream);
        }
        final File updateFile = new File(newFile);
        final File targetFile = new File(testFile);
        final BlockingQueue<File> queue = new LinkedBlockingQueue<>();
        watchManager.watchFile(targetFile, new TestWatcher(queue));
        Thread.sleep(1000);
        source = Paths.get(updateFile.toURI());
        Files.copy(source, Paths.get(targetFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
        Thread.sleep(1000);
        final File f = queue.poll(1, TimeUnit.SECONDS);
        assertNotNull(f, "File change not detected");
    }

    @Test
    void testWatchManagerReset() throws Exception {
        final File sourceFile = new File(originalFile);
        Path source = Paths.get(sourceFile.toURI());
        try (final FileOutputStream targetStream = new FileOutputStream(testFile)) {
            Files.copy(source, targetStream);
        }
        final File updateFile = new File(newFile);
        final File targetFile = new File(testFile);
        final BlockingQueue<File> queue = new LinkedBlockingQueue<>();
        watchManager.watchFile(targetFile, new TestWatcher(queue));
        watchManager.stop();
        Thread.sleep(1000);
        source = Paths.get(updateFile.toURI());
        Files.copy(source, Paths.get(targetFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
        watchManager.reset();
        watchManager.start();
        Thread.sleep(1000);
        final File f = queue.poll(1, TimeUnit.SECONDS);
        assertNull(f, "File change detected");
    }

    @Test
    void testWatchManagerResetFile() throws Exception {
        final File sourceFile = new File(originalFile);
        Path source = Paths.get(sourceFile.toURI());
        try (final FileOutputStream targetStream = new FileOutputStream(testFile)) {
            Files.copy(source, targetStream);
        }
        final File updateFile = new File(newFile);
        final File targetFile = new File(testFile);
        final BlockingQueue<File> queue = new LinkedBlockingQueue<>();
        watchManager.watchFile(targetFile, new TestWatcher(queue));
        watchManager.stop();
        Thread.sleep(1000);
        source = Paths.get(updateFile.toURI());
        Files.copy(source, Paths.get(targetFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
        watchManager.reset(targetFile);
        watchManager.start();
        Thread.sleep(1000);
        final File f = queue.poll(1, TimeUnit.SECONDS);
        assertNull(f, "File change detected");
    }

    /**
     * Verify the
     */
    @Test
    void testWatchManagerCallsWatcher() {
        Watcher watcher = mock(Watcher.class);
        when(watcher.isModified()).thenReturn(false);
        watchManager.watch(new Source(ConfigurationSource.NULL_SOURCE), watcher);
        verify(watcher, timeout(2000)).isModified();
        verify(watcher, never()).modified();
        when(watcher.isModified()).thenReturn(true);
        clearInvocations(watcher);
        verify(watcher, timeout(2000)).isModified();
        verify(watcher).modified();
    }

    private static class TestWatcher implements FileWatcher {

        private final Queue<File> queue;

        public TestWatcher(final Queue<File> queue) {
            this.queue = queue;
        }

        @Override
        public void fileModified(final File file) {
            System.out.println(file.toString() + " was modified");
            queue.add(file);
        }
    }
}
