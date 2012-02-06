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
package org.apache.logging.log4j.status;

import org.apache.logging.log4j.Level;

/**
 * StatusListener that writes to the Console.
 */
public class StatusConsoleListener implements StatusListener {

    private static final String STATUS_LEVEL = "org.apache.logging.log4j.StatusLevel";

    private Level level = Level.FATAL;

    private String[] filters = null;

    /**
     * Creates the StatusConsoleListener using either the level configured by the
     * "org.apache.logging.log4j.StatusLevel" system property if it is set or to a
     * default value of FATAL.
     */
    public StatusConsoleListener() {
        String str = System.getProperty(STATUS_LEVEL);
        if (str != null) {
            level = Level.toLevel(str, Level.FATAL);
        }
    }

    /**
     * Creates the StatusConsoleListener using the supplied Level.
     * @param level The Level of status messages that should appear on the console.
     */
    public StatusConsoleListener(Level level) {
        this.level = level;
    }

    /**
     * Sets the level to a new value.
     * @param level The new Level.
     */
    public void setLevel(Level level) {
        this.level = level;
    }

    /**
     * Writes status messages to the console.
     * @param data The StatusData.
     */
    public void log(StatusData data) {
        if (data.getLevel().isAtLeastAsSpecificAs(level) && !filtered(data)) {
            System.out.println(data.getFormattedStatus());
        }
    }

    /**
     * Adds package name filters to exclude.
     * @param filters An array of package names to exclude.
     */
    public void setFilters(String[] filters) {
        this.filters = filters;
    }

    private boolean filtered(StatusData data) {
        if (filters == null) {
            return false;
        }
        String caller = data.getStackTraceElement().getClassName();
        for (String filter : filters) {
            if (caller.startsWith(filter)) {
                return true;
            }
        }
        return false;
    }

}
