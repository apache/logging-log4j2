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
package org.apache.log4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.ListAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.bridge.AppenderAdapter.Adapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.OutputStreamManager;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.io.TempDir;

abstract class AbstractLog4j1ConfigurationTest {

    @TempDir
    File tempDir;

    abstract Configuration getConfiguration(String configResourcePrefix) throws URISyntaxException, IOException;

    protected InputStream getResourceAsStream(final String configResource) {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(configResource);
        assertNotNull(is);
        return is;
    }

    protected LoggerContext configure(final String configResourcePrefix) throws URISyntaxException, IOException {
        Configurator.reconfigure(getConfiguration(configResourcePrefix));
        return (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
    }

    void testConsoleCapitalization() throws Exception {
        final Configuration config = getConfiguration("config-1.2/log4j-capitalization");
        final Appender capitalized = config.getAppender("ConsoleCapitalized");
        assertNotNull(capitalized);
        assertEquals(ConsoleAppender.class, capitalized.getClass());
        final Appender javaStyle = config.getAppender("ConsoleJavaStyle");
        assertNotNull(javaStyle);
        assertEquals(ConsoleAppender.class, javaStyle.getClass());
        testConsoleAppender((ConsoleAppender) capitalized, (ConsoleAppender) javaStyle);
    }

    private void testConsoleAppender(final ConsoleAppender expected, final ConsoleAppender actual) {
        assertEquals(expected.getImmediateFlush(), actual.getImmediateFlush(), "immediateFlush");
        assertEquals(expected.getTarget(), actual.getTarget(), "target");
        assertEquals(expected.getLayout().getClass(), actual.getLayout().getClass(), "layoutClass");
        if (expected.getLayout() instanceof PatternLayout) {
            patternLayoutEquals((PatternLayout) expected.getLayout(), (PatternLayout) actual.getLayout());
        }
    }

    private void patternLayoutEquals(final PatternLayout expected, final PatternLayout actual) {
        assertEquals(expected.getCharset(), actual.getCharset());
        assertEquals(expected.getConversionPattern(), actual.getConversionPattern());
    }

    private Layout<?> testConsole(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final String name = "Console";
        final ConsoleAppender appender = configuration.getAppender(name);
        assertNotNull(
                appender, "Missing appender '" + name + "' in configuration " + configResource + " → " + configuration);
        assertTrue(getFollowProperty(appender), "follow");
        assertEquals(Target.SYSTEM_ERR, appender.getTarget());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        // immediateFlush is always true in Log4j 2.x
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

    void testConsoleTtccLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-TTCCLayout");
        assertEquals("%d{ISO8601}{CET} %p - %m%n", layout.getConversionPattern());
    }

    void testRollingFileAppender() throws Exception {
        testRollingFileAppender("config-1.2/log4j-RollingFileAppender");
    }

    void testDailyRollingFileAppender() throws Exception {
        testDailyRollingFileAppender("config-1.2/log4j-DailyRollingFileAppender");
    }

    void testRollingFileAppenderWithProperties() throws Exception {
        testRollingFileAppender("config-1.2/log4j-RollingFileAppender-with-props");
    }

    void testSystemProperties1() throws Exception {
        final String tempFileName = System.getProperty("java.io.tmpdir") + "/hadoop.log";
        final Path tempFilePath = new File(tempFileName).toPath();
        Files.deleteIfExists(tempFilePath);
        final Configuration configuration = getConfiguration("config-1.2/log4j-system-properties-1");
        try {
            final RollingFileAppender appender = configuration.getAppender("RFA");
            assertFalse(getAppendProperty(appender), "append");
            assertEquals(1000, appender.getManager().getBufferSize(), "bufferSize");
            assertFalse(appender.getImmediateFlush(), "immediateFlush");
            final DefaultRolloverStrategy rolloverStrategy =
                    (DefaultRolloverStrategy) appender.getManager().getRolloverStrategy();
            assertEquals(16, rolloverStrategy.getMaxIndex());
            final CompositeTriggeringPolicy ctp = appender.getTriggeringPolicy();
            final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
            assertEquals(1, triggeringPolicies.length);
            final TriggeringPolicy tp = triggeringPolicies[0];
            assertInstanceOf(SizeBasedTriggeringPolicy.class, tp, tp.getClass().getName());
            final SizeBasedTriggeringPolicy sbtp = (SizeBasedTriggeringPolicy) tp;
            assertEquals(20 * 1024 * 1024, sbtp.getMaxFileSize());
            appender.stop(10, TimeUnit.SECONDS);
            assertEquals(tempFileName, appender.getFileName());
        } finally {
            configuration.start();
            configuration.stop();
            Files.deleteIfExists(tempFilePath);
        }
    }

    void testSystemProperties2() throws Exception {
        final Configuration configuration = getConfiguration("config-1.2/log4j-system-properties-2");
        final RollingFileAppender appender = configuration.getAppender("RFA");
        final String tmpDir = System.getProperty("java.io.tmpdir");
        assertEquals(tmpDir + "/hadoop.log", appender.getFileName());
        appender.stop(10, TimeUnit.SECONDS);
        // Try to clean up
        Path path = new File(appender.getFileName()).toPath();
        Files.deleteIfExists(path);
        path = new File("${java.io.tmpdir}").toPath();
        Files.deleteIfExists(path);
    }

    private void testRollingFileAppender(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final Appender appender = configuration.getAppender("RFA");
        assertNotNull(appender);
        assertEquals("RFA", appender.getName());
        assertInstanceOf(
                RollingFileAppender.class, appender, appender.getClass().getName());
        final RollingFileAppender rfa = (RollingFileAppender) appender;

        assertInstanceOf(
                DefaultRolloverStrategy.class, rfa.getManager().getRolloverStrategy(), "defaultRolloverStrategy");
        assertFalse(((DefaultRolloverStrategy) rfa.getManager().getRolloverStrategy()).isUseMax(), "rolloverStrategy");
        assertFalse(getAppendProperty(rfa), "append");
        assertEquals(1000, rfa.getManager().getBufferSize(), "bufferSize");
        assertFalse(rfa.getImmediateFlush(), "immediateFlush");
        assertEquals("target/hadoop.log", rfa.getFileName());
        assertEquals("target/hadoop.log.%i", rfa.getFilePattern());
        final TriggeringPolicy triggeringPolicy = rfa.getTriggeringPolicy();
        assertNotNull(triggeringPolicy);
        assertInstanceOf(
                CompositeTriggeringPolicy.class,
                triggeringPolicy,
                triggeringPolicy.getClass().getName());
        final CompositeTriggeringPolicy ctp = (CompositeTriggeringPolicy) triggeringPolicy;
        final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
        assertEquals(1, triggeringPolicies.length);
        final TriggeringPolicy tp = triggeringPolicies[0];
        assertInstanceOf(SizeBasedTriggeringPolicy.class, tp, tp.getClass().getName());
        final SizeBasedTriggeringPolicy sbtp = (SizeBasedTriggeringPolicy) tp;
        assertEquals(256 * 1024 * 1024, sbtp.getMaxFileSize());
        final RolloverStrategy rolloverStrategy = rfa.getManager().getRolloverStrategy();
        assertInstanceOf(
                DefaultRolloverStrategy.class,
                rolloverStrategy,
                rolloverStrategy.getClass().getName());
        final DefaultRolloverStrategy drs = (DefaultRolloverStrategy) rolloverStrategy;
        assertEquals(20, drs.getMaxIndex());
        configuration.start();
        configuration.stop();
    }

    private void testDailyRollingFileAppender(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        try {
            final Appender appender = configuration.getAppender("DRFA");
            assertNotNull(appender);
            assertEquals("DRFA", appender.getName());
            assertInstanceOf(
                    RollingFileAppender.class, appender, appender.getClass().getName());
            final RollingFileAppender rfa = (RollingFileAppender) appender;
            assertFalse(getAppendProperty(rfa), "append");
            assertEquals(1000, rfa.getManager().getBufferSize(), "bufferSize");
            assertFalse(rfa.getImmediateFlush(), "immediateFlush");
            assertEquals("target/hadoop.log", rfa.getFileName());
            assertEquals("target/hadoop.log%d{.dd-MM-yyyy}", rfa.getFilePattern());
            final TriggeringPolicy triggeringPolicy = rfa.getTriggeringPolicy();
            assertNotNull(triggeringPolicy);
            assertInstanceOf(
                    CompositeTriggeringPolicy.class,
                    triggeringPolicy,
                    triggeringPolicy.getClass().getName());
            final CompositeTriggeringPolicy ctp = (CompositeTriggeringPolicy) triggeringPolicy;
            final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
            assertEquals(1, triggeringPolicies.length);
            final TriggeringPolicy tp = triggeringPolicies[0];
            assertInstanceOf(TimeBasedTriggeringPolicy.class, tp, tp.getClass().getName());
            final TimeBasedTriggeringPolicy tbtp = (TimeBasedTriggeringPolicy) tp;
            assertEquals(1, tbtp.getInterval());
            final RolloverStrategy rolloverStrategy = rfa.getManager().getRolloverStrategy();
            assertInstanceOf(
                    DefaultRolloverStrategy.class,
                    rolloverStrategy,
                    rolloverStrategy.getClass().getName());
            final DefaultRolloverStrategy drs = (DefaultRolloverStrategy) rolloverStrategy;
            assertEquals(Integer.MAX_VALUE, drs.getMaxIndex());
        } finally {
            configuration.start();
            configuration.stop();
        }
    }

    private Layout<?> testFile() throws Exception {
        final Configuration configuration = getConfiguration("config-1.2/log4j-file-SimpleLayout");
        final FileAppender appender = configuration.getAppender("File");
        assertNotNull(appender);
        assertEquals("target/mylog.txt", appender.getFileName());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        assertFalse(getAppendProperty(appender), "append");
        assertEquals(1000, appender.getManager().getBufferSize(), "bufferSize");
        assertFalse(appender.getImmediateFlush(), "immediateFlush");
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

    void testConsoleEnhancedPatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-EnhancedPatternLayout");
        // %p, %X and %x converted to their Log4j 1.x bridge equivalent
        assertEquals("%d{ISO8601} [%t][%c] %-5v1Level %properties %ndc: %m%n", layout.getConversionPattern());
    }

    void testConsoleHtmlLayout() throws Exception {
        final HtmlLayout layout = (HtmlLayout) testConsole("config-1.2/log4j-console-HtmlLayout");
        assertEquals("Headline", layout.getTitle());
        assertTrue(layout.isLocationInfo());
    }

    void testConsolePatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-PatternLayout");
        // %p converted to its Log4j 1.x bridge equivalent
        assertEquals("%d{ISO8601} [%t][%c] %-5v1Level: %m%n", layout.getConversionPattern());
    }

    void testConsoleSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-SimpleLayout");
        assertEquals("%v1Level - %m%n", layout.getConversionPattern());
    }

    void testFileSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testFile();
        assertEquals("%v1Level - %m%n", layout.getConversionPattern());
    }

    void testNullAppender() throws Exception {
        final Configuration configuration = getConfiguration("config-1.2/log4j-NullAppender");
        final Appender appender = configuration.getAppender("NullAppender");
        assertNotNull(appender);
        assertEquals("NullAppender", appender.getName());
        assertInstanceOf(NullAppender.class, appender, appender.getClass().getName());
    }

    private boolean getFollowProperty(final ConsoleAppender consoleAppender) throws Exception {
        OutputStream outputStream = getOutputStream(consoleAppender.getManager());
        String className = outputStream.getClass().getName();
        return className.endsWith("ConsoleAppender$SystemErrStream")
                || className.endsWith("ConsoleAppender$SystemOutStream");
    }

    private boolean getAppendProperty(final RollingFileAppender appender) throws Exception {
        return getAppendProperty((FileOutputStream) getOutputStream(appender.getManager()));
    }

    private boolean getAppendProperty(final FileAppender appender) throws Exception {
        return getAppendProperty((FileOutputStream) getOutputStream(appender.getManager()));
    }

    private boolean getAppendProperty(final FileOutputStream os) throws Exception {
        // Java 8
        try {
            final Field appendField = FileOutputStream.class.getDeclaredField("append");
            appendField.setAccessible(true);
            return (Boolean) appendField.get(os);
        } catch (NoSuchFieldError | NoSuchFieldException e) {
            // Java 11
            final Field appendField = FileDescriptor.class.getDeclaredField("append");
            appendField.setAccessible(true);
            return (Boolean) appendField.get(os.getFD());
        }
    }

    private OutputStream getOutputStream(final OutputStreamManager manager) throws Exception {
        final Method getOutputStream = OutputStreamManager.class.getDeclaredMethod("getOutputStream");
        getOutputStream.setAccessible(true);
        return (OutputStream) getOutputStream.invoke(manager);
    }

    private Layout<?> testLayout(final Configuration config, final String appenderName) {
        final ConsoleAppender appender = config.getAppender(appenderName);
        assertNotNull(
                appender,
                "Missing appender '" + appenderName + "' in configuration " + config.getConfigurationSource());
        return appender.getLayout();
    }

    /**
     * Test if the default values from Log4j 1.x are respected.
     */
    void testDefaultValues() throws Exception {
        final Configuration config = getConfiguration("config-1.2/log4j-defaultValues");
        // HtmlLayout
        final HtmlLayout htmlLayout = (HtmlLayout) testLayout(config, "HTMLLayout");
        assertNotNull(htmlLayout);
        assertEquals("Log4J Log Messages", htmlLayout.getTitle(), "title");
        assertFalse(htmlLayout.isLocationInfo(), "locationInfo");
        // PatternLayout
        final PatternLayout patternLayout = (PatternLayout) testLayout(config, "PatternLayout");
        assertNotNull(patternLayout);
        assertEquals("%m%n", patternLayout.getConversionPattern(), "conversionPattern");
        // TTCCLayout
        final PatternLayout ttccLayout = (PatternLayout) testLayout(config, "TTCCLayout");
        assertNotNull(ttccLayout);
        assertEquals(
                "%r [%t] %p %c %notEmpty{%ndc }- %m%n",
                ttccLayout.getConversionPattern(), "equivalent conversion pattern");
        // TODO: XMLLayout
        // final XmlLayout xmlLayout = (XmlLayout) testLayout(config, "XMLLayout");
        // assertNotNull(xmlLayout);
        // ConsoleAppender
        final ConsoleAppender consoleAppender = config.getAppender("ConsoleAppender");
        assertNotNull(consoleAppender);
        assertEquals(Target.SYSTEM_OUT, consoleAppender.getTarget(), "target");
        final boolean follow = getFollowProperty(consoleAppender);
        assertFalse(follow, "follow");
        // DailyRollingFileAppender
        final RollingFileAppender dailyRollingFileAppender = config.getAppender("DailyRollingFileAppender");
        assertNotNull(dailyRollingFileAppender);
        assertEquals(
                "target/dailyRollingFileAppender%d{.yyyy-MM-dd}",
                dailyRollingFileAppender.getFilePattern(), "equivalent file pattern");
        assertTrue(getAppendProperty(dailyRollingFileAppender), "append");
        assertEquals(8192, dailyRollingFileAppender.getManager().getBufferSize(), "bufferSize");
        assertTrue(dailyRollingFileAppender.getImmediateFlush(), "immediateFlush");
        // FileAppender
        final FileAppender fileAppender = config.getAppender("FileAppender");
        assertNotNull(fileAppender);
        assertTrue(getAppendProperty(fileAppender), "append");
        assertEquals(8192, fileAppender.getManager().getBufferSize(), "bufferSize");
        assertTrue(fileAppender.getImmediateFlush(), "immediateFlush");
        // RollingFileAppender
        final RollingFileAppender rollingFileAppender = config.getAppender("RollingFileAppender");
        assertNotNull(rollingFileAppender);
        assertEquals("target/rollingFileAppender.%i", rollingFileAppender.getFilePattern(), "equivalent file pattern");
        final CompositeTriggeringPolicy compositePolicy =
                rollingFileAppender.getManager().getTriggeringPolicy();
        assertEquals(1, compositePolicy.getTriggeringPolicies().length);
        final SizeBasedTriggeringPolicy sizePolicy =
                (SizeBasedTriggeringPolicy) compositePolicy.getTriggeringPolicies()[0];
        assertEquals(10 * 1024 * 1024L, sizePolicy.getMaxFileSize(), "maxFileSize");
        final DefaultRolloverStrategy strategy =
                (DefaultRolloverStrategy) rollingFileAppender.getManager().getRolloverStrategy();
        assertEquals(1, strategy.getMaxIndex(), "maxBackupIndex");
        assertTrue(getAppendProperty(rollingFileAppender), "append");
        assertEquals(8192, rollingFileAppender.getManager().getBufferSize(), "bufferSize");
        assertTrue(rollingFileAppender.getImmediateFlush(), "immediateFlush");
        config.start();
        config.stop();
    }

    /**
     * Checks a hierarchy of filters.
     *
     * @param filter A filter
     * @return the number of filters
     */
    private int checkFilters(final org.apache.logging.log4j.core.Filter filter) {
        int count = 0;
        if (filter instanceof CompositeFilter) {
            for (final org.apache.logging.log4j.core.Filter part : ((CompositeFilter) filter).getFiltersArray()) {
                count += checkFilters(part);
            }
        } else if (filter instanceof FilterAdapter) {
            // Don't create adapters from wrappers
            assertFalse(
                    ((FilterAdapter) filter).getFilter() instanceof FilterWrapper,
                    "found FilterAdapter of a FilterWrapper");
            count += checkFilters(((FilterAdapter) filter).getFilter());
        } else {
            count++;
        }
        return count;
    }

    /**
     * Checks a hierarchy of filters.
     *
     * @param filter A filter
     * @return the number of filters
     */
    private int checkFilters(final org.apache.log4j.spi.Filter filter) {
        int count = 0;
        if (filter instanceof FilterWrapper) {
            // Don't create wrappers from adapters
            assertFalse(
                    ((FilterWrapper) filter).getFilter() instanceof FilterAdapter,
                    "found FilterWrapper of a FilterAdapter");
            count += checkFilters(((FilterWrapper) filter).getFilter());
        } else {
            count++;
        }
        // We prefer a:
        // CompositeFilter of native Log4j 2.x filters
        // over a:
        // FilterAdapter of a chain of FilterWrappers.
        assertNull(filter.getNext(), "found chain of Log4j 1.x filters");
        return count;
    }

    void testMultipleFilters() throws Exception {
        System.setProperty("test.tmpDir", tempDir.getCanonicalPath());

        try (final LoggerContext loggerContext = configure("log4j-multipleFilters")) {
            final Configuration configuration = loggerContext.getConfiguration();

            assertNotNull(configuration);

            // Check only number of filters.
            final Filterable console = configuration.getAppender("CONSOLE");
            assertNotNull(console);
            assertEquals(4, checkFilters(console.getFilter()));
            final Filterable file = configuration.getAppender("FILE");
            assertNotNull(file);
            assertEquals(4, checkFilters(file.getFilter()));
            final Filterable rfa = configuration.getAppender("RFA");
            assertNotNull(rfa);
            assertEquals(4, checkFilters(rfa.getFilter()));
            final Filterable drfa = configuration.getAppender("DRFA");
            assertNotNull(drfa);
            assertEquals(4, checkFilters(drfa.getFilter()));
            // List appenders
            final Appender appender = configuration.getAppender("LIST");
            assertNotNull(appender);
            assertEquals(3, checkFilters(((Filterable) appender).getFilter()));
            final ListAppender legacyAppender = (ListAppender) ((Adapter) appender).getAppender();
            final org.apache.logging.log4j.core.test.appender.ListAppender nativeAppender =
                    configuration.getAppender("LIST2");
            assertEquals(3, checkFilters(nativeAppender.getFilter()));

            final Logger logger = LogManager.getLogger(PropertiesConfigurationTest.class);
            int expected = 0;
            // message blocked by Threshold
            logger.trace("NEUTRAL message");
            assertEquals(expected, legacyAppender.getEvents().size());
            assertEquals(expected, nativeAppender.getEvents().size());
            // message blocked by DenyAll filter
            logger.warn("NEUTRAL message");
            assertEquals(expected, legacyAppender.getEvents().size());
            assertEquals(expected, nativeAppender.getEvents().size());
            // message accepted by level filter
            logger.info("NEUTRAL message");
            expected++;
            assertEquals(expected, legacyAppender.getEvents().size());
            assertEquals(expected, nativeAppender.getEvents().size());
            // message accepted by "StartsWith" filter
            logger.warn("ACCEPT message");
            expected++;
            assertEquals(expected, legacyAppender.getEvents().size());
            assertEquals(expected, nativeAppender.getEvents().size());
            // message blocked by "StartsWith" filter
            logger.info("DENY message");
            assertEquals(expected, legacyAppender.getEvents().size());
            assertEquals(expected, nativeAppender.getEvents().size());
        } finally {
            System.clearProperty("test.tmpDir");
        }
    }

    void testGlobalThreshold() throws Exception {
        try (final LoggerContext ctx = configure("config-1.2/log4j-global-threshold")) {
            final Configuration config = ctx.getConfiguration();
            final Filter filter = config.getFilter();
            assertInstanceOf(ThresholdFilter.class, filter);
            final ThresholdFilter thresholdFilter = (ThresholdFilter) filter;
            assertEquals(Level.INFO, thresholdFilter.getLevel());
            assertEquals(Filter.Result.NEUTRAL, thresholdFilter.getOnMatch());
            assertEquals(Filter.Result.DENY, thresholdFilter.getOnMismatch());

            final Logger logger = LogManager.getLogger(PropertiesConfigurationTest.class);
            // List appender
            final Appender appender = config.getAppender("LIST");
            assertNotNull(appender);
            final ListAppender legacyAppender = (ListAppender) ((Adapter) appender).getAppender();
            // Stopped by root logger level
            logger.trace("TRACE");
            assertEquals(0, legacyAppender.getEvents().size());
            // Stopped by global threshold
            logger.debug("DEBUG");
            assertEquals(0, legacyAppender.getEvents().size());
            // Accepted
            logger.info("INFO");
            assertEquals(1, legacyAppender.getEvents().size());
        }
    }

    protected void testEnhancedRollingFileAppender(final Configuration configuration) {
        Appender appender;
        TriggeringPolicy policy;
        RolloverStrategy strategy;
        DefaultRolloverStrategy defaultRolloverStrategy;
        // Time policy with default attributes
        appender = configuration.getAppender("DEFAULT_TIME");
        assertInstanceOf(RollingFileAppender.class, appender, "is RollingFileAppender");
        final RollingFileAppender defaultTime = (RollingFileAppender) appender;
        assertTrue(defaultTime.getManager().isAppend(), "append");
        assertEquals(8192, defaultTime.getManager().getBufferSize(), "bufferSize");
        assertTrue(defaultTime.getImmediateFlush(), "immediateFlush");
        assertEquals("target/EnhancedRollingFileAppender/defaultTime.log", defaultTime.getFileName(), "fileName");
        assertEquals(
                "target/EnhancedRollingFileAppender/defaultTime.%d{yyyy-MM-dd}.log",
                defaultTime.getFilePattern(), "filePattern");
        policy = defaultTime.getTriggeringPolicy();
        assertInstanceOf(TimeBasedTriggeringPolicy.class, policy, "is TimeBasedTriggeringPolicy");
        // Size policy with default attributes
        appender = configuration.getAppender("DEFAULT_SIZE");
        assertInstanceOf(RollingFileAppender.class, appender, "is RollingFileAppender");
        final RollingFileAppender defaultSize = (RollingFileAppender) appender;
        assertTrue(defaultSize.getManager().isAppend(), "append");
        assertEquals(8192, defaultSize.getManager().getBufferSize(), "bufferSize");
        assertTrue(defaultSize.getImmediateFlush(), "immediateFlush");
        assertEquals("target/EnhancedRollingFileAppender/defaultSize.log", defaultSize.getFileName(), "fileName");
        assertEquals(
                "target/EnhancedRollingFileAppender/defaultSize.%i.log", defaultSize.getFilePattern(), "filePattern");
        policy = defaultSize.getTriggeringPolicy();
        assertInstanceOf(SizeBasedTriggeringPolicy.class, policy, "is SizeBasedTriggeringPolicy");
        assertEquals(10 * 1024 * 1024L, ((SizeBasedTriggeringPolicy) policy).getMaxFileSize());
        strategy = defaultSize.getManager().getRolloverStrategy();
        assertInstanceOf(DefaultRolloverStrategy.class, strategy, "is DefaultRolloverStrategy");
        defaultRolloverStrategy = (DefaultRolloverStrategy) strategy;
        assertEquals(1, defaultRolloverStrategy.getMinIndex());
        assertEquals(7, defaultRolloverStrategy.getMaxIndex());
        // Time policy with custom attributes
        appender = configuration.getAppender("TIME");
        assertInstanceOf(RollingFileAppender.class, appender, "is RollingFileAppender");
        final RollingFileAppender time = (RollingFileAppender) appender;
        assertFalse(time.getManager().isAppend(), "append");
        assertEquals(1000, time.getManager().getBufferSize(), "bufferSize");
        assertFalse(time.getImmediateFlush(), "immediateFlush");
        assertEquals("target/EnhancedRollingFileAppender/time.log", time.getFileName(), "fileName");
        assertEquals(
                "target/EnhancedRollingFileAppender/time.%d{yyyy-MM-dd}.log", time.getFilePattern(), "filePattern");
        policy = time.getTriggeringPolicy();
        assertInstanceOf(TimeBasedTriggeringPolicy.class, policy, "is TimeBasedTriggeringPolicy");
        // Size policy with custom attributes
        appender = configuration.getAppender("SIZE");
        assertInstanceOf(RollingFileAppender.class, appender, "is RollingFileAppender");
        final RollingFileAppender size = (RollingFileAppender) appender;
        assertFalse(size.getManager().isAppend(), "append");
        assertEquals(1000, size.getManager().getBufferSize(), "bufferSize");
        assertFalse(size.getImmediateFlush(), "immediateFlush");
        assertEquals("target/EnhancedRollingFileAppender/size.log", size.getFileName(), "fileName");
        assertEquals("target/EnhancedRollingFileAppender/size.%i.log", size.getFilePattern(), "filePattern");
        policy = size.getTriggeringPolicy();
        assertInstanceOf(SizeBasedTriggeringPolicy.class, policy, "is SizeBasedTriggeringPolicy");
        assertEquals(10_000_000L, ((SizeBasedTriggeringPolicy) policy).getMaxFileSize());
        strategy = size.getManager().getRolloverStrategy();
        assertInstanceOf(DefaultRolloverStrategy.class, strategy, "is DefaultRolloverStrategy");
        defaultRolloverStrategy = (DefaultRolloverStrategy) strategy;
        assertEquals(11, defaultRolloverStrategy.getMinIndex());
        assertEquals(20, defaultRolloverStrategy.getMaxIndex());
    }

    protected void testLevelRangeFilter() throws Exception {
        try (final LoggerContext ctx = configure("config-1.2/log4j-LevelRangeFilter")) {
            final Configuration config = ctx.getConfiguration();
            final Logger logger = LogManager.getLogger(PropertiesConfigurationTest.class);
            // List appender
            final Appender appender = config.getAppender("LIST");
            assertNotNull(appender);
            final ListAppender legacyAppender = (ListAppender) ((Adapter) appender).getAppender();
            // deny
            logger.trace("TRACE");
            assertEquals(0, legacyAppender.getEvents().size());
            // deny
            logger.debug("DEBUG");
            assertEquals(0, legacyAppender.getEvents().size());
            // accept
            logger.info("INFO");
            assertEquals(1, legacyAppender.getEvents().size());
            // accept
            logger.warn("WARN");
            assertEquals(2, legacyAppender.getEvents().size());
            // accept
            logger.error("ERROR");
            assertEquals(3, legacyAppender.getEvents().size());
            // deny
            logger.fatal("FATAL");
            assertEquals(3, legacyAppender.getEvents().size());
        }
    }
}
