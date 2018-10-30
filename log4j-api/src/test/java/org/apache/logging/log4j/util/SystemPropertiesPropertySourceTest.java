package org.apache.logging.log4j.util;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.Test;

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

/**
 * Tests https://issues.apache.org/jira/browse/LOG4J2-2276.
 */
public class SystemPropertiesPropertySourceTest {

	private static final int ITERATIONS = 10000;

	/**
	 * Tests avoiding a ConcurrentModificationException. For example:
	 * 
	 * <pre>
	 * java.util.ConcurrentModificationException
	 *  at java.util.Hashtable$Enumerator.next(Hashtable.java:1167)
	 *  at org.apache.logging.log4j.util.SystemPropertiesPropertySource.forEach(SystemPropertiesPropertySource.java:38)
	 *  at org.apache.logging.log4j.util.SystemPropertiesPropertySourceTest.testMultiThreadedAccess(SystemPropertiesPropertySourceTest.java:47)
	 * </pre>
	 */
	@Test
	public void testMultiThreadedAccess() throws InterruptedException, ExecutionException {
		final ExecutorService threadPool = Executors.newSingleThreadExecutor();
		try {
			final Future<?> future = threadPool.submit(new Runnable() {

				@Override
				public void run() {
					final Properties properties = System.getProperties();
					for (int i = 0; i < ITERATIONS; i++) {
						properties.setProperty("FOO_" + i, "BAR");
					}
				}
			});
			for (int i = 0; i < ITERATIONS; i++)
				new SystemPropertiesPropertySource().forEach(new BiConsumer<String, String>() {
					@Override
					public void accept(final String key, final String value) {
						// nothing
					}
				});
			future.get();
		} finally {
			threadPool.shutdown();
		}
	}

}
