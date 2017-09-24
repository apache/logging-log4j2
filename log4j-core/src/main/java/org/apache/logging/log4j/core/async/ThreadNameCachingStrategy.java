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

package org.apache.logging.log4j.core.async;

import org.apache.logging.log4j.status.StatusLogger;
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

    abstract String getThreadName();

    public static ThreadNameCachingStrategy create() {
        final String defaultStrategy = System.getProperty("java.version").compareTo("1.8.0_102") < 0
                ? "CACHED" // LOG4J2-2052 JDK 8u102 removed the String allocation in Thread.getName()
                : "UNCACHED";
        final String name = PropertiesUtil.getProperties().getStringProperty("AsyncLogger.ThreadNameStrategy");
        try {
            final ThreadNameCachingStrategy result = ThreadNameCachingStrategy.valueOf(
                    name != null ? name : defaultStrategy);
            LOGGER.debug("AsyncLogger.ThreadNameStrategy={} (user specified {}, default is {})",
                    result, name, defaultStrategy);
            return result;
        } catch (final Exception ex) {
            LOGGER.debug("Using AsyncLogger.ThreadNameStrategy.{}: '{}' not valid: {}",
                    defaultStrategy, name, ex.toString());
            return ThreadNameCachingStrategy.valueOf(defaultStrategy);
        }
    }
}