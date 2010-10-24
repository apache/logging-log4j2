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

import org.apache.logging.log4j.core.ErrorHandler;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderRuntimeException;
import org.apache.logging.log4j.core.appender.DefaultErrorHandler;

/**
 *
 */
public class AppenderControl {

    private final Appender appender;

    AppenderControl(Appender appender) {
        this.appender = appender;
    }

    public Appender getAppender() {
        return appender;
    }

    public void callAppender(LogEvent event) {
        if (!appender.isStarted()) {
            appender.getHandler().error("Attempted to append to non-started appender " + appender.getName());

            if (!appender.suppressException()) {
                throw new AppenderRuntimeException("Attempted to append to non-started appender " + appender.getName());
            }
        }

        Filter.Result result = Filter.Result.NEUTRAL;

        for (Filter filter : appender.getFilters()) {
            result = filter.filter(event);
            if (result != Filter.Result.NEUTRAL) {
                break;
            }
        }
        if (result == Filter.Result.DENY) {
            return;
        }

        try {
            appender.append(event);
        } catch (Exception ex) {
            appender.getHandler().error("An exception occurred processing Appender " + appender.getName(), ex);
            if (!appender.suppressException()) {
                throw new AppenderRuntimeException(ex);
            }
        }
    }

}
