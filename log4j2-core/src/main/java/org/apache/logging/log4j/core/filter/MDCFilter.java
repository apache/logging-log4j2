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
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

/**
 *
 */
@Plugin(name="MDC", type="Core", elementType="filter")
public class MDCFilter extends FilterBase {
    private final String key;
    private final String value;

    private static final String KEY = "key";
    private static final String VALUE = "value";

    public MDCFilter(String key, String value, Result onMatch, Result onMismatch) {
        super(onMatch, onMismatch);
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        }
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }
     public Result filter(Logger logger, Level level, Marker marker, String msg, Object[] params) {
        return filter(ThreadContext.get(key));
    }

    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(ThreadContext.get(key));
    }

    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(ThreadContext.get(key));
    }

    @Override
    public Result filter(LogEvent event) {
        return filter(event.getContextMap().get(key));
    }

    private Result filter(Object val) {
        return this.value.equals(val) ? onMatch : onMismatch;
    }

    @PluginFactory
    public static MDCFilter createFilter(@PluginAttr("key") String key,
                                         @PluginAttr("value") String value,
                                         @PluginAttr("onmatch") String match,
                                         @PluginAttr("onmismatch") String mismatch) {
        Result onMatch = match == null ? null : Result.valueOf(match);
        Result onMismatch = mismatch == null ? null : Result.valueOf(mismatch);
        return new MDCFilter(key, value, onMatch, onMismatch);
    }
}
