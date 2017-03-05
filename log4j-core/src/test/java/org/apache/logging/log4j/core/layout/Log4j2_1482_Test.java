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

package org.apache.logging.log4j.core.layout;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.categories.Layouts;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.junit.CleanFolders;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests https://issues.apache.org/jira/browse/LOG4J2-1482
 */
@Category(Layouts.Csv.class)
public abstract class Log4j2_1482_Test {

	static final String CONFIG_LOCATION = "log4j2-1482.xml";
	
	static final String FOLDER = "target/log4j2-1482";

	private static final int LOOP_COUNT = 10;

	static void assertFileContents(final int runNumber) throws IOException {
		final Path path = Paths.get(FOLDER + "/audit.tmp");
		final List<String> lines = Files.readAllLines(path, Charset.defaultCharset());
		int i = 1;
		final int size = lines.size();
		for (final String string : lines) {
			if (string.startsWith(",,")) {
				final Path folder = Paths.get(FOLDER);
				final File[] files = folder.toFile().listFiles();
				Arrays.sort(files);
				System.out.println("Run " + runNumber + ": " + Arrays.toString(files));
				Assert.fail(
						String.format("Run %,d, line %,d of %,d: \"%s\" in %s", runNumber, i++, size, string, lines));
			}
		}
	}

	@Rule
	public CleanFolders cleanFolders = new CleanFolders(FOLDER);

	protected abstract void log(int runNumber) ;

	private void loopingRun(final int loopCount) throws IOException {
		for (int i = 1; i <= loopCount; i++) {
			try (LoggerContext loggerContext = Configurator.initialize(getClass().getName(),
					CONFIG_LOCATION)) {
				log(i);
			}
			assertFileContents(i);
		}
	}

	@Test
	public void testLoopingRun() throws IOException {
		loopingRun(LOOP_COUNT);
	}

	@Test
	public void testSingleRun() throws IOException {
		loopingRun(1);
	}

}