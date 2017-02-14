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

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 *
 */
@RunWith(Parameterized.class)
public class RollingAppenderSizeTest {

    @Rule
    public RuleChain chain;

    private static final String DIR = "target/rolling1";

    private final String fileExtension;

    private Logger logger;

    private final boolean createOnDemand;

    @Parameterized.Parameters(name = "{0} \u2192 {1} (createOnDemand = {2})")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                // @formatter:off
               {"log4j-rolling-gz-lazy.xml", ".gz", true},
               {"log4j-rolling-gz.xml", ".gz", false},
               {"log4j-rolling-zip-lazy.xml", ".zip", true},
               {"log4j-rolling-zip.xml", ".zip", false},
                // Apache Commons Compress
               {"log4j-rolling-bzip2-lazy.xml", ".bz2", true},
               {"log4j-rolling-bzip2.xml", ".bz2", false},
               {"log4j-rolling-deflate-lazy.xml", ".deflate", true},
               {"log4j-rolling-deflate.xml", ".deflate", false},
               {"log4j-rolling-pack200-lazy.xml", ".pack200", true},
               {"log4j-rolling-pack200.xml", ".pack200", false},
               {"log4j-rolling-xz-lazy.xml", ".xz", true},
               {"log4j-rolling-xz.xml", ".xz", false},
                });
                // @formatter:on
    }

    private final LoggerContextRule loggerContextRule;

    public RollingAppenderSizeTest(final String configFile, final String fileExtension, final boolean createOnDemand) {
        this.fileExtension = fileExtension;
        this.createOnDemand = createOnDemand;
        this.loggerContextRule = LoggerContextRule.createShutdownTimeoutLoggerContextRule(configFile);
        this.chain = loggerContextRule.withCleanFoldersRule(DIR);
    }

    @Before
    public void setUp() throws Exception {
        this.logger = this.loggerContextRule.getLogger(RollingAppenderSizeTest.class.getName());
    }

    @Test
    public void testIsCreateOnDemand() {
        final RollingFileAppender rfAppender = loggerContextRule.getRequiredAppender("RollingFile",
                RollingFileAppender.class);
        final RollingFileManager manager = rfAppender.getManager();
        Assert.assertNotNull(manager);
        Assert.assertEquals(createOnDemand, manager.isCreateOnDemand());
    }

    @Test
    public void testAppender() throws Exception {
        final Path path = Paths.get(DIR, "rollingtest.log");
        if (Files.exists(path) && createOnDemand) {
            Assert.fail(String.format("Unexpected file: %s (%s bytes)", path, Files.getAttribute(path, "size")));
        }
        for (int i = 0; i < 500; ++i) {
            logger.debug("This is test message number " + i);
        }
        try {
            Thread.sleep(100);
        } catch (final InterruptedException ie) {
            // Ignore the error.
        }

        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(fileExtension))))));

        final FileExtension ext = FileExtension.lookup(fileExtension);
        if (ext == null || FileExtension.ZIP == ext || FileExtension.PACK200 == ext) {
            return; // Apache Commons Compress cannot deflate zip? TODO test decompressing these formats
        }
        // Stop the context to make sure all files are compressed and closed. Trying to remedy failures in CI builds.
        if (!loggerContextRule.getLoggerContext().stop(30, TimeUnit.SECONDS)) {
            System.err.println("Could not stop cleanly " + loggerContextRule + " for " + this);
        }
        for (final File file : files) {
            if (file.getName().endsWith(fileExtension)) {
                CompressorInputStream in = null;
                try (FileInputStream fis = new FileInputStream(file)) {
                    try {
                        in = new CompressorStreamFactory().createCompressorInputStream(ext.name().toLowerCase(), fis);
                    } catch (final CompressorException ce) {
                        ce.printStackTrace();
                        fail("Error creating intput stream from " + file.toString() + ": " + ce.getMessage());
                    }
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    assertNotNull("No input stream for " + file.getName(), in);
                    try {
                        IOUtils.copy(in, baos);
                    } catch (final Exception ex) {
                        ex.printStackTrace();
                        fail("Unable to decompress " + file.getAbsolutePath());
                    }
                    final String text = new String(baos.toByteArray(), Charset.defaultCharset());
                    final String[] lines = text.split("[\\r\\n]+");
                    for (final String line : lines) {
                        assertTrue(line.contains(
                                "DEBUG o.a.l.l.c.a.r.RollingAppenderSizeTest [main] This is test message number"));
                    }
                } finally {
                    Closer.close(in);
                }
            }
        }
    }
}
