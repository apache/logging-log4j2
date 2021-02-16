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
package org.apache.logging.log4j.core.appender;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rolling.DirectWriteRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Builder;
import org.junit.jupiter.api.Test;

public class ReconfigureAppenderTest {
	private RollingFileAppender appender;

	@Test
	public void addAndRemoveAppenderTest()
	{
		// this will create a rolling file appender and add it to the logger
		// of this class. The file manager is created for the first time.
		// see AbstractManager.getManager(...).
		this.createAndAddAppender();

		// let's write something to the logger to ensure the output stream is opened.
		// We expect this call to create a a new output stream (which is does).
		// see OutputStreamManager.writeToDestination(...).
		Logger logger = (Logger)LogManager.getLogger(this.getClass());
		logger.info("test message 1");

		// this will close the rolling file appender and remove it from the logger
		// of this class. We expect the file output stream to be closed (which is it)
		// however the FileManager instance is kept in AbstractManager.MAP. This means that
		// when we create a new rolling file appender with the DirectWriteRolloverStrategy
		// this OLD file manager will be retrieved from the map (since it has the SAME file pattern)
		// and this is a problem as the output stream on that file manager is CLOSED. The problem
		// here is that we attempt to remove a file manager call NULL instead of FILE PATTERN
		this.removeAppender();

		// create a new rolling file appender for this logger. We expect this to create a new file
		// manager as the old one should have been removed. Since the instance of this file manager
		// is still in AbstractManager.MAP, it is returned and assigned to our new rolling file
		// appender instance. The problem here is that the file manager is create with the name
		// FILE PATTERN and that its output stream is closed.
		this.createAndAddAppender();

		// try and log something. This will not be logged anywhere. An exception will be thrown:
		// Caused by: java.io.IOException: Stream Closed
		logger.info("test message 2");

		// remove the appender as before.
		this.removeAppender();

		// this method will use reflection to go and remove the instance of FileManager from the AbstractManager.MAP
		// ourselves. This means that the rolling file appender has been stopped (previous method) AND its
		// manager has been removed.
		this.removeManagerUsingReflection();

		// now that the instance of FileManager is not present in MAP, creating the appender will actually
		// create a new rolling file manager, and put this in the map (keyed on file pattern again).
		this.createAndAddAppender();

		// because we have a new instance of file manager, this will create a new output stream. We can verify
		// this by looking inside the filepattern.1.log file inside the working directory, and noticing that
		// we have 'test message 1' followed by 'test message 3'. 'test message 2' is missing because we attempted
		// to write while the output stream was closed.
		logger.info("test message 3");

		// possible fixes:
		// 1) create the RollingFileManager and set it's name to FILE PATTERN when using DirectWriteRolloverStrategy
		// 2) when stopping the appender (and thus the manager), remove on FILE PATTERN if DirectWriteRolloverStrategy
		// 3) on OutputStreamManager.getOutputStream(), determine if the output stream is closed, and if it is create
		//              a new one. Note that this isn't really desirable as the only fix as if the file pattern had to change
		//              an instance of file manager would still exist in MAP, causing a resource leak.

		// now the obvious problem here is that if multiple file appenders use the same rolling file manager. We may run
		// into a case where the file manager is removed and the output stream is closed, and the remaining appenders
		// may not work correctly. I'm not sure  of the use case in this scenario, and if people actually do this
		// but based on the code it would be possible. I have also not tested this scenario out as it is not the
		// scenario we would ever use, but it should be considered while fixing this issue.
	}
	private void removeManagerUsingReflection()
	{
		try
		{
			Field field = AbstractManager.class.getDeclaredField("MAP");
			field.setAccessible(true);

			// Retrieve the map itself.
			Map<String, AbstractManager> map =
				(Map<String, AbstractManager>)field.get(null);

			// Remove the file manager keyed on file pattern.
			map.remove(appender.getFilePattern());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void removeAppender()
	{
		Logger logger = (Logger)LogManager.getLogger(this.getClass());

		// This call attempts to remove the file manager, but uses the name of the appender
		// (NULL in this case) instead of PATTERN.
		// see AbstractManager.stop(...).
		appender.stop();
		logger.removeAppender(appender);
	}

	private void createAndAddAppender()
	{
		ConfigurationBuilder<BuiltConfiguration> config_builder =
			ConfigurationBuilderFactory.newConfigurationBuilder();

		// All loggers must have a root logger. The default root logger logs ERRORs to the console.
		// Override this with a root logger that does not log anywhere as we leave it up the
		// appenders on the logger.
		config_builder.add(config_builder.newRootLogger(Level.INFO));

		// Initialise the logger context.
		LoggerContext logger_context =
			Configurator.initialize(config_builder.build());

		// Retrieve the logger.
		Logger logger = (Logger) LogManager.getLogger(this.getClass());

		Builder pattern_builder = PatternLayout.newBuilder().withPattern(
			"[%d{dd-MM-yy HH:mm:ss}] %p %m %throwable %n");

		PatternLayout pattern_layout = (PatternLayout) pattern_builder.build();

		appender = RollingFileAppender
			.newBuilder()
			.withLayout(pattern_layout)
			.withName("rollingfileappender")
			.withFilePattern("target/filepattern.%i.log")
			.withPolicy(SizeBasedTriggeringPolicy.createPolicy("5 MB"))
			.withAppend(true)
			.withStrategy(
				DirectWriteRolloverStrategy
					.newBuilder()
					.withConfig(logger_context.getConfiguration())
					.withMaxFiles("5")
					.build())
			.setConfiguration(logger_context.getConfiguration())
			.build();

		appender.start();

		logger.addAppender(appender);
	}
}
