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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Filter based on a value in the Thread Context Map (MDC).
 */
@Configurable(elementType = Filter.ELEMENT_TYPE, printObject = true)
@Plugin
@PluginAliases("ContextMapFilter")
@PerformanceSensitive("allocation")
public class ThreadContextMapFilter extends MapFilter {

    private final ContextDataInjector injector;
    private final String key;
    private final String value;

    private final boolean useMap;

    public ThreadContextMapFilter(
            final Map<String, List<String>> pairs, final boolean oper, final Result onMatch,
            final Result onMismatch, final ContextDataInjector injector) {
        super(pairs, oper, onMatch, onMismatch);
        // ContextDataFactory looks up a property. The Spring PropertySource may log which will cause recursion.
        // By initializing the ContextDataFactory here recursion will be prevented.
        StringMap map = ContextDataFactory.createContextData();
        LOGGER.debug("Successfully initialized ContextDataFactory by retrieving the context data with {} entries",
                map.size());
        if (pairs.size() == 1) {
            final Iterator<Map.Entry<String, List<String>>> iter = pairs.entrySet().iterator();
            final Map.Entry<String, List<String>> entry = iter.next();
            if (entry.getValue().size() == 1) {
                this.key = entry.getKey();
                this.value = entry.getValue().get(0);
                this.useMap = false;
            } else {
                this.key = null;
                this.value = null;
                this.useMap = true;
            }
        } else {
            this.key = null;
            this.value = null;
            this.useMap = true;
        }
        this.injector = injector;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
                         final Object... params) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Object msg,
                         final Throwable t) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final Message msg,
                         final Throwable t) {
        return filter();
    }

    private Result filter() {
        boolean match = false;
        if (useMap) {
            ReadOnlyStringMap currentContextData = null;
            final IndexedReadOnlyStringMap map = getStringMap();
            for (int i = 0; i < map.size(); i++) {
                if (currentContextData == null) {
                    currentContextData = currentContextData();
                }
                final String toMatch = currentContextData.getValue(map.getKeyAt(i));
                match = toMatch != null && ((List<String>) map.getValueAt(i)).contains(toMatch);
                if ((!isAnd() && match) || (isAnd() && !match)) {
                    break;
                }
            }
        } else {
            match = value.equals(currentContextData().getValue(key));
        }
        return match ? onMatch : onMismatch;
    }

    private ReadOnlyStringMap currentContextData() {
        return injector.rawContextData();
    }

    @Override
    public Result filter(final LogEvent event) {
        return super.filter(event.getContextData()) ? onMatch : onMismatch;
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8) {
        return filter();
    }

    @Override
    public Result filter(final Logger logger, final Level level, final Marker marker, final String msg,
            final Object p0, final Object p1, final Object p2, final Object p3,
            final Object p4, final Object p5, final Object p6,
            final Object p7, final Object p8, final Object p9) {
        return filter();
    }

    public static class Builder extends AbstractFilterBuilder<Builder> implements Supplier<ThreadContextMapFilter> {
        private KeyValuePair[] pairs;
        private String operator;
        private ContextDataInjector contextDataInjector;

        public Builder setPairs(@Required @PluginElement final KeyValuePair[] pairs) {
            this.pairs = pairs;
            return this;
        }

        public Builder setOperator(@PluginAttribute final String operator) {
            this.operator = operator;
            return this;
        }

        @Inject
        public Builder setContextDataInjector(final ContextDataInjector contextDataInjector) {
            this.contextDataInjector = contextDataInjector;
            return this;
        }

        @Override
        public ThreadContextMapFilter get() {
            if (pairs == null || pairs.length == 0) {
                LOGGER.error("key and value pairs must be specified for the ThreadContextMapFilter");
                return null;
            }
            final Map<String, List<String>> map = new HashMap<>();
            for (final KeyValuePair pair : pairs) {
                final String key = pair.getKey();
                if (key == null) {
                    LOGGER.error("A null key is not valid in MapFilter");
                    continue;
                }
                final String value = pair.getValue();
                if (value == null) {
                    LOGGER.error("A null value for key " + key + " is not allowed in MapFilter");
                    continue;
                }
                List<String> list = map.get(pair.getKey());
                if (list != null) {
                    list.add(value);
                } else {
                    list = new ArrayList<>();
                    list.add(value);
                    map.put(pair.getKey(), list);
                }
            }
            if (map.isEmpty()) {
                LOGGER.error("ThreadContextMapFilter is not configured with any valid key value pairs");
                return null;
            }
            final boolean isAnd = operator == null || !operator.equalsIgnoreCase("or");
            return new ThreadContextMapFilter(map, isAnd, getOnMatch(), getOnMismatch(), contextDataInjector);
        }
    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Deprecated(since = "3.0.0", forRemoval = true)
    public static ThreadContextMapFilter createFilter(
            final KeyValuePair[] pairs, final String operator, final Result onMatch, final Result onMismatch) {
        return newBuilder()
                .setPairs(pairs)
                .setOperator(operator)
                .setOnMatch(onMatch)
                .setOnMismatch(onMismatch)
                .setContextDataInjector(ContextDataInjectorFactory.createInjector())
                .get();
    }
}
