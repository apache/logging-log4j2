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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Lifecycle;
import org.apache.logging.log4j.core.filter.Filterable;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttr;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.message.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logger object that is created via configuration.
 */
@Plugin(name = "logger", type = "Core", printObject = true)
public class LoggerConfig extends Filterable implements LogEventFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int MAX_RETRIES = 3;
    private static final long WAIT_TIME = 1000;

    private List<AppenderRef> appenderRefs = new ArrayList<AppenderRef>();
    private Map<String, AppenderControl> appenders = new ConcurrentHashMap<String, AppenderControl>();
    private final String name;
    private LogEventFactory logEventFactory;
    private Level level;
    private boolean additive = true;
    private LoggerConfig parent;
    private ConfigurationMonitor monitor = new DefaultConfigurationMonitor();
    private AtomicInteger counter = new AtomicInteger();
    private boolean shutdown = false;


    /**
     * Default constructor.
     */
    public LoggerConfig() {
        this.logEventFactory = this;
        this.level = Level.ERROR;
        this.name = "";
    }

    /**
     * Constructor that sets the name, level and additive values.
     * @param name The Logger name.
     * @param level The Level.
     * @param additive true if the Logger is additive, false otherwise.
     */
    public LoggerConfig(String name, Level level, boolean additive) {
        this.logEventFactory = this;
        this.name = name;
        this.level = level;
        this.additive = additive;
    }

    protected LoggerConfig(String name, List<AppenderRef> appenders, Filter filter, Level level,
                           boolean additive) {
        super(filter);
        this.logEventFactory = this;
        this.name = name;
        this.appenderRefs = appenders;
        this.level = level;
        this.additive = additive;
    }

    @Override
    public Filter getFilter() {
        monitor.checkConfiguration();
        return super.getFilter();
    }

    /**
     * Set the ConfigurationMonitor that will detect configuration changes.
     * @param monitor The ConfigurationMonitor.
     */
    public void setConfigurationMonitor(ConfigurationMonitor monitor) {
        this.monitor = monitor;
    }

    /**
     * Return the name of the LoggerConfig.
     * @return the name of the LoggerConfig.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the parent of this LoggerConfig.
     * @param parent the parent LoggerConfig.
     */
    public void setParent(LoggerConfig parent) {
        this.parent = parent;
    }

    /**
     * Return the parent of this LoggerConfig.
     * @return the LoggerConfig that is the parent of this one.
     */
    public LoggerConfig getParent() {
        return this.parent;
    }

    /**
     * Add an Appender to the LoggerConfig.
     * @param appender The Appender to add.
     * @param level The Level to use.
     * @param filter A Filter for the Appender reference.
     */
    public void addAppender(Appender appender, Level level, Filter filter) {
        appenders.put(appender.getName(), new AppenderControl(appender, level, filter));
    }

    /**
     * Remove the Appender with the specific name.
     * @param name The name of the Appender.
     */
    public void removeAppender(String name) {
        AppenderControl ctl = appenders.remove(name);
        if (ctl != null) {
            cleanupFilter(ctl);
        }
    }

    /**
     * Return all Appenders as a Map.
     * @return a Map with the Appender name as the key and the Appender as the value.
     */
    public Map<String, Appender> getAppenders() {
        Map<String, Appender> map = new HashMap<String, Appender>();
        for (Map.Entry<String, AppenderControl> entry : appenders.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAppender());
        }
        return map;
    }

    /**
     * Remove all Appenders.
     */
    protected void clearAppenders() {
        waitForCompletion();
        Collection<AppenderControl> controls = appenders.values();
        Iterator<AppenderControl> iterator = controls.iterator();
        while (iterator.hasNext()) {
            AppenderControl ctl = iterator.next();
            iterator.remove();
            cleanupFilter(ctl);
        }
    }

    private void cleanupFilter(AppenderControl ctl) {
        Filter filter = ctl.getFilter();
        if (filter != null) {
            ctl.removeFilter(filter);
            if (filter instanceof Lifecycle) {
                ((Lifecycle) filter).stop();
            }
        }
    }

    /**
     * Return the Appender references.
     * @return a List of all the Appender names attached to this LoggerConfig.
     */
    public List<AppenderRef> getAppenderRefs() {
        return appenderRefs;
    }

    /**
     * Set the logging Level.
     * @param level The logging Level.
     */
    public void setLevel(Level level) {
        this.level = level;
    }

    /**
     * Return the logging Level.
     * @return the logging Level.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Return the LogEventFactory.
     * @return the LogEventFactory.
     */
    public LogEventFactory getLogEventFactory() {
        return logEventFactory;
    }

    /**
     * Set the LogEventFactory. Usually the LogEventFactory will be this LoggerConfig.
     * @param logEventFactory the LogEventFactory.
     */
    public void setLogEventFactory(LogEventFactory logEventFactory) {
        this.logEventFactory = logEventFactory;
    }

    /**
     * Return the valid of the additive flag.
     * @return true if the LoggerConfig is additive, false otherwise.
     */
    public boolean isAdditive() {
        return additive;
    }

    /**
     * Set the additive setting.
     * @param additive true if thee LoggerConfig should be additive, false otherwise.
     */
    public void setAdditive(boolean additive) {
        this.additive = additive;
    }

    /**
     * Log an event.
     * @param loggerName The name of the Logger.
     * @param marker A Marker or null if none is present.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The event Level.
     * @param data The Message.
     * @param t A Throwable or null.
     */
    public void log(String loggerName, Marker marker, String fqcn, Level level, Message data, Throwable t) {
        LogEvent event = logEventFactory.createEvent(loggerName, marker, fqcn, level, data, t);
        log(event);
    }

    /**
     * Wait for all log events to complete before shutting down this loggerConfig.
     */
    private synchronized void waitForCompletion() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        int retries = 0;
        while (counter.get() > 0) {
            try {
                wait(WAIT_TIME * (retries + 1));
            } catch (InterruptedException ie) {
                if (++retries > MAX_RETRIES) {
                    break;
                }
            }
        }
    }

    /**
     * Logs an event.
     * @param event Yhe log event.
     */
    public void log(LogEvent event) {

        counter.incrementAndGet();
        try {
            monitor.checkConfiguration();
            if (isFiltered(event)) {
                return;
            }

            callAppenders(event);

            if (additive && parent != null) {
                parent.log(event);
            }
        } finally {
            if (counter.decrementAndGet() == 0) {
                synchronized (this) {
                    if (shutdown) {
                        notifyAll();
                    }
                }

            }
        }
    }

    private void callAppenders(LogEvent event) {
        for (AppenderControl control : appenders.values()) {
            control.callAppender(event);
        }
    }

    /**
     * Create a log event.
     * @param loggerName The name of the Logger.
     * @param marker An optional Marker.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The event Level.
     * @param data The Message.
     * @param t An optional Throwable.
     * @return The LogEvent.
     */
    public LogEvent createEvent(String loggerName, Marker marker, String fqcn, Level level, Message data,
                                Throwable t) {
        return new Log4jLogEvent(loggerName, marker, fqcn, level, data, t);
    }

    @Override
    public String toString() {
        return name == null || name.length() == 0 ? "root" : name;
    }

    /**
     * Factory method to create a LoggerConfig.
     * @param additivity True if additive, false otherwise.
     * @param loggerLevel The Level to be associated with the Logger.
     * @param loggerName The name of the Logger.
     * @param refs An array of Appender names.
     * @param filter A Filter.
     * @return A new LoggerConfig.
     */
    @PluginFactory
    public static LoggerConfig createLogger(@PluginAttr("additivity") String additivity,
                                            @PluginAttr("level") String loggerLevel,
                                            @PluginAttr("name") String loggerName,
                                            @PluginElement("appender-ref") AppenderRef[] refs,
                                            @PluginElement("filters") Filter filter) {
        if (loggerName == null) {
            LOGGER.error("Loggers cannot be configured without a name");
            return null;
        }

        List<AppenderRef> appenderRefs = Arrays.asList(refs);
        Level level;
        try {
            level = loggerLevel == null ? Level.ERROR : Level.valueOf(loggerLevel.toUpperCase());
        } catch (Exception ex) {
            LOGGER.error("Invalid Log level specified: {}. Defaulting to Error", loggerLevel);
            level = Level.ERROR;
        }
        String name = loggerName.equals("root") ? "" : loggerName;
        boolean additive = additivity == null ? true : Boolean.parseBoolean(additivity);

        return new LoggerConfig(name, appenderRefs, filter, level, additive);
    }

    /**
     * The root Logger.
     */
    @Plugin(name = "root", type = "Core", printObject = true)
    public static class RootLogger extends LoggerConfig {

        @PluginFactory
        public static LoggerConfig createLogger(@PluginAttr("additivity") String additivity,
                                            @PluginAttr("level") String loggerLevel,
                                            @PluginElement("appender-ref") AppenderRef[] refs,
                                            @PluginElement("filters") Filter filter) {
            List<AppenderRef> appenderRefs = Arrays.asList(refs);
            Level level;
            try {
                level = loggerLevel == null ? Level.ERROR : Level.valueOf(loggerLevel.toUpperCase());
            } catch (Exception ex) {
                LOGGER.error("Invalid Log level specified: {}. Defaulting to Error", loggerLevel);
                level = Level.ERROR;
            }
            boolean additive = additivity == null ? true : Boolean.parseBoolean(additivity);

            return new LoggerConfig(LogManager.ROOT_LOGGER_NAME, appenderRefs, filter, level, additive);
        }
    }

}
