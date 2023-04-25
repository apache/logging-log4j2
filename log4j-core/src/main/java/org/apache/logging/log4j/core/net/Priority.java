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
 * The Priority used in the Syslog system. Calculated from the Facility and Severity values.
 */
public class Priority {

    private final Facility facility;
    private final Severity severity;

    /**
     * The Constructor.
     * @param facility The Facility.
     * @param severity The Severity.
     */
    public Priority(final Facility facility, final Severity severity) {
        this.facility = facility;
        this.severity = severity;
    }

    /**
     * Returns the priority value based on the Facility and Log Level.
     * @param facility The Facility.
     * @param level The Level.
     * @return The integer value of the priority.
     */
    public static int getPriority(final Facility facility, final Level level) {
        return toPriority(facility, Severity.getSeverity(level));
    }

    private static int toPriority(final Facility aFacility, final Severity aSeverity) {
        return (aFacility.getCode() << 3) + aSeverity.getCode();
    }

    /**
     * Returns the Facility.
     * @return the Facility.
     */
    public Facility getFacility() {
        return facility;
    }

    /**
     * Returns the Severity.
     * @return the Severity.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Returns the value of this Priority.
     * @return the value of this Priority.
     */
    public int getValue() {
        return toPriority(facility, severity);
    }

    @Override
    public String toString() {
        return Integer.toString(getValue());
    }
}
