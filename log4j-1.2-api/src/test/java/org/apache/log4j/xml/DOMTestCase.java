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
package org.apache.log4j.xml;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.VectorAppender;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SetTestProperty(key = "log4j1.compatibility", value = "true")
class DOMTestCase {

    Logger root;

    Logger logger;

    @BeforeEach
    void setUp() {
        root = Logger.getRootLogger();
        logger = Logger.getLogger(DOMTestCase.class);
    }

    @AfterEach
    void tearDown() {
        root.getLoggerRepository().resetConfiguration();
    }

    /**
     * Test checks that configureAndWatch does initial configuration, see bug 33502.
     *
     * @throws Exception if IO error.
     */
    @Test
    void testConfigureAndWatch() throws Exception {
        final URL url = requireNonNull(DOMTestCase.class.getResource("/DOMTestCase/DOMTestCase1.xml"));
        DOMConfigurator.configureAndWatch(Paths.get(url.toURI()).toString());
        assertNotNull(Logger.getRootLogger().getAppender("A1"));
    }

    /**
     * Test for bug 47465. configure(URL) did not close opened JarURLConnection.
     *
     * @throws IOException if IOException creating properties jar.
     */
    @Test
    void testJarURL() throws Exception {
        final URL url = requireNonNull(DOMTestCase.class.getResource("/DOMTestCase/defaultInit.xml"));
        final File input = Paths.get(url.toURI()).toFile();
        System.out.println(input.getAbsolutePath());
        final File configJar = new File("target/output/xml.jar");
        final File dir = new File("target/output");
        Files.createDirectories(dir.toPath());
        try (final InputStream inputStream = Files.newInputStream(input.toPath());
                final FileOutputStream out = new FileOutputStream(configJar);
                final ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry("log4j.xml"));
            int len;
            final byte[] buf = new byte[1024];
            while ((len = inputStream.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
        }
        final URL urlInJar = new URL("jar:" + configJar.toURI() + "!/log4j.xml");
        DOMConfigurator.configure(urlInJar);
        assertTrue(configJar.delete());
        assertFalse(configJar.exists());
    }

    /**
     * This test checks that the subst method of an extending class is checked when evaluating parameters. See bug 43325.
     *
     */
    @Test
    void testOverrideSubst() {
        final DOMConfigurator configurator = new DOMConfigurator();
        configurator.doConfigure(
                DOMTestCase.class.getResource("/DOMTestCase/DOMTestCase1.xml"), LogManager.getLoggerRepository());
        final String name = "A1";
        final Appender appender = Logger.getRootLogger().getAppender(name);
        assertNotNull(appender, name);
        final AppenderWrapper wrapper = (AppenderWrapper) appender;
        assertNotNull(wrapper, name);
        final org.apache.logging.log4j.core.appender.FileAppender a1 =
                (org.apache.logging.log4j.core.appender.FileAppender) wrapper.getAppender();
        assertNotNull(a1, wrapper.toString());
        final String file = a1.getFileName();
        assertNotNull(file, a1.toString());
    }

    /**
     * Tests that reset="true" on log4j:configuration element resets repository before configuration.
     *
     */
    @Test
    void testReset() {
        final VectorAppender appender = new VectorAppender();
        appender.setName("V1");
        Logger.getRootLogger().addAppender(appender);
        DOMConfigurator.configure(DOMTestCase.class.getResource("/DOMTestCase/testReset.xml"));
        assertNull(Logger.getRootLogger().getAppender("V1"));
    }

    /**
     * Test of log4j.throwableRenderer support. See bug 45721.
     */
    @Test
    @UsingStatusListener
    void testThrowableRenderer(ListStatusListener listener) {
        DOMConfigurator.configure(DOMTestCase.class.getResource("/DOMTestCase/testThrowableRenderer.xml"));
        assertThat(listener.findStatusData(org.apache.logging.log4j.Level.WARN))
                .anySatisfy(status -> assertThat(status.getMessage().getFormattedMessage())
                        .contains("Log4j 1 throwable renderers are not supported"));
    }

    @Test
    @SetTestProperty(key = "log4j1.compatibility", value = "false")
    void when_compatibility_disabled_configurator_is_no_op() {
        final Logger rootLogger = Logger.getRootLogger();
        final Logger logger = Logger.getLogger("org.apache.log4j.xml");
        assertThat(logger.getLevel()).isNull();
        final URL configURL = DOMTestCase.class.getResource("/DOMTestCase/DOMTestCase1.xml");
        DOMConfigurator.configure(configURL);

        assertThat(rootLogger.getAppender("A1")).isNull();
        assertThat(rootLogger.getAppender("A2")).isNull();
        assertThat(rootLogger.getLevel()).isNotEqualTo(Level.TRACE);

        assertThat(logger.getAppender("A1")).isNull();
        assertThat(logger.getLevel()).isNull();
    }
}
