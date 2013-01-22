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
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
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
public class LoggerConfig extends AbstractFilterable implements LogEventFactory {

    private static final Logger LOGGER = StatusLogger.getLogger();
    private static final int MAX_RETRIES = 3;
    private static final long WAIT_TIME = 1000;

    private List<AppenderRef> appenderRefs = new ArrayList<AppenderRef>();
    private final Map<String, AppenderControl<?>> appenders = new ConcurrentHashMap<String, AppenderControl<?>>();
    private final String name;
    private LogEventFactory logEventFactory;
    private Level level;
    private boolean additive = true;
    private LoggerConfig parent;
    private final AtomicInteger counter = new AtomicInteger();
    private boolean shutdown = false;
    private final Map<Property, Boolean> properties;
    private final Configuration config;


    /**
     * Default constructor.
     */
    public LoggerConfig() {
        this.logEventFactory = this;
        this.level = Level.ERROR;
        this.name = "";
        this.properties = null;
        this.config = null;
    }

    /**
     * Constructor that sets the name, level and additive values.
     * @param name The Logger name.
     * @param level The Level.
     * @param additive true if the Logger is additive, false otherwise.
     */
    public LoggerConfig(final String name, final Level level, final boolean additive) {
        this.logEventFactory = this;
        this.name = name;
        this.level = level;
        this.additive = additive;
        this.properties = null;
        this.config = null;
    }

    protected LoggerConfig(final String name, final List<AppenderRef> appenders, final Filter filter, final Level level,
                           final boolean additive, final Property[] properties, final Configuration config) {
        super(filter);
        this.logEventFactory = this;
        this.name = name;
        this.appenderRefs = appenders;
        this.level = level;
        this.additive = additive;
        this.config = config;
        if (properties != null && properties.length > 0) {
            this.properties = new HashMap<Property, Boolean>(properties.length);
            for (final Property prop : properties) {
                final boolean interpolate = prop.getValue().contains("${");
                this.properties.put(prop, interpolate);
            }
        } else {
            this.properties = null;
        }
    }

    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    /**
     * Returns the name of the LoggerConfig.
     * @return the name of the LoggerConfig.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the parent of this LoggerConfig.
     * @param parent the parent LoggerConfig.
     */
    public void setParent(final LoggerConfig parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent of this LoggerConfig.
     * @return the LoggerConfig that is the parent of this one.
     */
    public LoggerConfig getParent() {
        return this.parent;
    }

    /**
     * Adds an Appender to the LoggerConfig.
     * @param appender The Appender to add.
     * @param level The Level to use.
     * @param filter A Filter for the Appender reference.
     */
    public void addAppender(final Appender appender, final Level level, final Filter filter) {
        appenders.put(appender.getName(), new AppenderControl(appender, level, filter));
    }

    /**
     * Removes the Appender with the specific name.
     * @param name The name of the Appender.
     */
    public void removeAppender(final String name) {
        final AppenderControl ctl = appenders.remove(name);
        if (ctl != null) {
            cleanupFilter(ctl);
        }
    }

    /**
     * Returns all Appenders as a Map.
     * @return a Map with the Appender name as the key and the Appender as the value.
     */
    public Map<String, Appender<?>> getAppenders() {
        final Map<String, Appender<?>> map = new HashMap<String, Appender<?>>();
        for (final Map.Entry<String, AppenderControl<?>> entry : appenders.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAppender());
        }
        return map;
    }

    /**
     * Removes all Appenders.
     */
    protected void clearAppenders() {
        waitForCompletion();
        final Collection<AppenderControl<?>> controls = appenders.values();
        final Iterator<AppenderControl<?>> iterator = controls.iterator();
        while (iterator.hasNext()) {
            final AppenderControl<?> ctl = iterator.next();
            iterator.remove();
            cleanupFilter(ctl);
        }
    }

    private void cleanupFilter(final AppenderControl ctl) {
        final Filter filter = ctl.getFilter();
        if (filter != null) {
            ctl.removeFilter(filter);
            if (filter instanceof LifeCycle) {
                ((LifeCycle) filter).stop();
            }
        }
    }

    /**
     * Returns the Appender references.
     * @return a List of all the Appender names attached to this LoggerConfig.
     */
    public List<AppenderRef> getAppenderRefs() {
        return appenderRefs;
    }

    /**
     * Sets the logging Level.
     * @param level The logging Level.
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    /**
     * Returns the logging Level.
     * @return the logging Level.
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the LogEventFactory.
     * @return the LogEventFactory.
     */
    public LogEventFactory getLogEventFactory() {
        return logEventFactory;
    }

    /**
     * Sets the LogEventFactory. Usually the LogEventFactory will be this LoggerConfig.
     * @param logEventFactory the LogEventFactory.
     */
    public void setLogEventFactory(final LogEventFactory logEventFactory) {
        this.logEventFactory = logEventFactory;
    }

    /**
     * Returns the valid of the additive flag.
     * @return true if the LoggerConfig is additive, false otherwise.
     */
    public boolean isAdditive() {
        return additive;
    }

    /**
     * Sets the additive setting.
     * @param additive true if thee LoggerConfig should be additive, false otherwise.
     */
    public void setAdditive(final boolean additive) {
        this.additive = additive;
    }

    /**
     * Logs an event.
     * @param loggerName The name of the Logger.
     * @param marker A Marker or null if none is present.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The event Level.
     * @param data The Message.
     * @param t A Throwable or null.
     */
    public void log(final String loggerName, final Marker marker, final String fqcn, final Level level,
                    final Message data, final Throwable t) {
        List<Property> props = null;
        if (properties != null) {
            props = new ArrayList<Property>(properties.size());

            for (final Map.Entry<Property, Boolean> entry : properties.entrySet()) {
                final Property prop = entry.getKey();
                final String value = entry.getValue() ? config.getSubst().replace(prop.getValue()) : prop.getValue();
                props.add(Property.createProperty(prop.getName(), value));
            }
        }
        final LogEvent event = logEventFactory.createEvent(loggerName, marker, fqcn, level, data, props, t);
        log(event);
    }

    /**
     * Waits for all log events to complete before shutting down this loggerConfig.
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
            } catch (final InterruptedException ie) {
                if (++retries > MAX_RETRIES) {
                    break;
                }
            }
        }
    }

    /**
     * Logs an event.
     * @param event The log event.
     */
    public void log(final LogEvent event) {

        counter.incrementAndGet();
        try {
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

    private void callAppenders(final LogEvent event) {
        for (final AppenderControl control : appenders.values()) {
            control.callAppender(event);
        }
    }

    /**
     * Creates a log event.
     * @param loggerName The name of the Logger.
     * @param marker An optional Marker.
     * @param fqcn The fully qualified class name of the caller.
     * @param level The event Level.
     * @param data The Message.
     * @param properties Properties to be added to the log event.
     * @param t An optional Throwable.
     * @return The LogEvent.
     */
    public LogEvent createEvent(final String loggerName, final Marker marker, final String fqcn, final Level level,
                                final Message data, final List<Property> properties, final Throwable t) {
        return new Log4jLogEvent(loggerName, marker, fqcn, level, data, properties, t);
    }

    @Override
    public String toString() {
        return name == null || name.length() == 0 ? "root" : name;
    }

    /**
     * Factory method to create a LoggerConfig.
     * @param additivity True if additive, false otherwise.
     * @param levelName The Level to be associated with the Logger.
     * @param loggerName The name of the Logger.
     * @param refs An array of Appender names.
     * @param properties Properties to pass to the Logger.
     * @param config The Configuration.
     * @param filter A Filter.
     * @return A new LoggerConfig.
     */
    @PluginFactory
    public static LoggerConfig createLogger(@PluginAttr("additivity") final String additivity,
                                            @PluginAttr("level") final String levelName,
                                            @PluginAttr("name") final String loggerName,
                                            @PluginElement("appender-ref") final AppenderRef[] refs,
                                            @PluginElement("properties") final Property[] properties,
                                            @PluginConfiguration final Configuration config,
                                            @PluginElement("filters") final Filter filter) {
        if (loggerName == null) {
            LOGGER.error("Loggers cannot be configured without a name");
            return null;
        }

        final List<AppenderRef> appenderRefs = Arrays.asList(refs);
        Level level;
        try {
            level = Level.toLevel(levelName, Level.ERROR);
        } catch (final Exception ex) {
            LOGGER.error("Invalid Log level specified: {}. Defaulting to Error", levelName);
            level = Level.ERROR;
        }
        final String name = loggerName.equals("root") ? "" : loggerName;
        final boolean additive = additivity == null ? true : Boolean.parseBoolean(additivity);

        return new LoggerConfig(name, appenderRefs, filter, level, additive, properties, config);
    }

    /**
     * The root Logger.
     */
    @Plugin(name = "root", type = "Core", printObject = true)
    public static class RootLogger extends LoggerConfig {

        @PluginFactory
        public static LoggerConfig createLogger(@PluginAttr("additivity") final String additivity,
                                            @PluginAttr("level") final String levelName,
                                            @PluginElement("appender-ref") final AppenderRef[] refs,
                                            @PluginElement("properties") final Property[] properties,
                                            @PluginConfiguration final Configuration config,
                                            @PluginElement("filters") final Filter filter) {
            final List<AppenderRef> appenderRefs = Arrays.asList(refs);
            Level level;
            try {
                level = Level.toLevel(levelName, Level.ERROR);
            } catch (final Exception ex) {
                LOGGER.error("Invalid Log level specified: {}. Defaulting to Error", levelName);
                level = Level.ERROR;
            }
            final boolean additive = additivity == null ? true : Boolean.parseBoolean(additivity);

            return new LoggerConfig(LogManager.ROOT_LOGGER_NAME, appenderRefs, filter, level, additive, properties,
                config);
        }
    }

}
