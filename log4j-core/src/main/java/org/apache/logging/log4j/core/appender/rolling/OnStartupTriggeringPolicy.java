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

import java.lang.reflect.Method;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.status.StatusLogger;

/**
 * Triggers a rollover on every restart. The target file's timestamp is compared with the JVM start time
 * and if it is older isTriggeringEvent will return true. After isTriggeringEvent has been called it will
 * always return false.
 */
@Plugin(name = "OnStartupTriggeringPolicy", category = "Core", printObject = true)
public class OnStartupTriggeringPolicy implements TriggeringPolicy {
    private static long JVM_START_TIME = initStartTime();

    private boolean evaluated = false;
    private RollingFileManager manager;

    /**
     * Provide the RollingFileManager to the policy.
     * @param manager The RollingFileManager.
     */
    @Override
    public void initialize(final RollingFileManager manager) {
        this.manager = manager;
        if (JVM_START_TIME == 0) {
            evaluated = true;
        }
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

            return result.longValue();
        } catch (final Throwable t) {
            StatusLogger.getLogger().error("Unable to call ManagementFactory.getRuntimeMXBean().getStartTime(), " //
                    + "using system time for OnStartupTriggeringPolicy", t);
            // We have little option but to declare "now" as the beginning of time.
            return System.currentTimeMillis();
        }
    }

    /**
     * Determine if a rollover should be triggered.
     * @param event   A reference to the current event.
     * @return true if the target file's timestamp is older than the JVM start time.
     */
    @Override
    public boolean isTriggeringEvent(final LogEvent event) {
        if (evaluated) {
            return false;
        }
        evaluated = true;
        return manager.getFileTime() < JVM_START_TIME;
    }

    @Override
    public String toString() {
        return "OnStartupTriggeringPolicy";
    }

    @PluginFactory
    public static OnStartupTriggeringPolicy createPolicy() {
        return new OnStartupTriggeringPolicy();
    }
}
