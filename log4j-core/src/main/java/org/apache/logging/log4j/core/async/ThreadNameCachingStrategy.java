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
enum ThreadNameCachingStrategy { // LOG4J2-467
    CACHED {
        @Override
        public long getThreadId() {
            Long result = THREADLOCAL_ID.get();
            if (result == null) {
                result = Thread.currentThread().getId();
                THREADLOCAL_ID.set(result);
            }
            return result;
        }

        @Override
        public String getThreadName() {
            String result = THREADLOCAL_NAME.get();
            if (result == null) {
                result = Thread.currentThread().getName();
                THREADLOCAL_NAME.set(result);
            }
            return result;
        }

        @Override
        public int getThreadPriority() {
            Integer result = THREADLOCAL_PRIORITY.get();
            if (result == null) {
                result = Thread.currentThread().getPriority();
                THREADLOCAL_PRIORITY.set(result);
            }
            return result;
        }
    },
    UNCACHED {
        @Override
        public long getThreadId() {
            return Thread.currentThread().getId();
        }

        @Override
        public String getThreadName() {
            return Thread.currentThread().getName();
        }

        @Override
        public int getThreadPriority() {
            return Thread.currentThread().getPriority();
        }
    };
    
    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final ThreadLocal<Long> THREADLOCAL_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> THREADLOCAL_NAME = new ThreadLocal<>();
    private static final ThreadLocal<Integer> THREADLOCAL_PRIORITY = new ThreadLocal<>();
    
    abstract long getThreadId();
    abstract String getThreadName();
    abstract int getThreadPriority();

    static ThreadNameCachingStrategy create() {
        final String name = PropertiesUtil.getProperties().getStringProperty("AsyncLogger.ThreadNameStrategy",
                CACHED.name());
        try {
            final ThreadNameCachingStrategy result = ThreadNameCachingStrategy.valueOf(name);
            LOGGER.debug("AsyncLogger.ThreadNameStrategy={}", result);
            return result;
        } catch (final Exception ex) {
            LOGGER.debug("Using AsyncLogger.ThreadNameStrategy.CACHED: '{}' not valid: {}", name, ex.toString());
            return CACHED;
        }
    }
}