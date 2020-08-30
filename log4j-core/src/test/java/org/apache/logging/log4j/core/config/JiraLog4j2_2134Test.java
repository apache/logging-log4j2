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

import java.io.Serializable;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Tag("yaml")
@LoggerContextSource("log4j2-2134.yml")
public class JiraLog4j2_2134Test {

	@Test
	public void testRefresh() {
		final Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		final PatternLayout layout = PatternLayout.newBuilder()
		// @formatter:off
				.withPattern(PatternLayout.SIMPLE_CONVERSION_PATTERN)
				.withConfiguration(config)
				.build();
        final Layout<? extends Serializable> layout1 = layout;
		// @formatter:on
		final Appender appender = FileAppender.newBuilder().withFileName("target/test.log").setLayout(layout1)
        .setConfiguration(config).withBufferSize(4000).setName("File").build();
		// appender.start();
		config.addAppender(appender);
		final AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
		final AppenderRef[] refs = new AppenderRef[] { ref };
		final LoggerConfig loggerConfig = LoggerConfig.createLogger(false, Level.INFO, "testlog4j2refresh", "true", refs,
				null, config, null);
		loggerConfig.addAppender(appender, null, null);
		config.addLogger("testlog4j2refresh", loggerConfig);
		ctx.stop();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@Test
	public void testRefreshMinimalCodeStart() {
		final Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@Test
	public void testRefreshMinimalCodeStopStart() {
		final Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		ctx.stop();
		ctx.start();

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@Test
	public void testRefreshMinimalCodeStopStartConfig() {
		final Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		ctx.stop();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testRefreshDeprecatedApis() {
		final Logger log = LogManager.getLogger(this.getClass());
		final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		final Configuration config = ctx.getConfiguration();
		final PatternLayout layout = PatternLayout.createLayout(PatternLayout.SIMPLE_CONVERSION_PATTERN, null, config, null,
				null, false, false, null, null);
		final Appender appender = FileAppender.createAppender("target/test.log", "false", "false", "File", "true", "false",
				"false", "4000", layout, null, "false", null, config);
		appender.start();
		config.addAppender(appender);
		final AppenderRef ref = AppenderRef.createAppenderRef("File", null, null);
		final AppenderRef[] refs = new AppenderRef[] { ref };
		final LoggerConfig loggerConfig = LoggerConfig.createLogger("false", Level.INFO, "testlog4j2refresh", "true", refs,
				null, config, null);
		loggerConfig.addAppender(appender, null, null);
		config.addLogger("testlog4j2refresh", loggerConfig);
		ctx.stop();
		ctx.start(config);

		assertDoesNotThrow(() -> log.error("Info message"));
	}
}
