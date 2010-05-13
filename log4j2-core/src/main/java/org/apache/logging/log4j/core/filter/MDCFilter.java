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
import org.apache.logging.log4j.MDC;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

import java.util.Map;

/**
 *
 */
@Plugin(name="MDC", type="Core")
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
        return filter(MDC.get(key));
    }

    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter(MDC.get(key));
    }

    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter(MDC.get(key));
    }

    @Override
    public Result filter(LogEvent event) {
        return filter(event.getContextMap().get(key));
    }

    private Result filter(Object val) {
        return this.value.equals(val) ? onMatch : onMismatch;
    }

    @PluginFactory
    public static MDCFilter createFilter(Node node) {
        String key = null;
        String value = null;
        Result onMatch = null;
        Result onMismatch = null;
        for (Map.Entry<String, String> entry : node.getAttributes().entrySet()) {
            String name = entry.getKey().toLowerCase();
            if (name.equals(KEY)) {
                key = entry.getValue();
            } else if (name.equals(VALUE)) {
                value = entry.getValue();
            } else if (name.equals(ON_MATCH)) {
                onMatch = Result.valueOf(entry.getValue());
            } else if (name.equals(ON_MISMATCH)) {
                onMismatch = Result.valueOf(entry.getValue());
            }
        }
        return new MDCFilter(key, value, onMatch, onMismatch);
    }
}
