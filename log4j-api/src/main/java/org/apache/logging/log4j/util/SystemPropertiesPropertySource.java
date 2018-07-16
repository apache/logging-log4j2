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
package org.apache.logging.log4j.util;

import java.util.Objects;
import java.util.Properties;

/**
 * PropertySource backed by the current system properties. Other than having a
 * higher priority over normal properties, this follows the same rules as
 * {@link PropertiesPropertySource}.
 *
 * @since 2.10.0
 */
public class SystemPropertiesPropertySource implements PropertySource {

	private static final int DEFAULT_PRIORITY = 100;
	private static final String PREFIX = "log4j2.";

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}

	@Override
	public void forEach(final BiConsumer<String, String> action) {
		Properties properties;
		try {
			properties = System.getProperties();
		} catch (final SecurityException e) {
			// (1) There is no status logger.
			// (2) LowLevelLogUtil also consults system properties ("line.separator") to
			// open a BufferedWriter, so this may fail as well. Just having a hard reference
			// in this code to LowLevelLogUtil would cause a problem.
			// (3) We could log to System.err (nah) or just be quiet as we do now.
			return;
		}
		// Lock properties only long enough to get a thread-safe SAFE snapshot of its
		// current keys, an array.
		final Object[] keySet;
		synchronized (properties) {
			keySet = properties.keySet().toArray();
		}
		// Then traverse for an unknown amount of time.
		// Some keys may now be absent, in which case, the value is null.
		for (final Object key : keySet) {
			final String keyStr = Objects.toString(key, null);
			action.accept(keyStr, properties.getProperty(keyStr));
		}
	}

	@Override
	public CharSequence getNormalForm(final Iterable<? extends CharSequence> tokens) {
		return PREFIX + Util.joinAsCamelCase(tokens);
	}

}
