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
package org.apache.logging.log4j.core.appender.rolling;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.test.junit.CleanUpDirectories;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * LOG4J2-1804.
 */
public class RollingAppenderCronAndSizeTest extends AbstractRollingListenerTest {

  private static final String CONFIG = "log4j-rolling-cron-and-size.xml";

    private static final String DIR = "target/rolling-cron-size";
	// we'll probably roll over more than 30 times, but that's a sufficient amount of test data
	private final CountDownLatch rollover = new CountDownLatch(30);

	@Test
	@CleanUpDirectories(DIR)
	@LoggerContextSource(value = CONFIG, timeout = 10)
	public void testAppender(final Logger logger, @Named("RollingFile") final RollingFileManager manager) throws Exception {
		manager.addRolloverListener(this);
		Random rand = new Random(currentTimeMillis.get());
		for (int j=0; j < 100; ++j) {
			int count = rand.nextInt(100);
			for (int i = 0; i < count; ++i) {
				logger.debug("This is test message number {}", i);
			}
			currentTimeMillis.addAndGet(rand.nextInt(50));
		}

		rollover.await();

		final File dir = new File(DIR);
		assertThat(dir).isNotEmptyDirectory();
		assertThat(dir).isDirectoryContaining("glob:**.log");
		final File[] files = dir.listFiles();
		assertNotNull(files);
		Arrays.sort(files);
		int fileCounter = 0;
		String previous = "";
		for (final File file: files) {
			final String actual = file.getName();
			final String[] fileParts = actual.split("[_.]");
			fileCounter = previous.equals(fileParts[1]) ? ++fileCounter : 1;
			previous = fileParts[1];
			assertEquals(Integer.toString(fileCounter), fileParts[2],
					"Incorrect file name. Expected counter value of " + fileCounter + " in " + actual);
		}

	}

	@Override
	public void rolloverComplete(final String fileName) {
		rollover.countDown();
	}
}
