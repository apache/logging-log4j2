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
package org.apache.logging.log4j.core.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates {@link Log4jThread}s.
 *
 * @since 2.7
 */
public class Log4jThreadFactory implements ThreadFactory {

    private static final String PREFIX = "TF-";

    /**
     * Creates a new daemon thread factory.
     *
     * @param threadFactoryName
     *            The thread factory name.
     * @return a new daemon thread factory.
     */
    public static Log4jThreadFactory createDaemonThreadFactory(final String threadFactoryName) {
        return new Log4jThreadFactory(threadFactoryName, true, Thread.NORM_PRIORITY);
    }

    /**
     * Creates a new thread factory.
     *
     * This is mainly used for tests. Production code should be very careful with creating
     * non-daemon threads since those will block application shutdown
     * (see https://issues.apache.org/jira/browse/LOG4J2-1748).
     *
     * @param threadFactoryName
     *            The thread factory name.
     * @return a new daemon thread factory.
     */
    public static Log4jThreadFactory createThreadFactory(final String threadFactoryName) {
        return new Log4jThreadFactory(threadFactoryName, false, Thread.NORM_PRIORITY);
    }

    private static final AtomicInteger FACTORY_NUMBER = new AtomicInteger(1);
    private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
    private final boolean daemon;
    private final ThreadGroup group;
    private final int priority;
    private final String threadNamePrefix;

    /**
     * Constructs an initialized thread factory.
     *
     * @param threadFactoryName
     *            The thread factory name.
     * @param daemon
     *            Whether to create daemon threads.
     * @param priority
     *            The thread priority.
     */
    public Log4jThreadFactory(final String threadFactoryName, final boolean daemon, final int priority) {
        this.threadNamePrefix = PREFIX + FACTORY_NUMBER.getAndIncrement() + "-" + threadFactoryName + "-";
        this.daemon = daemon;
        this.priority = priority;
        final SecurityManager securityManager = System.getSecurityManager();
        this.group = securityManager != null
                ? securityManager.getThreadGroup()
                : Thread.currentThread().getThreadGroup();
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        // Log4jThread prefixes names with "Log4j2-".
        final Thread thread = new Log4jThread(group, runnable, threadNamePrefix + THREAD_NUMBER.getAndIncrement(), 0);
        if (thread.isDaemon() != daemon) {
            thread.setDaemon(daemon);
        }
        if (thread.getPriority() != priority) {
            thread.setPriority(priority);
        }
        return thread;
    }
}
