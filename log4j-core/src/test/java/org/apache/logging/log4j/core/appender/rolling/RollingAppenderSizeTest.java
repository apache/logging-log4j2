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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Closer;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.apache.logging.log4j.hamcrest.Descriptors.that;
import static org.apache.logging.log4j.hamcrest.FileMatchers.hasName;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.*;

/**
 *
 */
@RunWith(Parameterized.class)
public class RollingAppenderSizeTest {

    private static final String DIR = "target/rolling1";

    private final String fileExtension;

    private Logger logger;

    @Parameterized.Parameters(name = "{0} \u2192 {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                // @formatter:off
                {"log4j-rolling-gz.xml", ".gz"}, //
                {"log4j-rolling-zip.xml", ".zip"}, //
                // Apache Commons Compress
                {"log4j-rolling-bzip2.xml", ".bz2"}, //
                {"log4j-rolling-deflate.xml", ".deflate"}, //
                {"log4j-rolling-pack200.xml", ".pack200"}, //
                {"log4j-rolling-xz.xml", ".xz"},});
                // @formatter:on
    }

    @Rule
    public LoggerContextRule loggerContextRule;

    public RollingAppenderSizeTest(final String configFile, final String fileExtension) {
        this.fileExtension = fileExtension;
        this.loggerContextRule = new LoggerContextRule(configFile);
    }

    @BeforeClass
    public static void beforeClass() {
        deleteDir();
    }

    @AfterClass
    public static void afterClass() {
        deleteDir();
    }

    @Before
    public void setUp() throws Exception {
        this.logger = this.loggerContextRule.getLogger(RollingAppenderSizeTest.class.getName());
    }

    @Test
    public void testAppender() throws Exception {
        for (int i = 0; i < 100; ++i) {
            logger.debug("This is test message number " + i);
        }
        final File dir = new File(DIR);
        assertTrue("Directory not created", dir.exists() && dir.listFiles().length > 0);
        final File[] files = dir.listFiles();
        assertNotNull(files);
        assertThat(files, hasItemInArray(that(hasName(that(endsWith(fileExtension))))));

        final DefaultRolloverStrategy.FileExtensions ext = DefaultRolloverStrategy.FileExtensions.lookup(fileExtension);
        if (ext == null || DefaultRolloverStrategy.FileExtensions.ZIP == ext
                || DefaultRolloverStrategy.FileExtensions.PACK200 == ext) {
            return; // Apache Commons Compress cannot deflate zip? TODO test decompressing these formats
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
                    IOUtils.copy(in, baos);
                    final String text = new String(baos.toByteArray(), Charset.defaultCharset());
                    final String[] lines = text.split("[\\r\\n]+");
                    for (final String line : lines) {
                        assertTrue(line
                                .contains("DEBUG o.a.l.l.c.a.r.RollingAppenderSizeTest [main] This is test message number"));
                    }
                } finally {
                    Closer.close(in);
                }
            }
        }
        deleteDir();
    }

    private static void deleteDir() {
        if (Files.exists(Paths.get(DIR))) {
            String fileName = null;
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(DIR))) {
                for (final Path path : directoryStream) {
                    fileName = path.toFile().getName();
                    Files.delete(path);
                }
                Files.delete(Paths.get(DIR));
            } catch (final IOException ioe) {
                fail("Unable to delete " + fileName + " due to " + ioe.getClass().getSimpleName() + ": " +
                        ioe.getMessage());
            }
        }
    }
}
