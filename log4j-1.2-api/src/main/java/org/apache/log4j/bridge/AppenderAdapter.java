/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.bridge;

import java.io.Serializable;
import org.apache.log4j.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.util.Strings;

/**
 * Binds a Log4j 1.x Appender to Log4j 2.
 */
public final class AppenderAdapter {

    private final Appender appender;
    private final Adapter adapter;

    /**
     * Adapts a Log4j 1.x appender into a Log4j 2.x appender. Applying this method
     * on the result of
     * {@link AppenderWrapper#adapt(org.apache.logging.log4j.core.Appender)} should
     * return the original Log4j 2.x appender.
     *
     * @param appender a Log4j 1.x appender
     * @return a Log4j 2.x appender or {@code null} if the parameter is {@code null}
     */
    public static org.apache.logging.log4j.core.Appender adapt(final Appender appender) {
        if (appender instanceof org.apache.logging.log4j.core.Appender) {
            return (org.apache.logging.log4j.core.Appender) appender;
        }
        if (appender instanceof AppenderWrapper) {
            return ((AppenderWrapper) appender).getAppender();
        }
        if (appender != null) {
            return new AppenderAdapter(appender).getAdapter();
        }
        return null;
    }

    /**
     * Constructor.
     * @param appender The Appender to wrap.
     */
    private AppenderAdapter(final Appender appender) {
        this.appender = appender;
        final org.apache.logging.log4j.core.Filter appenderFilter = FilterAdapter.adapt(appender.getFilter());
        String name = appender.getName();
        if (Strings.isEmpty(name)) {
            name = String.format("0x%08x", appender.hashCode());
        }
        this.adapter = new Adapter(name, appenderFilter, null, true, null);
    }

    public Adapter getAdapter() {
        return adapter;
    }

    public class Adapter extends AbstractAppender {

        protected Adapter(
                final String name,
                final Filter filter,
                final Layout<? extends Serializable> layout,
                final boolean ignoreExceptions,
                final Property[] properties) {
            super(name, filter, layout, ignoreExceptions, properties);
        }

        @Override
        public void append(final LogEvent event) {
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
