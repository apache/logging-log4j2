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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.filter.Filtering;

/**
 * Wraps an {@link Appender} with details an appender implementation shouldn't need to know about.
 */
public class AppenderControl extends Filterable {

    private final ThreadLocal<AppenderControl> recursive = new ThreadLocal<AppenderControl>();

    private final Appender appender;

    private final Level level;
    private final int intLevel;

    /**
     * Constructor.
     * @param appender The target Appender.
     * @param level the Level to filter on.
     * @param filter the Filter(s) to apply.
     */
    public AppenderControl(Appender appender, Level level, Filter filter) {
        super(filter);
        this.appender = appender;
        this.level = level;
        this.intLevel = level == null ? Level.ALL.intLevel() : level.intLevel();
        startFilter();
    }

    /**
     * Returns the Appender.
     * @return the Appender.
     */
    public Appender getAppender() {
        return appender;
    }

    /**
     * Call the appender.
     * @param event The event to process.
     */
    public void callAppender(LogEvent event) {
        if (getFilter() != null) {
            Filter.Result r = getFilter().filter(event);
            if (r == Filter.Result.DENY) {
                return;
            }
        }
        if (level != null) {
            if (intLevel < event.getLevel().intLevel()) {
                return;
            }
        }
        if (recursive.get() != null) {
            appender.getHandler().error("Recursive call to appender " + appender.getName());
            return;
        }
        try {
            recursive.set(this);

            if (appender instanceof LifeCycle && !appender.isStarted()) {
                appender.getHandler().error("Attempted to append to non-started appender " + appender.getName());

                if (!appender.isExceptionSuppressed()) {
                    throw new AppenderRuntimeException(
                        "Attempted to append to non-started appender " + appender.getName());
                }
            }

            if (appender instanceof Filtering && ((Filtering) appender).isFiltered(event)) {
                return;
            }

            try {
                appender.append(event);
            } catch (RuntimeException ex) {
                appender.getHandler().error("An exception occurred processing Appender " + appender.getName(), ex);
                if (!appender.isExceptionSuppressed()) {
                    throw ex;
                }
            } catch (Exception ex) {
                appender.getHandler().error("An exception occurred processing Appender " + appender.getName(), ex);
                if (!appender.isExceptionSuppressed()) {
                    throw new AppenderRuntimeException(ex);
                }
            }
        } finally {
            recursive.set(null);
        }
    }

}
