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
package org.apache.logging.log4j.core.jmx;

import java.util.List;

import org.apache.logging.log4j.status.StatusData;

/**
 * The MBean interface for monitoring and managing the {@code StatusLogger}.
 */
public interface StatusLoggerAdminMBean {
    /** Object name ({@value}) of this MBean. */
    String NAME = "org.apache.logging.log4j2:type=StatusLogger";

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
     * Returns a list with the most recent {@code StatusData} objects in the
     * status history. The list has up to 200 entries by default but the length
     * can be configured with system property {@code "log4j2.status.entries"}.
     * <p>
     * Note that the returned objects may contain {@code Throwable}s from
     * external libraries.
     *
     * JMX clients calling this method must be prepared to deal with the errors
     * that occur if they do not have the class definition for such
     * {@code Throwable}s in their classpath.
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

}
