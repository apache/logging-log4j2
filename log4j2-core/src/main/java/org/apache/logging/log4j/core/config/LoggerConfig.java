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
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.filter.Filters;
import org.apache.logging.log4j.internal.StatusLogger;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
@Plugin(name="logger",type="Core", printObject=true)
public class LoggerConfig extends Filterable implements LogEventFactory {

    private List<String> appenderRefs = new ArrayList<String>();
    private Map<String, AppenderControl> appenders = new ConcurrentHashMap<String, AppenderControl>();

    private final String name;

    private LogEventFactory logEventFactory;

    private Level level;

    private boolean additive = true;

    private LoggerConfig parent;

    private static Logger logger = StatusLogger.getLogger();

    private ConfigurationMonitor monitor = new DefaultConfigurationMonitor();

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

    protected LoggerConfig(String name, List<String> appenders, Filters filters, Level level,
                           boolean additive) {
        this.logEventFactory = this;
        this.name = name;
        this.appenderRefs = appenders;
        setFilters(filters);
        this.level = level;
        this.additive = additive;
    }

    public void setConfigurationMonitor(ConfigurationMonitor monitor) {
        this.monitor = monitor;
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

    public void log(LogEvent event) {
        monitor.checkConfiguration();
        if (isFiltered(event)) {
            return;
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

    public String toString() {
        return name == null || name.length() == 0 ? "root" : name;
    }

    @PluginFactory
    public static LoggerConfig createLogger(@PluginAttr("additivity") String additivity,
                                            @PluginAttr("level") String loggerLevel,
                                            @PluginAttr("name") String loggerName,
                                            @PluginElement("appender-ref") String[] refs,
                                            @PluginElement("filters") Filters filters) {
        if (loggerName == null) {
            logger.error("Loggers cannot be configured without a name");
            return null;
        }

        List<String> appenderRefs = Arrays.asList(refs);
        Level level = loggerLevel == null ? Level.ERROR : Level.valueOf(loggerLevel.toUpperCase());
        String name = loggerName.equals("root") ? "" : loggerName;
        boolean additive = additivity == null ? true : Boolean.parseBoolean(additivity);

        return new LoggerConfig(name, appenderRefs, filters, level, additive);
    }

    @Plugin(name = "root", type = "Core", printObject=true)
    public static class RootLogger extends LoggerConfig {

        @PluginFactory
        public static LoggerConfig createLogger(@PluginAttr("additivity") String additivity,
                                            @PluginAttr("level") String loggerLevel,
                                            @PluginElement("appender-ref") String[] refs,
                                            @PluginElement("filters") Filters filters) {
            List<String> appenderRefs = Arrays.asList(refs);
            Level level = loggerLevel == null ? Level.ERROR : Level.valueOf(loggerLevel.toUpperCase());
            boolean additive = additivity == null ? true : Boolean.parseBoolean(additivity);

            return new LoggerConfig("", appenderRefs, filters, level, additive);
        }
    }

}
