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

package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("yaml")
@LoggerContextSource("log4j2-2134.yml")
public class JiraLog4j2_2134Test {

	@Test
	public void testRefresh() {
		Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		PatternLayout layout = PatternLayout.newBuilder()
		// @formatter:off
				.setPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
				.setConfiguration(config)
				.build();
		// @formatter:on
		Appender appender = FileAppender.newBuilder().setFileName("target/test.log").setLayout(layout)
				.setConfiguration(config).setBufferSize(4000).setName("File").build();
		// appender.start();
		config.addAppender(appender);
		AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
		AppenderRef[] refs = new AppenderRef[] { ref };
		LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, "testlog4j2refresh", "true", refs,
				null, config, null);
		loggerConfig.addAppender(appender, null, null);
		config.addLogger("testlog4j2refresh", loggerConfig);
		ctx.stop();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@Test
	public void testRefreshMinimalCodeStart() {
		Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@Test
	public void testRefreshMinimalCodeStopStart() {
		Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.stop();
		ctx.start();

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@Test
	public void testRefreshMinimalCodeStopStartConfig() {
		Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		ctx.stop();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRefreshDeprecatedApis() {
		Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		PatternLayout layout = PatternLayout.newBuilder()
		        .setPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
		        .setPatternSelector(null)
		        .setConfiguration(config)
		        .setRegexReplacement(null)
		        .setCharset(null)
		        .setAlwaysWriteExceptions(false)
		        .setNoConsoleNoAnsi(false)
		        .setHeader(null)
		        .setFooter(null)
		        .build();
		// @formatter:off
		Appender appender = FileAppender.newBuilder()
		        .setFileName("target/test.log")
		        .setAppend(false)
		        .setLocking(false)
		        .setName("File")
		        .setImmediateFlush(true)
		        .setIgnoreExceptions(false)
		        .setBufferedIo(false)
		        .setBufferSize(4000)
		        .setLayout(layout)
		        .setAdvertise(false)
		        .setConfiguration(config)
		        .build();
        // @formatter:on
		appender.start();
		config.addAppender(appender);
		AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
		AppenderRef[] refs = new AppenderRef[] { ref };
		LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, "testlog4j2refresh", "true", refs,
				null, config, null);
		loggerConfig.addAppender(appender, null, null);
		config.addLogger("testlog4j2refresh", loggerConfig);
		ctx.stop();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}
}
