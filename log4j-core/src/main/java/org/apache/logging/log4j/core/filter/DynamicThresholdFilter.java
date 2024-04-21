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
package org.apache.logging.log4j.core.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.impl.ContextData;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

/**
 * Compares against a log level that is associated with a context value. By default the context is the
 * {@link ThreadContext}, but users may {@linkplain ContextDataProvider configure} a custom
 * {@link ContextDataProvider} which obtains context data from some other source.
 */
@Configurable(elementType = Filter.ELEMENT_TYPE, printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public final class DynamicThresholdFilter extends AbstractFilter {

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends AbstractFilter.AbstractFilterBuilder<Builder>
            implements Supplier<DynamicThresholdFilter> {
        private String key;
        private KeyValuePair[] pairs;
        private Level defaultThreshold;

        public Builder setKey(@PluginAttribute final String key) {
            this.key = key;
            return this;
        }

        public Builder setPairs(@PluginElement final KeyValuePair[] pairs) {
            this.pairs = pairs;
            return this;
        }

        public Builder setDefaultThreshold(@PluginAttribute final Level defaultThreshold) {
            this.defaultThreshold = defaultThreshold;
            return this;
        }

        @Override
        public DynamicThresholdFilter get() {
            if (defaultThreshold == null) {
                defaultThreshold = Level.ERROR;
            }
            final Map<String, Level> map = Stream.of(pairs)
                    .collect(Collectors.toMap(KeyValuePair::getKey, pair -> Level.toLevel(pair.getValue())));
            return new DynamicThresholdFilter(key, map, defaultThreshold, getOnMatch(), getOnMismatch());
        }
    }

    private final Level defaultThreshold;
    private final String key;
    private final Map<String, Level> levelMap;

    private DynamicThresholdFilter(
            final String key,
            final Map<String, Level> pairs,
            final Level defaultLevel,
            final Result onMatch,
            final Result onMismatch) {
        super(onMatch, onMismatch);
        // ContextDataFactory looks up a property. The Spring PropertySource may log which will cause recursion.
        // By initializing the ContextDataFactory here recursion will be prevented.
        final StringMap map = ContextDataFactory.createContextData();
        LOGGER.debug(
                "Successfully initialized ContextDataFactory by retrieving the context data with {} entries",
                map.size());
        Objects.requireNonNull(key, "key cannot be null");
        this.key = key;
        this.levelMap = pairs;
        this.defaultThreshold = defaultLevel;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equalsImpl(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DynamicThresholdFilter other = (DynamicThresholdFilter) obj;
        if (defaultThreshold == null) {
            if (other.defaultThreshold != null) {
                return false;
            }
        } else if (!defaultThreshold.equals(other.defaultThreshold)) {
            return false;
        }
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (levelMap == null) {
            if (other.levelMap != null) {
                return false;
            }
        } else if (!levelMap.equals(other.levelMap)) {
            return false;
        }
        return true;
    }

    private Result filter(final Level level, final ReadOnlyStringMap contextMap) {
        final String value = contextMap.getValue(key);
        if (value != null) {
            Level ctxLevel = levelMap.get(value);
            if (ctxLevel == null) {
                ctxLevel = defaultThreshold;
            }
            return level.isMoreSpecificThan(ctxLevel) ? onMatch : onMismatch;
        }
        return Result.NEUTRAL;
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter(event.getLevel(), event.getContextData());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter(level, currentContextData());
    }

    private ReadOnlyStringMap currentContextData() {
        Map<String, String> contextData = new HashMap<>();
        ContextData.addAll(contextData);
        return new JdkMapAdapterStringMap(contextData, true);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return filter(level, currentContextData());
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return filter(level, currentContextData());
    }

    public String getKey() {
        return this.key;
    }

    public Map<String, Level> getLevelMap() {
        return levelMap;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCodeImpl();
        result = prime * result + ((defaultThreshold == null) ? 0 : defaultThreshold.hashCode());
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((levelMap == null) ? 0 : levelMap.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("key=").append(key);
        sb.append(", default=").append(defaultThreshold);
        if (levelMap.size() > 0) {
            sb.append('{');
            boolean first = true;
            for (final Map.Entry<String, Level> entry : levelMap.entrySet()) {
                if (!first) {
                    sb.append(", ");
                    first = false;
                }
                sb.append(entry.getKey()).append('=').append(entry.getValue());
            }
            sb.append('}');
        }
        return sb.toString();
    }
}
