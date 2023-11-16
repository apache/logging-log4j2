/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.async;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.util.Integers;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Strategy for deciding whether thread name should be cached or not.
 */
public enum ThreadNameCachingStrategy { // LOG4J2-467
    CACHED {
        @Override
        public String getThreadName() {
            String result = THREADLOCAL_NAME.get();
            if (result == null) {
                result = Thread.currentThread().getName();
                THREADLOCAL_NAME.set(result);
            }
            return result;
        }
    },
    UNCACHED {
        @Override
        public String getThreadName() {
            return Thread.currentThread().getName();
        }
    };

    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final ThreadLocal<String> THREADLOCAL_NAME = new ThreadLocal<>();
    static final ThreadNameCachingStrategy DEFAULT_STRATEGY = isAllocatingThreadGetName() ? CACHED : UNCACHED;

    abstract String getThreadName();

    public static ThreadNameCachingStrategy create() {
        final String name = PropertiesUtil.getProperties().getStringProperty("AsyncLogger.ThreadNameStrategy");
        try {
            final ThreadNameCachingStrategy result =
                    name != null ? ThreadNameCachingStrategy.valueOf(name) : DEFAULT_STRATEGY;
            LOGGER.debug(
                    "AsyncLogger.ThreadNameStrategy={} (user specified {}, default is {})",
                    result.name(),
                    name,
                    DEFAULT_STRATEGY.name());
            return result;
        } catch (final Exception ex) {
            LOGGER.debug(
                    "Using AsyncLogger.ThreadNameStrategy.{}: '{}' not valid: {}",
                    DEFAULT_STRATEGY.name(),
                    name,
                    ex.toString());
            return DEFAULT_STRATEGY;
        }
    }

    static boolean isAllocatingThreadGetName() {
        // LOG4J2-2052, LOG4J2-2635 JDK 8u102 ("1.8.0_102") removed the String allocation in Thread.getName()
        if (Constants.JAVA_MAJOR_VERSION == 8) {
            try {
                final Pattern javaVersionPattern = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)_(\\d+)");
                final Matcher m = javaVersionPattern.matcher(System.getProperty("java.version"));
                if (m.matches()) {
                    return Integers.parseInt(m.group(3)) == 0 && Integers.parseInt(m.group(4)) < 102;
                }
                return true;
            } catch (Exception e) {
                return true;
            }
        } else {
            return Constants.JAVA_MAJOR_VERSION < 8;
        }
    }
}
