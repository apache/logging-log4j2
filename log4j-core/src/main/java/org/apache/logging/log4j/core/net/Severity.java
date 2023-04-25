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
package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.Level;

/**
 *  Severity values used by the Syslog system.
 *
 * <table summary="Syslog Severity Values">
 *     <tr>
 *         <th>Numerical Code</th>
 *         <th>Severity</th>
 *     </tr>
 *     <tr>
 *         <td>0</td>
 *         <td>Emergency: system is unusable</td>
 *     </tr>
 *     <tr>
 *         <td>1</td>
 *         <td>Alert: action must be taken immediately</td>
 *     </tr>
 *     <tr>
 *         <td>2</td>
 *         <td>Critical: critical conditions</td>
 *     </tr>
 *     <tr>
 *         <td>3</td>
 *         <td>Error: error conditions</td>
 *     </tr>
 *     <tr>
 *         <td>4</td>
 *         <td>Warning: warning conditions</td>
 *     </tr>
 *     <tr>
 *         <td>5</td>
 *         <td>Notice: normal but significant condition</td>
 *     </tr>
 *     <tr>
 *         <td>6</td>
 *         <td>Informational: informational messages</td>
 *     </tr>
 *     <tr>
 *         <td>7</td>
 *         <td>Debug: debug-level messages</td>
 *     </tr>
 * </table>
 */
public enum Severity {
    /** System is unusable. */
    EMERG(0),
    /** Action must be taken immediately. */
    ALERT(1),
    /** Critical conditions. */
    CRITICAL(2),
    /** Error conditions. */
    ERROR(3),
    /** Warning conditions. */
    WARNING(4),
    /** Normal but significant conditions. */
    NOTICE(5),
    /** Informational messages. */
    INFO(6),
    /** Debug level messages. */
    DEBUG(7);

    private final int code;

    Severity(final int code) {
        this.code = code;
    }

    /**
     * Returns the severity code.
     * @return The numeric value associated with the Severity.
     */
    public int getCode() {
        return this.code;
    }

    /**
     * Determine if the name matches this Severity.
     * @param name the name to match.
     * @return true if the name matches, false otherwise.
     */
    public boolean isEqual(final String name) {
        return this.name().equalsIgnoreCase(name);
    }

    /**
     * Returns the Severity for the specified Level.
     * @param level The Level.
     * @return The matching Severity, or DEBUG if there is no match.
     */
    public static Severity getSeverity(final Level level) {
        switch (level.getStandardLevel()) {
            case ALL:
                return DEBUG;
            case TRACE:
                return DEBUG;
            case DEBUG:
                return DEBUG;
            case INFO:
                return INFO;
            case WARN:
                return WARNING;
            case ERROR:
                return ERROR;
            case FATAL:
                return ALERT;
            case OFF:
                return EMERG;
        }
        return DEBUG;
    }
}
