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
package org.apache.logging.log4j.core.net;

import org.apache.logging.log4j.Level;

/**
 * The Priority used in the Syslog system. Calculated from the Facility and Severity values.
 */
public class Priority {

    private final Facility facility;
    private final Severity severity;

    public Priority(Facility facility, Severity severity) {
        this.facility = facility;
        this.severity = severity;
    }

    public static int getPriority(Facility facility, Level level) {
        return facility.getCode() << 3 + Severity.getSeverity(level).getCode();
    }

    public Facility getFacility() {
        return facility;
    }

    public Severity getSeverity() {
        return severity;
    }

    public int getValue() {
        return facility.getCode() << 3 + severity.getCode();
    }

    public String toString() {
        return Integer.toString(getValue());
    }
}
