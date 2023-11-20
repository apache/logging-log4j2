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

import java.util.List;
import javax.management.ObjectName;
import org.apache.logging.log4j.status.StatusData;

/**
 * The MBean interface for monitoring and managing the {@code StatusLogger}.
 */
public interface StatusLoggerAdminMBean {
    /**
     * ObjectName pattern ({@value}) for StatusLoggerAdmin MBeans.
     * This pattern contains a variable, which is the name of the logger context.
     * <p>
     * You can find all registered StatusLoggerAdmin MBeans like this:
     * </p>
     * <pre>
     * MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
     * String pattern = String.format(StatusLoggerAdminMBean.PATTERN, &quot;*&quot;);
     * Set&lt;ObjectName&gt; statusLoggerNames = mbs.queryNames(new ObjectName(pattern), null);
     * </pre>
     * <p>
     * Some characters are not allowed in ObjectNames. The logger context name
     * may be quoted. When StatusLoggerAdmin MBeans are
     * registered, their ObjectNames are created using this pattern as follows:
     * </p>
     * <pre>
     * String ctxName = Server.escape(loggerContext.getName());
     * String name = String.format(PATTERN, ctxName);
     * ObjectName objectName = new ObjectName(name);
     * </pre>
     * @see Server#escape(String)
     */
    String PATTERN = Server.DOMAIN + ":type=%s,component=StatusLogger";

    /**
     * Notifications with this type have a {@code StatusData} userData object
     * and a {@code null} message.
     */
    String NOTIF_TYPE_DATA = "com.apache.logging.log4j.core.jmx.statuslogger.data";

    /**
     * Notifications with this type have a formatted status data message string
     * but no {@code StatusData} in their userData field.
     */
    String NOTIF_TYPE_MESSAGE = "com.apache.logging.log4j.core.jmx.statuslogger.message";

    /**
     * Returns the {@code ObjectName} that this status logger mbean is registered with.
     * @return the ObjectName of this StatusLogger MBean
     */
    ObjectName getObjectName();

    /**
     * Returns a list with the most recent {@code StatusData} objects in the
     * status history. The list has up to 200 entries by default but the length
     * can be configured with system property {@code "log4j2.status.entries"}.
     * <p>
     * Note that the returned objects may contain {@code Throwable}s from
     * external libraries.
     * </p>
     * <p>
     * JMX clients calling this method must be prepared to deal with the errors
     * that occur if they do not have the class definition for such
     * {@code Throwable}s in their classpath.
     * </p>
     *
     * @return the most recent messages logged by the {@code StatusLogger}.
     */
    List<StatusData> getStatusData();

    /**
     * Returns a string array with the most recent messages in the status
     * history. The list has up to 200 entries by default but the length can be
     * configured with system property {@code "log4j2.status.entries"}.
     *
     * @return the most recent messages logged by the {@code StatusLogger}.
     */
    String[] getStatusDataHistory();

    /**
     * Returns the {@code StatusLogger} level as a String.
     *
     * @return the {@code StatusLogger} level.
     */
    String getLevel();

    /**
     * Sets the {@code StatusLogger} level to the specified value.
     *
     * @param level the new {@code StatusLogger} level.
     * @throws IllegalArgumentException if the specified level is not one of
     *             "OFF", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE",
     *             "ALL"
     */
    void setLevel(String level);

    /**
     * Returns the name of the LoggerContext that the {@code StatusLogger} is associated with.
     * @return logger context name
     */
    String getContextName();
}
