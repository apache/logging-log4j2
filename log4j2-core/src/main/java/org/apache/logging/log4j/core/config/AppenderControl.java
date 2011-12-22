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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.filter.Filtering;

/**
 * Wraps appenders with details the appender implementation shouldn't need to know about.
 */
public class AppenderControl {

    private ThreadLocal<AppenderControl> recursive = new ThreadLocal<AppenderControl>();

    private final Appender appender;

    /**
     * Constructor.
     * @param appender The target Appender.
     */
    public AppenderControl(Appender appender) {
        this.appender = appender;
    }

    /**
     * Return the Appender.
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
        if (recursive.get() != null) {
            appender.getHandler().error("Recursive call to appender " + appender.getName());
            return;
        }
        try {
            recursive.set(this);

            if (appender instanceof Lifecycle && !appender.isStarted()) {
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
