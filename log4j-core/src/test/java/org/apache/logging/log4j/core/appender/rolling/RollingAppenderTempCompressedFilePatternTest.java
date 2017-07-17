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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

/**
 * LOG4J2-1766.
 */
public class RollingAppenderTempCompressedFilePatternTest {

    private static final String CONFIG = "log4j-rolling-gz-tmp-compress.xml";

    private static final String DIR = "target/rolling2";
    private static final String DIR_TMP = "target/rolling-tmp";

    public static LoggerContextRule loggerContextRule = LoggerContextRule
            .createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFoldersRule(DIR, DIR_TMP);

    private Logger logger;

    @Before
    public void setUp() throws Exception {
        // Disable this test on MacOS. FileWatcher isn't fast enough to work properly.
        Assume.assumeTrue(!SystemUtils.IS_OS_MAC_OSX);
        this.logger = loggerContextRule.getLogger(RollingAppenderTempCompressedFilePatternTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        final File dirTmp = new File(DIR_TMP);
        dirTmp.mkdirs();
        try (final WatchService watcher = FileSystems.getDefault().newWatchService()) {
            WatchKey key = dirTmp.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE);

            final List<String> messages = new ArrayList<>();
            for (int i = 0; i < 500; ++i) {
                final String message = "This is test message number " + i;
                messages.add(message);
                logger.debug(message);
                if (i % 100 == 0) {
                    Thread.sleep(500);
                }
            }
            if (!loggerContextRule.getLoggerContext().stop(30, TimeUnit.SECONDS)) {
                System.err.println("Could not stop cleanly " + loggerContextRule + " for " + this);
            }
            final File dir = new File(DIR);
            assertTrue("Directory not created", dir.exists());
            final File[] files = dir.listFiles();
            assertNotNull(files);
            int gzippedFiles = 0;
            for (final File file : files) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                InputStream in = null;
                final FileExtension ext = FileExtension.lookupForFile(file.getName());
                try {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        if (ext != null) {
                            gzippedFiles++;
                            try {
                                in = new CompressorStreamFactory().createCompressorInputStream(ext.name().toLowerCase(),
                                        fis);
                            } catch (final CompressorException ce) {
                                ce.printStackTrace();
                                fail("Error creating intput stream from " + file.toString() + ": " + ce.getMessage());
                            }
                        } else {
                            in = new FileInputStream(file);
                        }
                        assertNotNull("No input stream for " + file.getName(), in);
                        try {
                            IOUtils.copy(in, baos);
                        } catch (final Exception ex) {
                            ex.printStackTrace();
                            fail("Unable to decompress " + file.getAbsolutePath());
                        }
                    }
                } finally {
                    Closer.close(in);
                }
                final String text = new String(baos.toByteArray(), Charset.defaultCharset());
                final String[] lines = text.split("[\\r\\n]+");
                for (final String line : lines) {
                    messages.remove(line);
                }
            }
            assertTrue("Log messages lost : " + messages.size(), messages.isEmpty());
            assertTrue("Files not rolled : " + files.length, files.length > 2);
            assertTrue("Files gzipped not rolled : " + gzippedFiles, gzippedFiles > 0);

            int temporaryFilesCreated = 0;
            key = watcher.take();

            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                final Path filename = ev.context();
                if (filename.toString().endsWith(".tmp")) {
                    temporaryFilesCreated++;
                }
            }
            assertTrue("No temporary file created during compression", temporaryFilesCreated > 0);
            assertTrue("Temporarys file created not equals to compressed files " + temporaryFilesCreated + "/"
                    + gzippedFiles, gzippedFiles == temporaryFilesCreated);
        }
    }
}
