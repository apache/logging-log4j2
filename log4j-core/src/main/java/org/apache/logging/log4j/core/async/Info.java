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

import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Tuple with the event translator and thread name for a thread.
 */
class Info {
    /**
     * Strategy for deciding whether thread name should be cached or not.
     */
    enum ThreadNameStrategy { // LOG4J2-467
        CACHED {
            @Override
            public String getThreadName(final Info info) {
                return info.cachedThreadName;
            }
        },
        UNCACHED {
            @Override
            public String getThreadName(final Info info) {
                return Thread.currentThread().getName();
            }
        };
        abstract String getThreadName(Info info);

        static Info.ThreadNameStrategy create() {
            final String name = PropertiesUtil.getProperties().getStringProperty("AsyncLogger.ThreadNameStrategy",
                    CACHED.name());
            try {
                final Info.ThreadNameStrategy result = ThreadNameStrategy.valueOf(name);
                LOGGER.debug("AsyncLogger.ThreadNameStrategy={}", result);
                return result;
            } catch (final Exception ex) {
                LOGGER.debug("Using AsyncLogger.ThreadNameStrategy.CACHED: '{}' not valid: {}", name, ex.toString());
                return CACHED;
            }
        }
    }

    private static final StatusLogger LOGGER = StatusLogger.getLogger();
    private static final Info.ThreadNameStrategy THREAD_NAME_STRATEGY = ThreadNameStrategy.create();
    private static final ThreadLocal<Info> THREADLOCAL = new ThreadLocal<Info>();

    final RingBufferLogEventTranslator translator;
    final boolean isAppenderThread;
    private final String cachedThreadName;

    Info(final RingBufferLogEventTranslator translator, final String threadName, final boolean appenderThread) {
        this.translator = translator;
        this.cachedThreadName = threadName;
        this.isAppenderThread = appenderThread;
    }

    /**
     * Initialize an {@code Info} object that is threadlocal to the consumer/appender thread. This Info object
     * uniquely has attribute {@code isAppenderThread} set to {@code true}. All other Info objects will have this
     * attribute set to {@code false}. This allows us to detect Logger.log() calls initiated from the appender
     * thread, which may cause deadlock when the RingBuffer is full. (LOG4J2-471)
     * 
     * @param executor runs the appender thread
     */
    public static void initExecutorThreadInstance(final ExecutorService executor) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                final boolean isAppenderThread = true;
                final Info info = new Info(new RingBufferLogEventTranslator(), //
                        Thread.currentThread().getName(), isAppenderThread);
                THREADLOCAL.set(info);
            }
        });
    }
    
    static Info get() {
        Info result = THREADLOCAL.get();
        if (result == null) {
            // by default, set isAppenderThread to false
            result = new Info(new RingBufferLogEventTranslator(), Thread.currentThread().getName(), false);
            THREADLOCAL.set(result);
        }
        return result;
    }

    // LOG4J2-467
    String threadName() {
        return THREAD_NAME_STRATEGY.getThreadName(this);
    }
}
