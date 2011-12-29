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
package org.apache.logging.log4j.core.impl;

import java.io.Serializable;

/**
 * Package data for a StackTraceElement.
 */
public class StackTracePackageElement implements Serializable {

    private static final long serialVersionUID = -2171069569241280505L;

    private final String location;

    private final String version;

    private final boolean isExact;

    /**
     * Constructor that takes the location, version, and exact match flag.
     * @param location The location of the Class.
     * @param version The version of the component.
     * @param exact if true this is an exact package element.
     */
    public StackTracePackageElement(String location, String version, boolean exact) {
        this.location = location;
        this.version = version;
        this.isExact = exact;
    }

    /**
     * Return the location of the element.
     * @return The location of the element.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Return the version of the element.
     * @return the version of the element.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Return the indicator of whether this is an exact match.
     * @return true if the location was determined exactly.
     */
    public boolean isExact() {
        return isExact;
    }

    @Override
    public String toString() {
        String exact = isExact ? "" : "~";
        return exact + "[" + location + ":" + version + "]";
    }
}
