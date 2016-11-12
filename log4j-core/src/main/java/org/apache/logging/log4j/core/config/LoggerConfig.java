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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.DefaultLogEventFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.impl.ReusableLogEventFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.util.Booleans;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

/**
 * Logger object that is created via configuration.
 */
@Plugin(name = "logger", category = Node.CATEGORY, printObject = true)
public class LoggerConfig extends AbstractFilterable {

    public static final String ROOT = "root";
    private static LogEventFactory LOG_EVENT_FACTORY = null;

    private List<AppenderRef> appenderRefs = new ArrayList<>();
    private final AppenderControlArraySet appenders = new AppenderControlArraySet();
    private final String name;
    private LogEventFactory logEventFactory;
    private Level level;
    private boolean additive = true;
    private boolean includeLocation = true;
    private LoggerConfig parent;
    private Map<Property, Boolean> propertiesMap;
    private final List<Property> properties;
    private final boolean propertiesRequireLookup;
    private final Configuration config;
    private final ReliabilityStrategy reliabilityStrategy;

    static {
        final String factory = PropertiesUtil.getProperties().getStringProperty(Constants.LOG4J_LOG_EVENT_FACTORY);
        if (factory != null) {
            try {
                final Class<?> clazz = LoaderUtil.loadClass(factory);
                if (clazz != null && LogEventFactory.class.isAssignableFrom(clazz)) {
                    LOG_EVENT_FACTORY = (LogEventFactory) clazz.newInstance();
                }
            } catch (final Exception ex) {
                LOGGER.error("Unable to create LogEventFactory {}", factory, ex);
            }
        }
        if (LOG_EVENT_FACTORY == null) {
            LOG_EVENT_FACTORY = Constants.ENABLE_THREADLOCALS
                    ? new ReusableLogEventFactory()
                    : new DefaultLogEventFactory();
        }
    }

    /**
     * Default constructor.
     */
    public LoggerConfig() {
        this.logEventFactory = LOG_EVENT_FACTORY;
        this.level = Level.ERROR;
        this.name = Strings.EMPTY;
        this.properties = null;
        this.propertiesRequireLookup = false;
        this.config = null;
        this.reliabilityStrategy = new DefaultReliabilityStrategy(this);
    }

    /**
     * Constructor that sets the name, level and additive values.
     *
     * @param name The Logger name.
     * @param level The Level.
     * @param additive true if the Logger is additive, false otherwise.
     */
    public LoggerConfig(final String name, final Level level, final boolean additive) {
        this.logEventFactory = LOG_EVENT_FACTORY;
        this.name = name;
        this.level = level;
        this.additive = additive;
        this.properties = null;
        this.propertiesRequireLookup = false;
        this.config = null;
        this.reliabilityStrategy = new DefaultReliabilityStrategy(this);
    }

    protected LoggerConfig(final String name, final List<AppenderRef> appenders, final Filter filter,
            final Level level, final boolean additive, final Property[] properties, final Configuration config,
            final boolean includeLocation) {
        super(filter);
        this.logEventFactory = LOG_EVENT_FACTORY;
        this.name = name;
        this.appenderRefs = appenders;
        this.level = level;
        this.additive = additive;
        this.includeLocation = includeLocation;
        this.config = config;
        if (properties != null && properties.length > 0) {
            this.properties = Collections.unmodifiableList(Arrays.asList(Arrays.copyOf(
                    properties, properties.length)));
        } else {
            this.properties = null;
        }
        this.propertiesRequireLookup = containsPropertyRequiringLookup(properties);
        this.reliabilityStrategy = config.getReliabilityStrategy(this);
    }

    private static boolean containsPropertyRequiringLookup(final Property[] properties) {
        if (properties == null) {
            return false;
        }
        for (int i = 0; i < properties.length; i++) {
            if (properties[i].isValueNeedsLookup()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Filter getFilter() {
        return super.getFilter();
    }

    /**
     * Returns the name of the LoggerConfig.
     *
     * @return the name of the LoggerConfig.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the parent of this LoggerConfig.
     *
     * @param parent the parent LoggerConfig.
     */
    public void setParent(final LoggerConfig parent) {
        this.parent = parent;
    }

    /**
     * Returns the parent of this LoggerConfig.
     *
     * @return the LoggerConfig that is the parent of this one.
     */
    public LoggerConfig getParent() {
        return this.parent;
    }

    /**
     * Adds an Appender to the LoggerConfig.
     *
     * @param appender The Appender to add.
     * @param level The Level to use.
     * @param filter A Filter for the Appender reference.
     */
    public void addAppender(final Appender appender, final Level level, final Filter filter) {
        appenders.add(new AppenderControl(appender, level, filter));
    }

    /**
     * Removes the Appender with the specific name.
     *
     * @param name The name of the Appender.
     */
    public void removeAppender(final String name) {
        AppenderControl removed = null;
        while ((removed = appenders.remove(name)) != null) {
            cleanupFilter(removed);
        }
    }

    /**
     * Returns all Appenders as a Map.
     *
     * @return a Map with the Appender name as the key and the Appender as the value.
     */
    public Map<String, Appender> getAppenders() {
        return appenders.asMap();
    }

    /**
     * Removes all Appenders.
     */
    protected void clearAppenders() {
        do {
            final AppenderControl[] original = appenders.clear();
            for (final AppenderControl ctl : original) {
                cleanupFilter(ctl);
            }
        } while (!appenders.isEmpty());
    }

    private void cleanupFilter(final AppenderControl ctl) {
        final Filter filter = ctl.getFilter();
        if (filter != null) {
            ctl.removeFilter(filter);
            filter.stop();
        }
    }

    /**
     * Returns the Appender references.
     *
     * @return a List of all the Appender names attached to this LoggerConfig.
     */
    public List<AppenderRef> getAppenderRefs() {
        return appenderRefs;
    }

    /**
     * Sets the logging Level.
     *
     * @param level The logging Level.
     */
    public void setLevel(final Level level) {
        this.level = level;
    }

    /**
     * Returns the logging Level.
     *
     * @return the logging Level.
     */
    public Level getLevel() {
        return level == null ? parent.getLevel() : level;
    }

    /**
     * Returns the LogEventFactory.
     *
     * @return the LogEventFactory.
     */
    public LogEventFactory getLogEventFactory() {
        return logEventFactory;
    }

    /**
     * Sets the LogEventFactory. Usually the LogEventFactory will be this LoggerConfig.
     *
     * @param logEventFactory the LogEventFactory.
     */
    public void setLogEventFactory(final LogEventFactory logEventFactory) {
        this.logEventFactory = logEventFactory;
    }

    /**
     * Returns the valid of the additive flag.
     *
     * @return true if the LoggerConfig is additive, false otherwise.
     */
    public boolean isAdditive() {
        return additive;
    }

    /**
     * Sets the additive setting.
     *
     * @param additive true if the LoggerConfig should be additive, false otherwise.
     */
    public void setAdditive(final boolean additive) {
        this.additive = additive;
    }

    /**
     * Returns the value of logger configuration attribute {@code includeLocation}, or, if no such attribute was
     * configured, {@code true} if logging is synchronous or {@code false} if logging is asynchronous.
     *
     * @return whether location should be passed downstream
     */
    public boolean isIncludeLocation() {
        return includeLocation;
    }

    /**
     * Returns an unmodifiable map with the configuration properties, or {@code null} if this {@code LoggerConfig} does
     * not have any configuration properties.
     * <p>
     * For each {@code Property} key in the map, the value is {@code true} if the property value has a variable that
     * needs to be substituted.
     *
     * @return an unmodifiable map with the configuration properties, or {@code null}
     * @see Configuration#getStrSubstitutor()
     * @see StrSubstitutor
     * @deprecated use {@link #getPropertyList()} instead
     */
    // LOG4J2-157
    @Deprecated
    public Map<Property, Boolean> getProperties() {
        if (properties == null) {
            return null;
        }
        if (propertiesMap == null) { // lazily initialize: only used by user custom code, not by Log4j any more
            final Map<Property, Boolean> result = new HashMap<>(properties.size() * 2);
            for (int i = 0; i < properties.size(); i++) {
                result.put(properties.get(i), Boolean.valueOf(properties.get(i).isValueNeedsLookup()));
            }
            propertiesMap = Collections.unmodifiableMap(result);
        }
        return propertiesMap;
    }

    /**
     * Returns an unmodifiable list with the configuration properties, or {@code null} if this {@code LoggerConfig} does
     * not have any configuration properties.
     * <p>
     * Each {@code Property} in the list has an attribute {@link Property#isValueNeedsLookup() valueNeedsLookup} that
     * is {@code true} if the property value has a variable that needs to be substituted.
     *
     * @return an unmodifiable list with the configuration properties, or {@code null}
     * @see Configuration#getStrSubstitutor()
     * @see StrSubstitutor
     * @since 2.7
     */
    public List<Property> getPropertyList() {
        return properties;
    }

    public boolean isPropertiesRequireLookup() {
        return propertiesRequireLookup;
    }

    /**
     * Logs an event.
     *
     * @param loggerName The name of the Logger.
     * @param fqcn The fully qualified class name of the caller.
     * @param marker A Marker or null if none is present.
     * @param level The event Level.
     * @param data The Message.
     * @param t A Throwable or null.
     */
    @PerformanceSensitive("allocation")
    public void log(final String loggerName, final String fqcn, final Marker marker, final Level level,
            final Message data, final Throwable t) {
        List<Property> props = null;
        if (!propertiesRequireLookup) {
            props = properties;
        } else {
            if (properties != null) {
                props = new ArrayList<>(properties.size());
                final LogEvent event = Log4jLogEvent.newBuilder()
                        .setMessage(data)
                        .setMarker(marker)
                        .setLevel(level)
                        .setLoggerName(loggerName)
                        .setLoggerFqcn(fqcn)
                        .setThrown(t)
                        .build();
                for (int i = 0; i < properties.size(); i++) {
                    final Property prop = properties.get(i);
                    final String value = prop.isValueNeedsLookup() // since LOG4J2-1575
                            ? config.getStrSubstitutor().replace(event, prop.getValue()) //
                            : prop.getValue();
                    props.add(Property.createProperty(prop.getName(), value));
                }
            }
        }
        final LogEvent logEvent = logEventFactory.createEvent(loggerName, marker, fqcn, level, data, props, t);
        try {
            log(logEvent);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            ReusableLogEventFactory.release(logEvent);
        }
    }

    /**
     * Logs an event.
     *
     * @param event The log event.
     */
    public void log(final LogEvent event) {
        if (!isFiltered(event)) {
            processLogEvent(event);
        }
    }

    /**
     * Returns the object responsible for ensuring log events are delivered to a working appender, even during or after
     * a reconfiguration.
     *
     * @return the object responsible for delivery of log events to the appender
     */
    public ReliabilityStrategy getReliabilityStrategy() {
        return reliabilityStrategy;
    }

    private void processLogEvent(final LogEvent event) {
        event.setIncludeLocation(isIncludeLocation());
        callAppenders(event);
        logParent(event);
    }

    private void logParent(final LogEvent event) {
        if (additive && parent != null) {
            parent.log(event);
        }
    }

    @PerformanceSensitive("allocation")
    protected void callAppenders(final LogEvent event) {
        final AppenderControl[] controls = appenders.get();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < controls.length; i++) {
            controls[i].callAppender(event);
        }
    }

    @Override
    public String toString() {
        return Strings.isEmpty(name) ? ROOT : name;
    }

    /**
     * Factory method to create a LoggerConfig.
     *
     * @param additivity True if additive, false otherwise.
     * @param level The Level to be associated with the Logger.
     * @param loggerName The name of the Logger.
     * @param includeLocation whether location should be passed downstream
     * @param refs An array of Appender names.
     * @param properties Properties to pass to the Logger.
     * @param config The Configuration.
     * @param filter A Filter.
     * @return A new LoggerConfig.
     * @deprecated Deprecated in 2.7; use {@link #createLogger(boolean, Level, String, String, AppenderRef[], Property[], Configuration, Filter)}
     */
    @Deprecated
    public static LoggerConfig createLogger(final String additivity,
            // @formatter:off
            final Level level,
            @PluginAttribute("name") final String loggerName,
            final String includeLocation,
            final AppenderRef[] refs,
            final Property[] properties,
            @PluginConfiguration final Configuration config,
            final Filter filter) {
            // @formatter:on
        if (loggerName == null) {
            LOGGER.error("Loggers cannot be configured without a name");
            return null;
        }

        final List<AppenderRef> appenderRefs = Arrays.asList(refs);
        final String name = loggerName.equals(ROOT) ? Strings.EMPTY : loggerName;
        final boolean additive = Booleans.parseBoolean(additivity, true);

        return new LoggerConfig(name, appenderRefs, filter, level, additive, properties, config,
                includeLocation(includeLocation));
    }

    /**
     * Factory method to create a LoggerConfig.
     *
     * @param additivity true if additive, false otherwise.
     * @param level The Level to be associated with the Logger.
     * @param loggerName The name of the Logger.
     * @param includeLocation whether location should be passed downstream
     * @param refs An array of Appender names.
     * @param properties Properties to pass to the Logger.
     * @param config The Configuration.
     * @param filter A Filter.
     * @return A new LoggerConfig.
     * @since 2.6
     */
    @PluginFactory
    public static LoggerConfig createLogger(
         // @formatter:off
        @PluginAttribute(value = "additivity", defaultBoolean = true) final boolean additivity,
        @PluginAttribute("level") final Level level,
        @Required(message = "Loggers cannot be configured without a name") @PluginAttribute("name") final String loggerName,
        @PluginAttribute("includeLocation") final String includeLocation,
        @PluginElement("AppenderRef") final AppenderRef[] refs,
        @PluginElement("Properties") final Property[] properties,
        @PluginConfiguration final Configuration config,
        @PluginElement("Filter") final Filter filter
        // @formatter:on
    ) {
        final String name = loggerName.equals(ROOT) ? Strings.EMPTY : loggerName;
        return new LoggerConfig(name, Arrays.asList(refs), filter, level, additivity, properties, config,
            includeLocation(includeLocation));
    }

    // Note: for asynchronous loggers, includeLocation default is FALSE,
    // for synchronous loggers, includeLocation default is TRUE.
    protected static boolean includeLocation(final String includeLocationConfigValue) {
        if (includeLocationConfigValue == null) {
            final boolean sync = !AsyncLoggerContextSelector.isSelected();
            return sync;
        }
        return Boolean.parseBoolean(includeLocationConfigValue);
    }

    /**
     * The root Logger.
     */
    @Plugin(name = ROOT, category = Core.CATEGORY_NAME, printObject = true)
    public static class RootLogger extends LoggerConfig {

        @PluginFactory
        public static LoggerConfig createLogger(
                // @formatter:off
                @PluginAttribute("additivity") final String additivity,
                @PluginAttribute("level") final Level level,
                @PluginAttribute("includeLocation") final String includeLocation,
                @PluginElement("AppenderRef") final AppenderRef[] refs,
                @PluginElement("Properties") final Property[] properties,
                @PluginConfiguration final Configuration config,
                @PluginElement("Filter") final Filter filter) {
                // @formatter:on
            final List<AppenderRef> appenderRefs = Arrays.asList(refs);
            final Level actualLevel = level == null ? Level.ERROR : level;
            final boolean additive = Booleans.parseBoolean(additivity, true);

            return new LoggerConfig(LogManager.ROOT_LOGGER_NAME, appenderRefs, filter, actualLevel, additive,
                    properties, config, includeLocation(includeLocation));
        }
    }

}
