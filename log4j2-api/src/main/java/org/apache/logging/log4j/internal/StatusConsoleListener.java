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
package org.apache.logging.log4j.internal;

import org.apache.logging.log4j.Level;

/**
 *
 */
public class StatusConsoleListener implements StatusListener {

    private static final String STATUS_LEVEL = "org.apache.logging.log4j.StatusLevel";

    private Level level = Level.FATAL;

    private String[] filters = null;

    public StatusConsoleListener() {
        String str = System.getProperty(STATUS_LEVEL);
        if (str != null) {
            level = Level.toLevel(str, Level.FATAL);
        }
    }

    public StatusConsoleListener(Level level) {
        this.level = level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public void log(StatusData data) {
        if (data.getLevel().greaterOrEqual(level) && !filtered(data)) {
            System.out.println(data.getFormattedStatus());
        }
    }

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
