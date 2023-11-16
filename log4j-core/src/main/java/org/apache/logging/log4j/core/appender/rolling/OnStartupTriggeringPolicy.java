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
package org.apache.logging.log4j.core.appender.rolling;

import java.lang.reflect.Method;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Triggers a rollover on every restart, but only if the file size is greater than zero.
 */
@Plugin(name = "OnStartupTriggeringPolicy", category = Core.CATEGORY_NAME, printObject = true)
public class OnStartupTriggeringPolicy extends AbstractTriggeringPolicy {

    private static final long JVM_START_TIME = initStartTime();

    private final long minSize;

    private OnStartupTriggeringPolicy(final long minSize) {
        this.minSize = minSize;
    }

    /**
     * Returns the result of {@code ManagementFactory.getRuntimeMXBean().getStartTime()},
     * or the current system time if JMX is not available.
     */
    private static long initStartTime() {
        // LOG4J2-379:
        // We'd like to call ManagementFactory.getRuntimeMXBean().getStartTime(),
        // but Google App Engine throws a java.lang.NoClassDefFoundError
        // "java.lang.management.ManagementFactory is a restricted class".
        // The reflection is necessary because without it, Google App Engine
        // will refuse to initialize this class.
        try {
            final Class<?> factoryClass = Loader.loadSystemClass("java.lang.management.ManagementFactory");
            final Method getRuntimeMXBean = factoryClass.getMethod("getRuntimeMXBean");
            final Object runtimeMXBean = getRuntimeMXBean.invoke(null);

            final Class<?> runtimeMXBeanClass = Loader.loadSystemClass("java.lang.management.RuntimeMXBean");
            final Method getStartTime = runtimeMXBeanClass.getMethod("getStartTime");
            final Long result = (Long) getStartTime.invoke(runtimeMXBean);

            return result;
        } catch (final Throwable t) {
            StatusLogger.getLogger()
                    .error(
                            "Unable to call ManagementFactory.getRuntimeMXBean().getStartTime(), "
                                    + "using system time for OnStartupTriggeringPolicy",
                            t);
            // We have little option but to declare "now" as the beginning of time.
            return System.currentTimeMillis();
        }
    }

    /**
     * Provide the RollingFileManager to the policy.
     * @param manager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager manager) {
        if (manager.getFileTime() < JVM_START_TIME && manager.getFileSize() >= minSize) {
            StatusLogger.getLogger().debug("Initiating rollover at startup");
            if (minSize == 0) {
                manager.setRenameEmptyFiles(true);
            }
            manager.skipFooter(true);
            manager.rollover();
            manager.skipFooter(false);
        }
    }

    /**
     * Determine if a rollover should be triggered.
     * @param event   A reference to the current event.
     * @return true if the target file's timestamp is older than the JVM start time.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        return false;
    }

    @Override
    public String toString() {
        return "OnStartupTriggeringPolicy";
    }

    @PluginFactory
    public static OnStartupTriggeringPolicy createPolicy(
            @PluginAttribute(value = "minSize", defaultLong = 1) final long minSize) {
        return new OnStartupTriggeringPolicy(minSize);
    }
}
