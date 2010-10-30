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
package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.internal.StatusLogger;
import org.apache.logging.log4j.message.Message;

/**
 * Users should extend this class to implement filters. Filters can be either context wide or attached to
 * an appender. A filter may choose to support being called only from the context or only from an appender in
 * which case it will only implement the required method(s). The rest will default to return NEUTRAL.
 *
 */
public abstract class FilterBase implements Filter, Lifecycle {

    protected boolean started;

    protected final Result onMatch;

    protected final Result onMismatch;

    protected static final org.apache.logging.log4j.Logger logger = StatusLogger.getLogger();

    protected static final String ON_MATCH = "onmatch";
    protected static final String ON_MISMATCH = "onmismatch";

    protected FilterBase() {
        this(null, null);
    }

    protected FilterBase(Result onMatch, Result onMismatch) {
        this.onMatch = onMatch == null ? Result.NEUTRAL : onMatch;
        this.onMismatch = onMismatch == null ? Result.DENY : onMismatch;
    }

    public void start() {
        started = true;
    }

    public boolean isStarted() {
        return started;
    }

    public void stop() {
        started = false;
    }

    public final Result getOnMismatch() {
        return onMismatch;
    }

    public final Result getOnMatch() {
        return onMatch;
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger the logger
     * @param level
     * @param marker
     * @param msg
     * @param params
     * @return
     */
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object[] params) {
        return Result.NEUTRAL;
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger
     * @param level
     * @param marker
     * @param msg
     * @param t
     * @return
     */
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return Result.NEUTRAL;
    }

    /**
     * Appender Filter method. The default returns NEUTRAL.
     * @param logger
     * @param level
     * @param marker
     * @param msg
     * @param t
     * @return
     */
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return Result.NEUTRAL;
    }

    /**
     * Context Filter method. The default returns NEUTRAL.
     * @param event
     * @return
     */
    public Result filter(LogEvent event) {
        return Result.NEUTRAL;
    }
}
