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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;
import java.net.URL;

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
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.HtmlLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.status.StatusLogger;
import org.junit.Test;

public class Log4j1ConfigurationFactoryTest {

	private Layout<?> testConsole(final String configResource) throws Exception {
		final Configuration configuration = configure(configResource);
		final ConsoleAppender appender = configuration.getAppender("Console");
		assertNotNull(appender);
		assertEquals(Target.SYSTEM_ERR, appender.getTarget());
		//
		final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
		assertNotNull(loggerConfig);
		assertEquals(Level.DEBUG, loggerConfig.getLevel());
		return appender.getLayout();
	}

	private Layout<?> testFile(final String configResource) throws Exception {
		final Configuration configuration = configure(configResource);
		final FileAppender appender = configuration.getAppender("File");
		assertNotNull(appender);
		assertEquals("target/mylog.txt", appender.getFileName());
		//
		final LoggerConfig loggerConfig = configuration.getLoggerConfig("com.example.foo");
		assertNotNull(loggerConfig);
		assertEquals(Level.DEBUG, loggerConfig.getLevel());
		return appender.getLayout();
	}

	private Configuration configure(final String configResource) throws URISyntaxException {
		final URL configLocation = ClassLoader.getSystemResource(configResource);
		assertNotNull(configLocation);
		final Configuration configuration = new Log4j1ConfigurationFactory().getConfiguration(null, "test",
				configLocation.toURI());
		assertNotNull(configuration);
		return configuration;
	}

	@Test
	public void testConsoleEnhancedPatternLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole(
				"config-1.2/log4j-console-EnhancedPatternLayout.properties");
		assertEquals("%d{ISO8601} [%t][%c] %-5p %properties %ndc: %m%n", layout.getConversionPattern());
	}

	@Test
	public void testConsoleHtmlLayout() throws Exception {
		final HtmlLayout layout = (HtmlLayout) testConsole("config-1.2/log4j-console-HtmlLayout.properties");
		assertEquals("Headline", layout.getTitle());
		assertTrue(layout.isLocationInfo());
	}

	@Test
	public void testConsolePatternLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-PatternLayout.properties");
		assertEquals("%d{ISO8601} [%t][%c] %-5p: %m%n", layout.getConversionPattern());
	}

	@Test
	public void testConsoleSimpleLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-SimpleLayout.properties");
		assertEquals("%level - %m%n", layout.getConversionPattern());
	}

	@Test
	public void testConsoleTtccLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testConsole("config-1.2/log4j-console-TTCCLayout.properties");
		assertEquals("%r [%t] %p %notEmpty{%ndc }- %m%n", layout.getConversionPattern());
	}

	@Test
	public void testConsoleXmlLayout() throws Exception {
		final Log4j1XmlLayout layout = (Log4j1XmlLayout) testConsole("config-1.2/log4j-console-XmlLayout.properties");
		assertTrue(layout.isLocationInfo());
		assertFalse(layout.isProperties());
	}

	@Test
	public void testFileSimpleLayout() throws Exception {
		final PatternLayout layout = (PatternLayout) testFile("config-1.2/log4j-file-SimpleLayout.properties");
		assertEquals("%level - %m%n", layout.getConversionPattern());
	}

	@Test
	public void testNullAppender() throws Exception {
		final Configuration configuration = configure("config-1.2/log4j-NullAppender.properties");
		final Appender appender = configuration.getAppender("NullAppender");
		assertNotNull(appender);
		assertEquals("NullAppender", appender.getName());
		assertTrue(appender.getClass().getName(), appender instanceof NullAppender);
	}

	@Test
	public void testRollingFileAppender() throws Exception {
		StatusLogger.getLogger().setLevel(Level.TRACE);
		final Configuration configuration = configure("config-1.2/log4j-RollingFileAppender.properties");
		final Appender appender = configuration.getAppender("RFA");
		assertNotNull(appender);
		assertEquals("RFA", appender.getName());
		assertTrue(appender.getClass().getName(), appender instanceof RollingFileAppender);
		final RollingFileAppender rfa = (RollingFileAppender) appender;
		assertEquals("./hadoop.log", rfa.getFileName());
		assertEquals("./hadoop.log.%i", rfa.getFilePattern());
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
	}

}
