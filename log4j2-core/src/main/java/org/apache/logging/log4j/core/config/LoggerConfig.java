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
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Log4jLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LogEventFactory;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.internal.StatusLogger;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 */
@Plugin(name="logger",type="Core")
public class LoggerConfig implements LogEventFactory {

    private List<String> appenderRefs = new ArrayList<String>();
    private Map<String, AppenderControl> appenders = new ConcurrentHashMap<String, AppenderControl>();

    private List<Filter> filters = new CopyOnWriteArrayList<Filter>();
    private boolean hasFilters = false;

    private final String name;

    private LogEventFactory logEventFactory;

    private Level level;

    private boolean additive = true;

    private LoggerConfig parent;

    private static Logger logger = StatusLogger.getLogger();

    public LoggerConfig() {
        this.logEventFactory = this;
        this.level = Level.ERROR;
        this.name = "";
    }

    public LoggerConfig(String name, Level level, boolean additive) {
        this.logEventFactory = this;
        this.name = name;
        this.level = level;
        this.additive = additive;
    }

    protected LoggerConfig(String name, List<String> appenders, Filter[] filters, Level level,
                           boolean additive) {
        this.logEventFactory = this;
        this.name = name;
        this.appenderRefs = appenders;
        if (filters != null && filters.length > 0) {
            this.filters = new CopyOnWriteArrayList<Filter>(filters);
            hasFilters = true;
        }
        this.level = level;
        this.additive = additive;
    }

    public String getName() {
        return name;
    }

    public void setParent(LoggerConfig parent) {
        this.parent = parent;
    }

    public LoggerConfig getParent() {
        return this.parent;
    }

    public void addAppender(Appender appender) {
        appenders.put(appender.getName(), new AppenderControl(appender));
    }

    public void removeAppender(String name) {
        appenders.remove(name);
    }

    public Map<String, Appender> getAppenders() {
        Map<String, Appender> map = new HashMap<String, Appender>();
        for (Map.Entry<String, AppenderControl> entry : appenders.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAppender());
        }
        return map;
    }

    protected void clearAppenders() {
        appenders.clear();
    }

    public List<String> getAppenderRefs() {
        return appenderRefs;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public LogEventFactory getLogEventFactory() {
        return logEventFactory;
    }

    public void setLogEventFactory(LogEventFactory logEventFactory) {
        this.logEventFactory = logEventFactory;
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
        hasFilters = filters.size() > 0;
    }

    public void removeFilter(Filter filter) {
        filters.remove(filter);
        hasFilters = filters.size() > 0;
    }

    public List<Filter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    public boolean isAdditive() {
        return additive;
    }

    public void setAdditive(boolean additive) {
        this.additive = additive;
    }

    public void log(String loggerName, Marker marker, String fqcn, Level level, Message data, Throwable t) {
        LogEvent event = logEventFactory.createEvent(loggerName, marker, fqcn, level, data, t);
        log(event);
    }

    private void log(LogEvent event) {
        if (hasFilters) {
            for (Filter filter : filters) {
                if (filter.filter(event) == Filter.Result.DENY) {
                    return;
                }
            }
        }

        callAppenders(event);

        if (additive && parent != null) {
            parent.log(event);
        }
    }

    private void callAppenders(LogEvent event) {
        for (AppenderControl control: appenders.values()) {
            control.callAppender(event);
        }
    }

    public LogEvent createEvent(String loggerName, Marker marker, String fqcn, Level level, Message data,
                                Throwable t) {
        return new Log4jLogEvent(loggerName, marker, fqcn, level, data, t);
    }

    @PluginFactory
    public static LoggerConfig createLogger(Node node) {
        Map<String, String> map = node.getAttributes();
        List<String> appenderRefs = new ArrayList<String>();
        Filter[] filters = null;
        boolean additive = true;
        Level level = Level.ERROR;
        String name = null;

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase("additivity"))  {
                additive = Boolean.parseBoolean(entry.getValue());
            }
            if (key.equalsIgnoreCase("level")) {
                Level l = Level.valueOf(entry.getValue().toUpperCase());
                if (l != null) {
                    level = l;
                }
            }
            if (key.equalsIgnoreCase("name")) {
                name = entry.getValue();
            }
        }

        if (node.getName().equals("root")) {
            name = "";
        }

        if (name == null) {
            logger.error("Loggers cannot be configured without a name");
            return null;
        }

        for (Node child : node.getChildren()) {
            Object obj = child.getObject();
            if (obj != null) {
                if (obj instanceof String) {
                    appenderRefs.add((String) obj);
                } else if (obj instanceof Filter[]) {
                    filters = (Filter[]) obj;
                }
            }
        }

        return new LoggerConfig(name, appenderRefs, filters, level, additive);
    }
    /*
    @Plugin("appender-refs")
    public static class AppenderRefs {

        @PluginFactory
        public static String[] createAppenderRefs(Node node) {
            String[] refs = new String[node.getChildren().size()];
            int i = 0;
            for (Node child : node.getChildren()) {
                refs[i++] = (String) child.getObject();
            }
            return refs;
        }
    } */

    @Plugin(name="appender-ref",type="Core")
    public static class AppenderRef {

        @PluginFactory
        public static String createAppenderRef(Node node) {
            Map<String, String> attrs = node.getAttributes();
            for (Map.Entry<String, String> attr : attrs.entrySet()) {
                if (attr.getKey().equalsIgnoreCase("ref")) {
                    return attr.getValue();
                }
            }
            return null;
        }
    }
}
