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

import static org.junit.Assert.assertNotNull;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.logging.log4j.core.config.Configuration;
import org.junit.Test;

public class Log4j1ConfigurationFactoryTest extends AbstractLog4j1ConfigurationTest {

	Configuration getConfiguration(final String configResourcePrefix) throws URISyntaxException {
		final String configResource = configResourcePrefix + ".properties";
		final URL configLocation = ClassLoader.getSystemResource(configResource);
		assertNotNull(configResource, configLocation);
		final Configuration configuration = new Log4j1ConfigurationFactory().getConfiguration(null, "test", configLocation.toURI());
		assertNotNull(configuration);
		configuration.initialize();
		return configuration;
	}

	@Override
	@Test
	public void testConsoleEnhancedPatternLayout() throws Exception {
		super.testConsoleEnhancedPatternLayout();
	}

	@Override
	@Test
	public void testConsoleHtmlLayout() throws Exception {
		super.testConsoleHtmlLayout();
	}

	@Override
	@Test
	public void testConsolePatternLayout() throws Exception {
		super.testConsolePatternLayout();
	}

	@Override
	@Test
	public void testConsoleSimpleLayout() throws Exception {
		super.testConsoleSimpleLayout();
	}

	@Override
	@Test
	public void testConsoleTtccLayout() throws Exception {
		super.testConsoleTtccLayout();
	}

	@Override
	@Test
	public void testConsoleXmlLayout() throws Exception {
		super.testConsoleXmlLayout();
	}

	@Override
	@Test
	public void testFileSimpleLayout() throws Exception {
		super.testFileSimpleLayout();
	}

	@Override
	@Test
	public void testNullAppender() throws Exception {
		super.testNullAppender();
	}

	@Override
	@Test
	public void testRollingFileAppender() throws Exception {
		super.testRollingFileAppender();
	}

	@Override
	@Test
	public void testDailyRollingFileAppender() throws Exception {
		super.testDailyRollingFileAppender();
	}

	@Override
	@Test
	public void testRollingFileAppenderWithProperties() throws Exception {
		super.testRollingFileAppenderWithProperties();
	}

	@Override
	@Test
	public void testSystemProperties1() throws Exception {
		super.testSystemProperties1();
	}

	@Override
	@Test
	public void testSystemProperties2() throws Exception {
		super.testSystemProperties2();
	}

}
