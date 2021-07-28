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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.filter.CompositeFilter;

/**
 * Binds a Log4j 1.x Appender to Log4j 2.
 */
public class AppenderAdapter {

    private final Appender appender;
    private final Adapter adapter;

    /**
     * Constructor.
     * @param appender The Appender to wrap.
     */
    public AppenderAdapter(Appender appender) {
        this.appender = appender;
        org.apache.logging.log4j.core.Filter appenderFilter = null;
        if (appender.getFilter() != null) {
            if (appender.getFilter().getNext() != null) {
                org.apache.log4j.spi.Filter filter = appender.getFilter();
                List<org.apache.logging.log4j.core.Filter> filters = new ArrayList<>();
                while (filter != null) {
                    filters.add(new FilterAdapter(filter));
                    filter = filter.getNext();
                }
                appenderFilter = CompositeFilter.createFilters(filters.toArray(Filter.EMPTY_ARRAY));
            } else {
                appenderFilter = new FilterAdapter(appender.getFilter());
            }
        }
        this.adapter = new Adapter(appender.getName(), appenderFilter, null, true, null);
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public class Adapter extends AbstractAppender {

        protected Adapter(final String name, final Filter filter, final Layout<? extends Serializable> layout,
            final boolean ignoreExceptions, final Property[] properties) {
            super(name, filter, layout, ignoreExceptions, properties);
        }

        @Override
        public void append(LogEvent event) {
            appender.doAppend(new LogEventAdapter(event));
        }

        @Override
        public void stop() {
            appender.close();
        }

        public Appender getAppender() {
            return appender;
        }
    }
}
