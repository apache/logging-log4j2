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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.FileSystemException;
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
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class AbstractLog4j1ConfigurationTest {

    @Rule
    public TemporaryFolder tempFolder =
            TemporaryFolder.builder().assureDeletion().build();

    abstract Configuration getConfiguration(String configResourcePrefix) throws URISyntaxException, IOException;

    protected InputStream getResourceAsStream(final String configResource) throws IOException {
        final InputStream is = getClass().getClassLoader().getResourceAsStream(configResource);
        assertNotNull(is);
        return is;
    }

    protected LoggerContext configure(final String configResourcePrefix) throws URISyntaxException, IOException {
        Configurator.reconfigure(getConfiguration(configResourcePrefix));
        return (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
    }

    public void testConsoleCapitalization() throws Exception {
        final Configuration config = getConfiguration("config-1.2/log4j-capitalization");
        final Appender capitalized = config.getAppender("ConsoleCapitalized");
        assertNotNull(capitalized);
        assertEquals(capitalized.getClass(), ConsoleAppender.class);
        final Appender javaStyle = config.getAppender("ConsoleJavaStyle");
        assertNotNull(javaStyle);
        assertEquals(javaStyle.getClass(), ConsoleAppender.class);
        testConsoleAppender((ConsoleAppender) capitalized, (ConsoleAppender) javaStyle);
    }

    private void testConsoleAppender(final ConsoleAppender expected, final ConsoleAppender actual) {
        assertEquals("immediateFlush", expected.getImmediateFlush(), actual.getImmediateFlush());
        assertEquals("target", expected.getTarget(), actual.getTarget());
        assertEquals(
                "layoutClass",
                expected.getLayout().getClass(),
                actual.getLayout().getClass());
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
                "Missing appender '" + name + "' in configuration " + configResource + " → " + configuration, appender);
        assertEquals("follow", true, getFollowProperty(appender));
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

    public void testConsoleTtccLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-TTCCLayout");
        assertEquals("%d{ISO8601}{CET} %p - %m%n", layout.getConversionPattern());
    }

    public void testRollingFileAppender() throws Exception {
        testRollingFileAppender("config-1.2/log4j-RollingFileAppender", "RFA", "target/hadoop.log.%i");
    }

    public void testDailyRollingFileAppender() throws Exception {
        testDailyRollingFileAppender(
                "config-1.2/log4j-DailyRollingFileAppender", "DRFA", "target/hadoop.log%d{.dd-MM-yyyy}");
    }

    public void testRollingFileAppenderWithProperties() throws Exception {
        testRollingFileAppender("config-1.2/log4j-RollingFileAppender-with-props", "RFA", "target/hadoop.log.%i");
    }

    public void testSystemProperties1() throws Exception {
        final String tempFileName = System.getProperty("java.io.tmpdir") + "/hadoop.log";
        final Path tempFilePath = new File(tempFileName).toPath();
        Files.deleteIfExists(tempFilePath);
        final Configuration configuration = getConfiguration("config-1.2/log4j-system-properties-1");
        try {
            final RollingFileAppender appender = configuration.getAppender("RFA");
            assertEquals("append", false, getAppendProperty(appender));
            assertEquals("bufferSize", 1000, appender.getManager().getBufferSize());
            assertEquals("immediateFlush", false, appender.getImmediateFlush());
            final DefaultRolloverStrategy rolloverStrategy =
                    (DefaultRolloverStrategy) appender.getManager().getRolloverStrategy();
            assertEquals(16, rolloverStrategy.getMaxIndex());
            final CompositeTriggeringPolicy ctp = (CompositeTriggeringPolicy) appender.getTriggeringPolicy();
            final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
            assertEquals(1, triggeringPolicies.length);
            final TriggeringPolicy tp = triggeringPolicies[0];
            assertTrue(tp.getClass().getName(), tp instanceof SizeBasedTriggeringPolicy);
            final SizeBasedTriggeringPolicy sbtp = (SizeBasedTriggeringPolicy) tp;
            assertEquals(20 * 1024 * 1024, sbtp.getMaxFileSize());
            appender.stop(10, TimeUnit.SECONDS);
            // System.out.println("expected: " + tempFileName + " Actual: " +
            // appender.getFileName());
            assertEquals(tempFileName, appender.getFileName());
        } finally {
            configuration.start();
            configuration.stop();
            try {
                Files.deleteIfExists(tempFilePath);
            } catch (final FileSystemException e) {
                e.printStackTrace();
            }
        }
    }

    public void testSystemProperties2() throws Exception {
        final Configuration configuration = getConfiguration("config-1.2/log4j-system-properties-2");
        final RollingFileAppender appender = configuration.getAppender("RFA");
        final String tmpDir = System.getProperty("java.io.tmpdir");
        assertEquals(tmpDir + "/hadoop.log", appender.getFileName());
        appender.stop(10, TimeUnit.SECONDS);
        // Try to clean up
        try {
            Path path = new File(appender.getFileName()).toPath();
            Files.deleteIfExists(path);
            path = new File("${java.io.tmpdir}").toPath();
            Files.deleteIfExists(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void testRollingFileAppender(final String configResource, final String name, final String filePattern)
            throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final Appender appender = configuration.getAppender(name);
        assertNotNull(appender);
        assertEquals(name, appender.getName());
        assertTrue(appender.getClass().getName(), appender instanceof RollingFileAppender);
        final RollingFileAppender rfa = (RollingFileAppender) appender;

        assertTrue(
                "defaultRolloverStrategy", rfa.getManager().getRolloverStrategy() instanceof DefaultRolloverStrategy);
        assertFalse(
                "rolloverStrategy", ((DefaultRolloverStrategy) rfa.getManager().getRolloverStrategy()).isUseMax());
        assertEquals("append", false, getAppendProperty(rfa));
        assertEquals("bufferSize", 1000, rfa.getManager().getBufferSize());
        assertEquals("immediateFlush", false, rfa.getImmediateFlush());
        assertEquals("target/hadoop.log", rfa.getFileName());
        assertEquals(filePattern, rfa.getFilePattern());
        final TriggeringPolicy triggeringPolicy = rfa.getTriggeringPolicy();
        assertNotNull(triggeringPolicy);
        assertTrue(triggeringPolicy.getClass().getName(), triggeringPolicy instanceof CompositeTriggeringPolicy);
        final CompositeTriggeringPolicy ctp = (CompositeTriggeringPolicy) triggeringPolicy;
        final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
        assertEquals(1, triggeringPolicies.length);
        final TriggeringPolicy tp = triggeringPolicies[0];
        assertTrue(tp.getClass().getName(), tp instanceof SizeBasedTriggeringPolicy);
        final SizeBasedTriggeringPolicy sbtp = (SizeBasedTriggeringPolicy) tp;
        assertEquals(256 * 1024 * 1024, sbtp.getMaxFileSize());
        final RolloverStrategy rolloverStrategy = rfa.getManager().getRolloverStrategy();
        assertTrue(rolloverStrategy.getClass().getName(), rolloverStrategy instanceof DefaultRolloverStrategy);
        final DefaultRolloverStrategy drs = (DefaultRolloverStrategy) rolloverStrategy;
        assertEquals(20, drs.getMaxIndex());
        configuration.start();
        configuration.stop();
    }

    private void testDailyRollingFileAppender(final String configResource, final String name, final String filePattern)
            throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        try {
            final Appender appender = configuration.getAppender(name);
            assertNotNull(appender);
            assertEquals(name, appender.getName());
            assertTrue(appender.getClass().getName(), appender instanceof RollingFileAppender);
            final RollingFileAppender rfa = (RollingFileAppender) appender;
            assertEquals("append", false, getAppendProperty(rfa));
            assertEquals("bufferSize", 1000, rfa.getManager().getBufferSize());
            assertEquals("immediateFlush", false, rfa.getImmediateFlush());
            assertEquals("target/hadoop.log", rfa.getFileName());
            assertEquals(filePattern, rfa.getFilePattern());
            final TriggeringPolicy triggeringPolicy = rfa.getTriggeringPolicy();
            assertNotNull(triggeringPolicy);
            assertTrue(triggeringPolicy.getClass().getName(), triggeringPolicy instanceof CompositeTriggeringPolicy);
            final CompositeTriggeringPolicy ctp = (CompositeTriggeringPolicy) triggeringPolicy;
            final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
            assertEquals(1, triggeringPolicies.length);
            final TriggeringPolicy tp = triggeringPolicies[0];
            assertTrue(tp.getClass().getName(), tp instanceof TimeBasedTriggeringPolicy);
            final TimeBasedTriggeringPolicy tbtp = (TimeBasedTriggeringPolicy) tp;
            assertEquals(1, tbtp.getInterval());
            final RolloverStrategy rolloverStrategy = rfa.getManager().getRolloverStrategy();
            assertTrue(rolloverStrategy.getClass().getName(), rolloverStrategy instanceof DefaultRolloverStrategy);
            final DefaultRolloverStrategy drs = (DefaultRolloverStrategy) rolloverStrategy;
            assertEquals(Integer.MAX_VALUE, drs.getMaxIndex());
        } finally {
            configuration.start();
            configuration.stop();
        }
    }

    private Layout<?> testFile(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final FileAppender appender = configuration.getAppender("File");
        assertNotNull(appender);
        assertEquals("target/mylog.txt", appender.getFileName());
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertNotNull(loggerConfig);
        assertEquals(Level.DEBUG, loggerConfig.getLevel());
        assertEquals("append", false, getAppendProperty(appender));
        assertEquals("bufferSize", 1000, appender.getManager().getBufferSize());
        assertEquals("immediateFlush", false, appender.getImmediateFlush());
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

    public void testConsoleEnhancedPatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-EnhancedPatternLayout");
        // %p, %X and %x converted to their Log4j 1.x bridge equivalent
        assertEquals("%d{ISO8601} [%t][%c] %-5v1Level %properties %ndc: %m%n", layout.getConversionPattern());
    }

    public void testConsoleHtmlLayout() throws Exception {
        final HtmlLayout layout = (HtmlLayout) testConsole("config-1.2/log4j-console-HtmlLayout");
        assertEquals("Headline", layout.getTitle());
        assertTrue(layout.isLocationInfo());
    }

    public void testConsolePatternLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-PatternLayout");
        // %p converted to its Log4j 1.x bridge equivalent
        assertEquals("%d{ISO8601} [%t][%c] %-5v1Level: %m%n", layout.getConversionPattern());
    }

    public void testConsoleSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-SimpleLayout");
        assertEquals("%v1Level - %m%n", layout.getConversionPattern());
    }

    public void testFileSimpleLayout() throws Exception {
        final PatternLayout layout = (PatternLayout) testFile("config-1.2/log4j-file-SimpleLayout");
        assertEquals("%v1Level - %m%n", layout.getConversionPattern());
    }

    public void testNullAppender() throws Exception {
        final Configuration configuration = getConfiguration("config-1.2/log4j-NullAppender");
        final Appender appender = configuration.getAppender("NullAppender");
        assertNotNull(appender);
        assertEquals("NullAppender", appender.getName());
        assertTrue(appender.getClass().getName(), appender instanceof NullAppender);
    }

    private boolean getFollowProperty(final ConsoleAppender consoleAppender)
            throws Exception, NoSuchFieldException, IllegalAccessException {
        final CloseShieldOutputStream wrapperStream =
                (CloseShieldOutputStream) getOutputStream(consoleAppender.getManager());
        final Field delegateField = CloseShieldOutputStream.class.getDeclaredField("delegate");
        delegateField.setAccessible(true);
        final boolean follow = !System.out.equals(delegateField.get(wrapperStream));
        return follow;
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
                "Missing appender '" + appenderName + "' in configuration " + config.getConfigurationSource(),
                appender);
        return appender.getLayout();
    }

    /**
     * Test if the default values from Log4j 1.x are respected.
     */
    public void testDefaultValues() throws Exception {
        final Configuration config = getConfiguration("config-1.2/log4j-defaultValues");
        // HtmlLayout
        final HtmlLayout htmlLayout = (HtmlLayout) testLayout(config, "HTMLLayout");
        assertNotNull(htmlLayout);
        assertEquals("title", "Log4J Log Messages", htmlLayout.getTitle());
        assertEquals("locationInfo", false, htmlLayout.isLocationInfo());
        // PatternLayout
        final PatternLayout patternLayout = (PatternLayout) testLayout(config, "PatternLayout");
        assertNotNull(patternLayout);
        assertEquals("conversionPattern", "%m%n", patternLayout.getConversionPattern());
        // TTCCLayout
        final PatternLayout ttccLayout = (PatternLayout) testLayout(config, "TTCCLayout");
        assertNotNull(ttccLayout);
        assertEquals(
                "equivalent conversion pattern",
                "%r [%t] %p %c %notEmpty{%ndc }- %m%n",
                ttccLayout.getConversionPattern());
        // TODO: XMLLayout
        // final XmlLayout xmlLayout = (XmlLayout) testLayout(config, "XMLLayout");
        // assertNotNull(xmlLayout);
        // ConsoleAppender
        final ConsoleAppender consoleAppender = config.getAppender("ConsoleAppender");
        assertNotNull(consoleAppender);
        assertEquals("target", Target.SYSTEM_OUT, consoleAppender.getTarget());
        final boolean follow = getFollowProperty(consoleAppender);
        assertEquals("follow", false, follow);
        // DailyRollingFileAppender
        final RollingFileAppender dailyRollingFileAppender = config.getAppender("DailyRollingFileAppender");
        assertNotNull(dailyRollingFileAppender);
        assertEquals(
                "equivalent file pattern",
                "target/dailyRollingFileAppender%d{.yyyy-MM-dd}",
                dailyRollingFileAppender.getFilePattern());
        assertEquals("append", true, getAppendProperty(dailyRollingFileAppender));
        assertEquals("bufferSize", 8192, dailyRollingFileAppender.getManager().getBufferSize());
        assertEquals("immediateFlush", true, dailyRollingFileAppender.getImmediateFlush());
        // FileAppender
        final FileAppender fileAppender = config.getAppender("FileAppender");
        assertNotNull(fileAppender);
        assertEquals("append", true, getAppendProperty(fileAppender));
        assertEquals("bufferSize", 8192, fileAppender.getManager().getBufferSize());
        assertEquals("immediateFlush", true, fileAppender.getImmediateFlush());
        // RollingFileAppender
        final RollingFileAppender rollingFileAppender = config.getAppender("RollingFileAppender");
        assertNotNull(rollingFileAppender);
        assertEquals("equivalent file pattern", "target/rollingFileAppender.%i", rollingFileAppender.getFilePattern());
        final CompositeTriggeringPolicy compositePolicy =
                rollingFileAppender.getManager().getTriggeringPolicy();
        assertEquals(1, compositePolicy.getTriggeringPolicies().length);
        final SizeBasedTriggeringPolicy sizePolicy =
                (SizeBasedTriggeringPolicy) compositePolicy.getTriggeringPolicies()[0];
        assertEquals("maxFileSize", 10 * 1024 * 1024L, sizePolicy.getMaxFileSize());
        final DefaultRolloverStrategy strategy =
                (DefaultRolloverStrategy) rollingFileAppender.getManager().getRolloverStrategy();
        assertEquals("maxBackupIndex", 1, strategy.getMaxIndex());
        assertEquals("append", true, getAppendProperty(rollingFileAppender));
        assertEquals("bufferSize", 8192, rollingFileAppender.getManager().getBufferSize());
        assertEquals("immediateFlush", true, rollingFileAppender.getImmediateFlush());
        config.start();
        config.stop();
    }

    /**
     * Checks a hierarchy of filters.
     *
     * @param filter
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
                    "found FilterAdapter of a FilterWrapper",
                    ((FilterAdapter) filter).getFilter() instanceof FilterWrapper);
            count += checkFilters(((FilterAdapter) filter).getFilter());
        } else {
            count++;
        }
        return count;
    }

    /**
     * Checks a hierarchy of filters.
     *
     * @param filter
     * @return the number of filters
     */
    private int checkFilters(final org.apache.log4j.spi.Filter filter) {
        int count = 0;
        if (filter instanceof FilterWrapper) {
            // Don't create wrappers from adapters
            assertFalse(
                    "found FilterWrapper of a FilterAdapter",
                    ((FilterWrapper) filter).getFilter() instanceof FilterAdapter);
            count += checkFilters(((FilterWrapper) filter).getFilter());
        } else {
            count++;
        }
        // We prefer a:
        // CompositeFilter of native Log4j 2.x filters
        // over a:
        // FilterAdapter of a chain of FilterWrappers.
        assertNull("found chain of Log4j 1.x filters", filter.getNext());
        return count;
    }

    public void testMultipleFilters() throws Exception {
        final File folder = tempFolder.newFolder();
        System.setProperty("test.tmpDir", folder.getCanonicalPath());
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
            assertEquals(3, checkFilters(((Filterable) nativeAppender).getFilter()));
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

    public void testGlobalThreshold() throws Exception {
        try (final LoggerContext ctx = configure("config-1.2/log4j-global-threshold")) {
            final Configuration config = ctx.getConfiguration();
            final Filter filter = config.getFilter();
            assertTrue(filter instanceof ThresholdFilter);
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
        assertTrue("is RollingFileAppender", appender instanceof RollingFileAppender);
        final RollingFileAppender defaultTime = (RollingFileAppender) appender;
        assertEquals("append", true, defaultTime.getManager().isAppend());
        assertEquals("bufferSize", 8192, defaultTime.getManager().getBufferSize());
        assertEquals("immediateFlush", true, defaultTime.getImmediateFlush());
        assertEquals("fileName", "target/EnhancedRollingFileAppender/defaultTime.log", defaultTime.getFileName());
        assertEquals(
                "filePattern",
                "target/EnhancedRollingFileAppender/defaultTime.%d{yyyy-MM-dd}.log",
                defaultTime.getFilePattern());
        policy = defaultTime.getTriggeringPolicy();
        assertTrue("is TimeBasedTriggeringPolicy", policy instanceof TimeBasedTriggeringPolicy);
        // Size policy with default attributes
        appender = configuration.getAppender("DEFAULT_SIZE");
        assertTrue("is RollingFileAppender", appender instanceof RollingFileAppender);
        final RollingFileAppender defaultSize = (RollingFileAppender) appender;
        assertEquals("append", true, defaultSize.getManager().isAppend());
        assertEquals("bufferSize", 8192, defaultSize.getManager().getBufferSize());
        assertEquals("immediateFlush", true, defaultSize.getImmediateFlush());
        assertEquals("fileName", "target/EnhancedRollingFileAppender/defaultSize.log", defaultSize.getFileName());
        assertEquals(
                "filePattern", "target/EnhancedRollingFileAppender/defaultSize.%i.log", defaultSize.getFilePattern());
        policy = defaultSize.getTriggeringPolicy();
        assertTrue("is SizeBasedTriggeringPolicy", policy instanceof SizeBasedTriggeringPolicy);
        assertEquals(10 * 1024 * 1024L, ((SizeBasedTriggeringPolicy) policy).getMaxFileSize());
        strategy = defaultSize.getManager().getRolloverStrategy();
        assertTrue("is DefaultRolloverStrategy", strategy instanceof DefaultRolloverStrategy);
        defaultRolloverStrategy = (DefaultRolloverStrategy) strategy;
        assertEquals(1, defaultRolloverStrategy.getMinIndex());
        assertEquals(7, defaultRolloverStrategy.getMaxIndex());
        // Time policy with custom attributes
        appender = configuration.getAppender("TIME");
        assertTrue("is RollingFileAppender", appender instanceof RollingFileAppender);
        final RollingFileAppender time = (RollingFileAppender) appender;
        assertEquals("append", false, time.getManager().isAppend());
        assertEquals("bufferSize", 1000, time.getManager().getBufferSize());
        assertEquals("immediateFlush", false, time.getImmediateFlush());
        assertEquals("fileName", "target/EnhancedRollingFileAppender/time.log", time.getFileName());
        assertEquals(
                "filePattern", "target/EnhancedRollingFileAppender/time.%d{yyyy-MM-dd}.log", time.getFilePattern());
        policy = time.getTriggeringPolicy();
        assertTrue("is TimeBasedTriggeringPolicy", policy instanceof TimeBasedTriggeringPolicy);
        // Size policy with custom attributes
        appender = configuration.getAppender("SIZE");
        assertTrue("is RollingFileAppender", appender instanceof RollingFileAppender);
        final RollingFileAppender size = (RollingFileAppender) appender;
        assertEquals("append", false, size.getManager().isAppend());
        assertEquals("bufferSize", 1000, size.getManager().getBufferSize());
        assertEquals("immediateFlush", false, size.getImmediateFlush());
        assertEquals("fileName", "target/EnhancedRollingFileAppender/size.log", size.getFileName());
        assertEquals("filePattern", "target/EnhancedRollingFileAppender/size.%i.log", size.getFilePattern());
        policy = size.getTriggeringPolicy();
        assertTrue("is SizeBasedTriggeringPolicy", policy instanceof SizeBasedTriggeringPolicy);
        assertEquals(10_000_000L, ((SizeBasedTriggeringPolicy) policy).getMaxFileSize());
        strategy = size.getManager().getRolloverStrategy();
        assertTrue("is DefaultRolloverStrategy", strategy instanceof DefaultRolloverStrategy);
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
