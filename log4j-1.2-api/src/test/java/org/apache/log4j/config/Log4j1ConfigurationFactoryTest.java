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
package org.apache.log4j.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.layout.Log4j1XmlLayout;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.NullAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.Test;

public class Log4j1ConfigurationFactoryTest {

    private Layout<?> testConsole(final String configResource) throws Exception {
        final Configuration configuration = getConfiguration(configResource);
        final String name = "Console";
        final ConsoleAppender appender = configuration.getAppender(name);
        assertThat(appender).describedAs("Missing appender '" + name + "' in configuration " + configResource + " → " + configuration).isNotNull();
        assertThat(appender.getTarget()).isEqualTo(Target.SYSTEM_ERR);
        //
        final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
        assertThat(loggerConfig).isNotNull();
        assertThat(loggerConfig.getLevel()).isEqualTo(Level.DEBUG);
        configuration.start();
        configuration.stop();
        return appender.getLayout();
    }

	private Layout<?> testFile(final String configResource) throws Exception {
		final Configuration configuration = getConfiguration(configResource);
		final FileAppender appender = configuration.getAppender("File");
		assertThat(appender).isNotNull();
		assertThat(appender.getFileName()).isEqualTo("target/mylog.txt");
		//
		final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
		assertThat(loggerConfig).isNotNull();
		assertThat(loggerConfig.getLevel()).isEqualTo(Level.DEBUG);
		configuration.start();
		configuration.stop();
		return appender.getLayout();
	}

	private Configuration getConfiguration(final String configResource) throws URISyntaxException {
		final URL configLocation = ClassLoader.getSystemResource(configResource);
		assertThat(configLocation).describedAs(configResource).isNotNull();
		final Configuration configuration = new Log4j1ConfigurationFactory().getConfiguration(null, "test",
				configLocation.toURI());
		assertThat(configuration).isNotNull();
		return configuration;
	}

	@Test
	public void testConsoleEnhancedPatternLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole(
				"config-1.2/log4j-console-EnhancedPatternLayout.properties");
		assertThat(layout.getConversionPattern()).isEqualTo("%d{ISO8601} [%t][%c] %-5p %properties %ndc: %m%n");
	}

	@Test
	public void testConsoleHtmlLayout() throws Exception {
		final HtmlLayout layout = (HtmlLayout) testConsole("config-1.2/log4j-console-HtmlLayout.properties");
		assertThat(layout.getTitle()).isEqualTo("Headline");
		assertThat(layout.isLocationInfo()).isTrue();
	}

	@Test
	public void testConsolePatternLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-PatternLayout.properties");
		assertThat(layout.getConversionPattern()).isEqualTo("%d{ISO8601} [%t][%c] %-5p: %m%n");
	}

	@Test
	public void testConsoleSimpleLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-SimpleLayout.properties");
		assertThat(layout.getConversionPattern()).isEqualTo("%level - %m%n");
	}

	@Test
	public void testConsoleTtccLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-TTCCLayout.properties");
		assertThat(layout.getConversionPattern()).isEqualTo("%r [%t] %p %notEmpty{%ndc }- %m%n");
	}

	@Test
	public void testConsoleXmlLayout() throws Exception {
		final Log4j1XmlLayout layout = (Log4j1XmlLayout) testConsole("config-1.2/log4j-console-XmlLayout.properties");
		assertThat(layout.isLocationInfo()).isTrue();
		assertThat(layout.isProperties()).isFalse();
	}

	@Test
	public void testFileSimpleLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testFile("config-1.2/log4j-file-SimpleLayout.properties");
		assertThat(layout.getConversionPattern()).isEqualTo("%level - %m%n");
	}

	@Test
	public void testNullAppender() throws Exception {
		final Configuration configuration = getConfiguration("config-1.2/log4j-NullAppender.properties");
		final Appender appender = configuration.getAppender("NullAppender");
		assertThat(appender).isNotNull();
		assertThat(appender.getName()).isEqualTo("NullAppender");
		assertThat(appender instanceof NullAppender).describedAs(appender.getClass().getName()).isTrue();
	}

	@Test
	public void testRollingFileAppender() throws Exception {
		testRollingFileAppender("config-1.2/log4j-RollingFileAppender.properties", "RFA", "target/hadoop.log.%i");
	}

	@Test
	public void testDailyRollingFileAppender() throws Exception {
		testDailyRollingFileAppender("config-1.2/log4j-DailyRollingFileAppender.properties", "DRFA", "target/hadoop.log%d{.yyyy-MM-dd}");
	}

	@Test
	public void testRollingFileAppenderWithProperties() throws Exception {
		testRollingFileAppender("config-1.2/log4j-RollingFileAppender-with-props.properties", "RFA", "target/hadoop.log.%i");
	}

	@Test
	public void testSystemProperties1() throws Exception {
        final String tempFileName = System.getProperty("java.io.tmpdir") + "/hadoop.log";
        final Path tempFilePath = new File(tempFileName).toPath();
        Files.deleteIfExists(tempFilePath);
        try {
            final Configuration configuration = getConfiguration("config-1.2/log4j-system-properties-1.properties");
            final RollingFileAppender appender = configuration.getAppender("RFA");
			appender.stop(10, TimeUnit.SECONDS);
            // System.out.println("expected: " + tempFileName + " Actual: " + appender.getFileName());
            assertThat(appender.getFileName()).isEqualTo(tempFileName);
        } finally {
			try {
				Files.deleteIfExists(tempFilePath);
			} catch (final FileSystemException e) {
				e.printStackTrace();
			}
        }
	}

	@Test
	public void testSystemProperties2() throws Exception {
		final Configuration configuration = getConfiguration("config-1.2/log4j-system-properties-2.properties");
		final RollingFileAppender appender = configuration.getAppender("RFA");
		assertThat(appender.getFileName()).isEqualTo("${java.io.tmpdir}/hadoop.log");
		appender.stop(10, TimeUnit.SECONDS);
		Path path = new File(appender.getFileName()).toPath();
        Files.deleteIfExists(path);
        path = new File("${java.io.tmpdir}").toPath();
        Files.deleteIfExists(path);
	}

	private void testRollingFileAppender(final String configResource, final String name, final String filePattern) throws URISyntaxException {
		final Configuration configuration = getConfiguration(configResource);
		final Appender appender = configuration.getAppender(name);
		assertThat(appender).isNotNull();
		assertThat(appender.getName()).isEqualTo(name);
		assertThat(appender instanceof RollingFileAppender).describedAs(appender.getClass().getName()).isTrue();
		final RollingFileAppender rfa = (RollingFileAppender) appender;
		assertThat(rfa.getFileName()).isEqualTo("target/hadoop.log");
		assertThat(rfa.getFilePattern()).isEqualTo(filePattern);
		final TriggeringPolicy triggeringPolicy = rfa.getTriggeringPolicy();
		assertThat(triggeringPolicy).isNotNull();
		assertThat(triggeringPolicy instanceof CompositeTriggeringPolicy).describedAs(triggeringPolicy.getClass().getName()).isTrue();
		final CompositeTriggeringPolicy ctp = (CompositeTriggeringPolicy) triggeringPolicy;
		final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
		assertThat(triggeringPolicies).hasSize(1);
		final TriggeringPolicy tp = triggeringPolicies[0];
		assertThat(tp instanceof SizeBasedTriggeringPolicy).describedAs(tp.getClass().getName()).isTrue();
		final SizeBasedTriggeringPolicy sbtp = (SizeBasedTriggeringPolicy) tp;
		assertThat(sbtp.getMaxFileSize()).isEqualTo(256 * 1024 * 1024);
		final RolloverStrategy rolloverStrategy = rfa.getManager().getRolloverStrategy();
		assertThat(rolloverStrategy instanceof DefaultRolloverStrategy).describedAs(rolloverStrategy.getClass().getName()).isTrue();
		final DefaultRolloverStrategy drs = (DefaultRolloverStrategy) rolloverStrategy;
		assertThat(drs.getMaxIndex()).isEqualTo(20);
		configuration.start();
		configuration.stop();
	}

	private void testDailyRollingFileAppender(final String configResource, final String name, final String filePattern) throws URISyntaxException {
		final Configuration configuration = getConfiguration(configResource);
		final Appender appender = configuration.getAppender(name);
		assertThat(appender).isNotNull();
		assertThat(appender.getName()).isEqualTo(name);
		assertThat(appender instanceof RollingFileAppender).describedAs(appender.getClass().getName()).isTrue();
		final RollingFileAppender rfa = (RollingFileAppender) appender;
		assertThat(rfa.getFileName()).isEqualTo("target/hadoop.log");
		assertThat(rfa.getFilePattern()).isEqualTo(filePattern);
		final TriggeringPolicy triggeringPolicy = rfa.getTriggeringPolicy();
		assertThat(triggeringPolicy).isNotNull();
		assertThat(triggeringPolicy instanceof CompositeTriggeringPolicy).describedAs(triggeringPolicy.getClass().getName()).isTrue();
		final CompositeTriggeringPolicy ctp = (CompositeTriggeringPolicy) triggeringPolicy;
		final TriggeringPolicy[] triggeringPolicies = ctp.getTriggeringPolicies();
		assertThat(triggeringPolicies).hasSize(1);
		final TriggeringPolicy tp = triggeringPolicies[0];
		assertThat(tp instanceof TimeBasedTriggeringPolicy).describedAs(tp.getClass().getName()).isTrue();
		final TimeBasedTriggeringPolicy tbtp = (TimeBasedTriggeringPolicy) tp;
		assertThat(tbtp.getInterval()).isEqualTo(1);
		final RolloverStrategy rolloverStrategy = rfa.getManager().getRolloverStrategy();
		assertThat(rolloverStrategy instanceof DefaultRolloverStrategy).describedAs(rolloverStrategy.getClass().getName()).isTrue();
		final DefaultRolloverStrategy drs = (DefaultRolloverStrategy) rolloverStrategy;
		assertThat(drs.getMaxIndex()).isEqualTo(Integer.MAX_VALUE);
		configuration.start();
		configuration.stop();
	}

}
