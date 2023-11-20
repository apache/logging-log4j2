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
package org.apache.logging.log4j.core.jmx;

/**
 * The MBean interface for monitoring and managing a {@code LoggerConfig}.
 */
public interface LoggerConfigAdminMBean {
    /**
     * ObjectName pattern ({@value}) for LoggerConfigAdmin MBeans.
     * This pattern contains two variables, where the first is the name of the
     * context, the second is the name of the instrumented logger config.
     * <p>
     * You can find all registered LoggerConfigAdmin MBeans like this:
     * </p>
     * <pre>
     * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
     * String pattern = String.format(LoggerConfigAdminMBean.PATTERN, &quot;*&quot;, &quot;*&quot;);
     * Set&lt;ObjectName&gt; loggerConfigNames = mbs.queryNames(new ObjectName(pattern), null);
     * </pre>
     * <p>
     * Some characters are not allowed in ObjectNames. The logger context name
     * and logger config name may be quoted. When LoggerConfigAdmin MBeans are
     * registered, their ObjectNames are created using this pattern as follows:
     * </p>
     * <pre>
     * String ctxName = Server.escape(loggerContext.getName());
     * String loggerConfigName = Server.escape(loggerConfig.getName());
     * String name = String.format(PATTERN, ctxName, loggerConfigName);
     * ObjectName objectName = new ObjectName(name);
     * </pre>
     * @see Server#escape(String)
     */
    String PATTERN = Server.DOMAIN + ":type=%s,component=Loggers,name=%s";

    /**
     * Returns the name of the instrumented {@code LoggerConfig}.
     *
     * @return the name of the LoggerConfig
     */
    String getName();

    /**
     * Returns the {@code LoggerConfig} level as a String.
     *
     * @return the {@code LoggerConfig} level.
     */
    String getLevel();

    /**
     * Sets the {@code LoggerConfig} level to the specified value.
     *
     * @param level the new {@code LoggerConfig} level.
     * @throws IllegalArgumentException if the specified level is not one of
     *             "OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE",
     *             "ALL"
     */
    void setLevel(String level);

    /**
     * Returns whether the instrumented {@code LoggerConfig} is additive.
     *
     * @return {@code true} if the LoggerConfig is additive, {@code false}
     *         otherwise
     */
    boolean isAdditive();

    /**
     * Sets whether the instrumented {@code LoggerConfig} should be additive.
     *
     * @param additive {@code true} if the instrumented LoggerConfig should be
     *            additive, {@code false} otherwise
     */
    void setAdditive(boolean additive);

    /**
     * Returns whether the instrumented {@code LoggerConfig} is configured to
     * include location.
     *
     * @return whether location should be passed downstream
     */
    boolean isIncludeLocation();

    /**
     * Returns a string description of all filters configured for the
     * instrumented {@code LoggerConfig}.
     *
     * @return a string description of all configured filters for this
     *         LoggerConfig
     */
    String getFilter();

    /**
     * Returns a String array with the appender refs configured for the
     * instrumented {@code LoggerConfig}.
     *
     * @return the appender refs for the instrumented {@code LoggerConfig}.
     */
    String[] getAppenderRefs();
}
