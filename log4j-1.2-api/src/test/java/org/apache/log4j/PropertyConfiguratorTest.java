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
package org.apache.log4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.spi.RootLogger;
import org.apache.log4j.spi.ThrowableRenderer;
import org.apache.log4j.spi.ThrowableRendererSupport;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PropertyConfigurator}.
 */
public class PropertyConfiguratorTest {

    /**
     * Mock definition of FilterBasedTriggeringPolicy from extras companion.
     */
    public static final class FilterBasedTriggeringPolicy extends TriggeringPolicy {
        private Filter filter;

        public FilterBasedTriggeringPolicy() {}

        public Filter getFilter() {
            return filter;
        }

        public void setFilter(final Filter val) {
            filter = val;
        }
    }

    /**
     * Mock definition of FixedWindowRollingPolicy from extras companion.
     */
    public static final class FixedWindowRollingPolicy extends RollingPolicy {
        private String activeFileName;
        private String fileNamePattern;
        private int minIndex;

        public FixedWindowRollingPolicy() {
            minIndex = -1;
        }

        public String getActiveFileName() {
            return activeFileName;
        }

        public String getFileNamePattern() {
            return fileNamePattern;
        }

        public int getMinIndex() {
            return minIndex;
        }

        public void setActiveFileName(final String val) {
            activeFileName = val;
        }

        public void setFileNamePattern(final String val) {
            fileNamePattern = val;
        }

        public void setMinIndex(final int val) {
            minIndex = val;
        }
    }

    /**
     * Mock ThrowableRenderer for testThrowableRenderer. See bug 45721.
     */
    public static class MockThrowableRenderer implements ThrowableRenderer, OptionHandler {
        private boolean activated = false;
        private boolean showVersion = true;

        public MockThrowableRenderer() {}

        @Override
        public void activateOptions() {
            activated = true;
        }

        @Override
        public String[] doRender(final Throwable t) {
            return new String[0];
        }

        public boolean getShowVersion() {
            return showVersion;
        }

        public boolean isActivated() {
            return activated;
        }

        public void setShowVersion(final boolean v) {
            showVersion = v;
        }
    }

    /**
     * Mock definition of org.apache.log4j.rolling.RollingFileAppender from extras companion.
     */
    public static final class RollingFileAppender extends AppenderSkeleton {
        private RollingPolicy rollingPolicy;
        private TriggeringPolicy triggeringPolicy;
        private boolean append;

        public RollingFileAppender() {}

        @Override
        public void append(final LoggingEvent event) {}

        @Override
        public void close() {}

        public boolean getAppend() {
            return append;
        }

        public RollingPolicy getRollingPolicy() {
            return rollingPolicy;
        }

        public TriggeringPolicy getTriggeringPolicy() {
            return triggeringPolicy;
        }

        @Override
        public boolean requiresLayout() {
            return true;
        }

        public void setAppend(final boolean val) {
            append = val;
        }

        public void setRollingPolicy(final RollingPolicy policy) {
            rollingPolicy = policy;
        }

        public void setTriggeringPolicy(final TriggeringPolicy policy) {
            triggeringPolicy = policy;
        }
    }

    /**
     * Mock definition of org.apache.log4j.rolling.RollingPolicy from extras companion.
     */
    public static class RollingPolicy implements OptionHandler {
        private boolean activated = false;

        public RollingPolicy() {}

        @Override
        public void activateOptions() {
            activated = true;
        }

        public final boolean isActivated() {
            return activated;
        }
    }

    /**
     * Mock definition of TriggeringPolicy from extras companion.
     */
    public static class TriggeringPolicy implements OptionHandler {
        private boolean activated = false;

        public TriggeringPolicy() {}

        @Override
        public void activateOptions() {
            activated = true;
        }

        public final boolean isActivated() {
            return activated;
        }
    }

    private static final String FILTER1_PROPERTIES = "target/test-classes/log4j1-1.2.17/input/filter1.properties";

    private static final String CAT_A_NAME = "categoryA";

    private static final String CAT_B_NAME = "categoryB";

    private static final String CAT_C_NAME = "categoryC";

    /**
     * Test for bug 40944. Did not catch IllegalArgumentException on Properties.load and close input stream.
     *
     * @throws IOException if IOException creating properties file.
     */
    @Test
    public void testBadUnicodeEscape() throws IOException {
        final String fileName = "target/badescape.properties";
        try (final FileWriter writer = new FileWriter(fileName)) {
            writer.write("log4j.rootLogger=\\uXX41");
        }
        PropertyConfigurator.configure(fileName);
        final File file = new File(fileName);
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    /**
     * Tests configuring Log4J from an InputStream.
     *
     * @since 1.2.17
     */
    @Test
    public void testInputStream() throws IOException {
        final Path file = Paths.get(FILTER1_PROPERTIES);
        assertTrue(Files.exists(file));
        try (final InputStream inputStream = Files.newInputStream(file)) {
            PropertyConfigurator.configure(inputStream);
        }
        this.validateNested();
        LogManager.resetConfiguration();
    }

    /**
     * Test for bug 47465. configure(URL) did not close opened JarURLConnection.
     *
     * @throws IOException if IOException creating properties jar.
     */
    @Test
    public void testJarURL() throws IOException {
        final File dir = new File("output");
        dir.mkdirs();
        final File file = new File("output/properties.jar");
        try (final ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            zos.putNextEntry(new ZipEntry(LogManager.DEFAULT_CONFIGURATION_FILE));
            zos.write("log4j.rootLogger=debug".getBytes());
            zos.closeEntry();
        }
        final URL url = new URL("jar:" + file.toURI().toURL() + "!/" + LogManager.DEFAULT_CONFIGURATION_FILE);
        PropertyConfigurator.configure(url);
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    @Test
    public void testLocalVsGlobal() {
        LoggerRepository repos1, repos2;
        final Logger catA = Logger.getLogger(CAT_A_NAME);
        final Logger catB = Logger.getLogger(CAT_B_NAME);
        final Logger catC = Logger.getLogger(CAT_C_NAME);

        final Properties globalSettings = new Properties();
        globalSettings.put("log4j.logger." + CAT_A_NAME, Level.WARN.toString());
        globalSettings.put("log4j.logger." + CAT_B_NAME, Level.WARN.toString());
        globalSettings.put("log4j.logger." + CAT_C_NAME, Level.DEBUG.toString());
        PropertyConfigurator.configure(globalSettings);
        assertEquals(Level.WARN, catA.getLevel());
        assertEquals(Level.WARN, catB.getLevel());
        assertEquals(Level.DEBUG, catC.getLevel());

        assertEquals(
                Level.WARN, catA.getLoggerRepository().getLogger(CAT_A_NAME).getLevel());
        assertEquals(
                Level.WARN, catB.getLoggerRepository().getLogger(CAT_B_NAME).getLevel());
        assertEquals(
                Level.DEBUG, catC.getLoggerRepository().getLogger(CAT_C_NAME).getLevel());

        final Properties repos1Settings = new Properties();
        repos1Settings.put("log4j.logger." + CAT_A_NAME, Level.DEBUG.toString());
        repos1Settings.put("log4j.logger." + CAT_B_NAME, Level.INFO.toString());
        repos1 = new Hierarchy(new RootLogger(Level.OFF));
        new PropertyConfigurator().doConfigure(repos1Settings, repos1);
        assertEquals(Level.DEBUG, repos1.getLogger(CAT_A_NAME).getLevel());
        assertEquals(Level.INFO, repos1.getLogger(CAT_B_NAME).getLevel());

        final Properties repos2Settings = new Properties();
        repos2Settings.put("log4j.logger." + CAT_A_NAME, Level.INFO.toString());
        repos2Settings.put("log4j.logger." + CAT_B_NAME, Level.DEBUG.toString());
        repos2 = new Hierarchy(new RootLogger(Level.OFF));
        new PropertyConfigurator().doConfigure(repos2Settings, repos2);
        assertEquals(Level.INFO, repos2.getLogger(CAT_A_NAME).getLevel());
        assertEquals(Level.DEBUG, repos2.getLogger(CAT_B_NAME).getLevel());
    }

    /**
     * Tests processing of nested objects, see bug 36384.
     */
    public void testNested() {
        try {
            PropertyConfigurator.configure(FILTER1_PROPERTIES);
            this.validateNested();
        } finally {
            LogManager.resetConfiguration();
        }
    }

    /**
     * Test processing of log4j.reset property, see bug 17531.
     */
    @Test
    public void testReset() {
        final VectorAppender appender = new VectorAppender();
        appender.setName("A1");
        Logger.getRootLogger().addAppender(appender);
        final Properties properties = new Properties();
        properties.put("log4j.reset", "true");
        PropertyConfigurator.configure(properties);
        assertNull(Logger.getRootLogger().getAppender("A1"));
        LogManager.resetConfiguration();
    }

    /**
     * Test of log4j.throwableRenderer support. See bug 45721.
     */
    public void testThrowableRenderer() {
        final Properties properties = new Properties();
        properties.put("log4j.throwableRenderer", "org.apache.log4j.PropertyConfiguratorTest$MockThrowableRenderer");
        properties.put("log4j.throwableRenderer.showVersion", "false");
        PropertyConfigurator.configure(properties);
        final ThrowableRendererSupport repo = (ThrowableRendererSupport) LogManager.getLoggerRepository();
        final MockThrowableRenderer renderer = (MockThrowableRenderer) repo.getThrowableRenderer();
        LogManager.resetConfiguration();
        //        assertNotNull(renderer);
        //        assertEquals(true, renderer.isActivated());
        //        assertEquals(false, renderer.getShowVersion());
    }

    /**
     * Test for bug 40944. configure(URL) never closed opened stream.
     *
     * @throws IOException if IOException creating properties file.
     */
    @Test
    public void testURL() throws IOException {
        final File file = new File("target/unclosed.properties");
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write("log4j.rootLogger=debug");
        }
        final URL url = file.toURI().toURL();
        PropertyConfigurator.configure(url);
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    /**
     * Test for bug 40944. configure(URL) did not catch IllegalArgumentException and did not close stream.
     *
     * @throws IOException if IOException creating properties file.
     */
    @Test
    public void testURLBadEscape() throws IOException {
        final File file = new File("target/urlbadescape.properties");
        try (final FileWriter writer = new FileWriter(file)) {
            writer.write("log4j.rootLogger=\\uXX41");
        }
        final URL url = file.toURI().toURL();
        PropertyConfigurator.configure(url);
        assertTrue(file.delete());
        assertFalse(file.exists());
    }

    public void validateNested() {
        final Logger logger = Logger.getLogger("org.apache.log4j.PropertyConfiguratorTest");
        final String appenderName = "ROLLING";
        // Appender OK
        final Appender appender = logger.getAppender(appenderName);
        assertNotNull(appender);
        // Down-cast?
        //        final RollingFileAppender rfa = (RollingFileAppender) appender;
        //        assertNotNull(appenderName, rfa);
        //        final FixedWindowRollingPolicy rollingPolicy = (FixedWindowRollingPolicy) rfa.getRollingPolicy();
        //        assertEquals("filterBase-test1.log", rollingPolicy.getActiveFileName());
        //        assertEquals("filterBased-test1.%i", rollingPolicy.getFileNamePattern());
        //        assertEquals(0, rollingPolicy.getMinIndex());
        //        assertTrue(rollingPolicy.isActivated());
        //        final FilterBasedTriggeringPolicy triggeringPolicy = (FilterBasedTriggeringPolicy)
        // rfa.getTriggeringPolicy();
        //        final LevelRangeFilter filter = (LevelRangeFilter) triggeringPolicy.getFilter();
        //        assertTrue(Level.INFO.equals(filter.getLevelMin()));
    }
}
