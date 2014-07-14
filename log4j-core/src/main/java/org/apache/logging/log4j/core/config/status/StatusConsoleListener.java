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
package org.apache.logging.log4j.core.config.status;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;

/**
 * StatusListener that writes to the Console.
 */
public abstract class StatusConsoleListener implements StatusListener {

    protected Level level;
    private String[] filters = null;

    /**
     * Creates the StatusConsoleListener using the supplied Level. 
     */
    public StatusConsoleListener() {
        this(Level.FATAL);
    }

    /**
     * Creates the StatusConsoleListener using the supplied Level. 
     * @param level The Level of status messages that should appear on the console.
     */
    public StatusConsoleListener(final Level level) {
        this.level = level;
    }

    /**
     * Sets the level to a new value.
     * @param level The new Level.
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    public Object getLevel() {
		return level;
	}

	/**
     * Adds package name filters to exclude.
     * @param filters An array of package names to exclude.
     */
    public void setFilters(final String... filters) {
        this.filters = filters;
    }

    protected boolean isEnabledFor(final StatusData data) {
        if (level.isMoreSpecificThan(data.getLevel())) {
        	return false;
        } 
        if (filters == null) {
        	return true;
        }
        final String caller = data.getStackTraceElement().getClassName();
        for (final String filter : filters) {
            if (caller.startsWith(filter)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        // don't close system streams
    }
}
