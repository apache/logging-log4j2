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
package org.apache.logging.log4j.core.util;

import static org.junit.Assert.assertNotNull;

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
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.Assume;
import org.junit.Test;

/**
 * Test the WatchManager
 */
public class WatchManagerTest {

    private final String testFile = "target/testWatchFile";
    private final String originalFile = "target/test-classes/log4j-test1.xml";
    private final String newFile = "target/test-classes/log4j-test1.yaml";

    private static final boolean IS_WINDOWS = PropertiesUtil.getProperties().isOsWindows();

    @Test
    public void testWatchManager() throws Exception {
        Assume.assumeFalse(IS_WINDOWS);
        final ConfigurationScheduler scheduler = new ConfigurationScheduler();
        scheduler.incrementScheduledItems();
        final WatchManager watchManager = new WatchManager(scheduler);
        watchManager.setIntervalSeconds(1);
        scheduler.start();
        watchManager.start();
        try {
            final File sourceFile = new File(originalFile);
            final FileOutputStream targetStream = new FileOutputStream(testFile);
            final File updateFile = new File(newFile);
            Path source = Paths.get(sourceFile.toURI());
            Files.copy(source, targetStream);
            final File targetFile = new File(testFile);
            final BlockingQueue<File> queue = new LinkedBlockingQueue<>();
            watchManager.watchFile(targetFile, new TestWatcher(queue));
            Thread.sleep(1000);
            source = Paths.get(updateFile.toURI());
            Files.copy(source, Paths.get(targetFile.toURI()), StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(1000);
            final File f = queue.poll(1, TimeUnit.SECONDS);
            assertNotNull("File change not detected", f);
        } finally {
            watchManager.stop();
            scheduler.stop();
        }
    }

    private class TestWatcher implements FileWatcher {

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
