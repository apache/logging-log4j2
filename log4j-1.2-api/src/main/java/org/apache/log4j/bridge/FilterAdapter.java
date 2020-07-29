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
package org.apache.log4j.bridge;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * Binds a Log4j 1.x Filter with Log4j 2.
 */
public class FilterAdapter extends AbstractFilter {

    private final Filter filter;

    public FilterAdapter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public void start() {
        filter.activateOptions();
    }

    @Override
    public Result filter(LogEvent event) {
        LoggingEvent loggingEvent = new LogEventAdapter(event);
        Filter next = filter;
        while (next != null) {
            switch (filter.decide(loggingEvent)) {
                case Filter.ACCEPT:
                    return Result.ACCEPT;
                case Filter.DENY:
                    return Result.DENY;
                default:
            }
            next = filter.getNext();
        }
        return Result.NEUTRAL;
    }
}
