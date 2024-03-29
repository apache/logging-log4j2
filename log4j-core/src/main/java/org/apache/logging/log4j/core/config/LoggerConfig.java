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
package org.apache.logging.log4j.core.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.LogEventFactory;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StackLocatorUtil;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;

/**
 * Logger object that is created via configuration.
 */
@Configurable(printObject = true)
@Plugin("logger")
public class LoggerConfig extends AbstractFilterable {

    public static final String ROOT = "root";
    static Key<LoggerConfig> KEY = Key.forClass(LoggerConfig.class).withQualifierType(PluginElement.class);

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

    @PluginFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    /**
     * Builds LoggerConfig instances.
     *
     * @param <B>
     *            The type to build
     */
    public static class Builder<B extends Builder<B>>
            implements org.apache.logging.log4j.plugins.util.Builder<LoggerConfig> {

        @PluginBuilderAttribute
        private boolean additivity = true;

        private Level level;
        private String levelAndRefs;
        private String loggerName;
        private @Nullable Boolean includeLocation;
        private AppenderRef[] refs;
        private Property[] properties;
        private Configuration config;
        private Filter filter;

        public boolean isAdditivity() {
            return additivity;
        }

        public B setAdditivity(final boolean additivity) {
            this.additivity = additivity;
            return asBuilder();
        }

        public Level getLevel() {
            return level;
        }

        public B setLevel(@PluginAttribute final Level level) {
            this.level = level;
            return asBuilder();
        }

        public String getLevelAndRefs() {
            return levelAndRefs;
        }

        public B setLevelAndRefs(@PluginAttribute final String levelAndRefs) {
            this.levelAndRefs = levelAndRefs;
            return asBuilder();
        }

        public String getLoggerName() {
            return loggerName;
        }

        public B setLoggerName(
                @Required(message = "Loggers cannot be configured without a name") @PluginAttribute final String name) {
            this.loggerName = name;
            return asBuilder();
        }

        public @Nullable Boolean getIncludeLocation() {
            return includeLocation;
        }

        public B setIncludeLocation(final @Nullable Boolean includeLocation) {
            this.includeLocation = includeLocation;
            return asBuilder();
        }

        // TODO: remove this once https://github.com/apache/logging-log4j2/pull/2329 is solved.
        @Deprecated
        public B setIncludeLocation(final @PluginAttribute @Nullable String includeLocation) {
            return setIncludeLocation(includeLocation != null ? Boolean.valueOf(includeLocation) : null);
        }

        public AppenderRef[] getRefs() {
            return refs;
        }

        public B setRefs(@PluginElement final AppenderRef... refs) {
            this.refs = refs;
            return asBuilder();
        }

        public Property[] getProperties() {
            return properties;
        }

        public B setProperties(@PluginElement final Property... properties) {
            this.properties = properties;
            return asBuilder();
        }

        public Configuration getConfig() {
            return config;
        }

        public B setConfig(@PluginConfiguration final Configuration config) {
            this.config = config;
            return asBuilder();
        }

        public Filter getFilter() {
            return filter;
        }

        public B setFilter(@PluginElement final Filter filter) {
            this.filter = filter;
            return asBuilder();
        }

        @Override
        public LoggerConfig build() {
            final String name = loggerName.equals(ROOT) ? Strings.EMPTY : loggerName;
            final LevelAndRefs container = LoggerConfig.getLevelAndRefs(level, refs, levelAndRefs, config);
            final boolean useLocation = includeLocation(getIncludeLocation(), config);
            return new LoggerConfig(
                    name, container.refs, filter, container.level, isAdditivity(), properties, config, useLocation);
        }

        @SuppressWarnings("unchecked")
        public B asBuilder() {
            return (B) this;
        }
    }

    /**
     * Constructor that sets the name, level and additive values.
     *
     * @param name The Logger name.
     * @param level The Level.
     * @param additive true if the Logger is additive, false otherwise.
     */
    public LoggerConfig(final String name, final Level level, final boolean additive, final Configuration config) {
        this(name, Collections.emptyList(), null, level, additive, null, config, includeLocation(null, config));
    }

    protected LoggerConfig(
            final String name,
            final List<AppenderRef> appenders,
            final Filter filter,
            final Level level,
            final boolean additive,
            final Property[] properties,
            final Configuration config,
            final boolean includeLocation) {
        super(filter, null);
        this.logEventFactory = config.getLogEventFactory();
        this.name = name;
        this.appenderRefs = appenders;
        this.level = level;
        this.additive = additive;
        this.includeLocation = includeLocation;
        this.config = config;
        if (properties != null && properties.length > 0) {
            // don't use List.of() here as that will create temporary iterators downstream
            this.properties = Arrays.asList(properties.clone());
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
        return level == null ? parent == null ? Level.ERROR : parent.getLevel() : level;
    }

    /**
     * Allows callers to determine the Level assigned to this LoggerConfig.
     * @return the Level associated with this LoggerConfig or null if none is set.
     */
    public Level getExplicitLevel() {
        return level;
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
    public void log(
            final String loggerName,
            final String fqcn,
            final Marker marker,
            final Level level,
            final Message data,
            final Throwable t) {
        final List<Property> props = getProperties(loggerName, fqcn, marker, level, data, t);
        final LogEvent logEvent =
                logEventFactory.createEvent(loggerName, marker, fqcn, location(fqcn), level, data, props, t);
        try {
            log(logEvent, null);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            logEventFactory.recycle(logEvent);
        }
    }

    private StackTraceElement location(final String fqcn) {
        return requiresLocation() ? StackLocatorUtil.calcLocation(fqcn) : null;
    }

    /**
     * Logs an event.
     *
     * @param loggerName The name of the Logger.
     * @param fqcn The fully qualified class name of the caller.
     * @param location the location of the caller.
     * @param marker A Marker or null if none is present.
     * @param level The event Level.
     * @param data The Message.
     * @param t A Throwable or null.
     */
    @PerformanceSensitive("allocation")
    public void log(
            final String loggerName,
            final String fqcn,
            final StackTraceElement location,
            final Marker marker,
            final Level level,
            final Message data,
            final Throwable t) {
        final List<Property> props = getProperties(loggerName, fqcn, marker, level, data, t);
        final LogEvent logEvent =
                logEventFactory.createEvent(loggerName, marker, fqcn, location, level, data, props, t);
        try {
            log(logEvent, null);
        } finally {
            // LOG4J2-1583 prevent scrambled logs when logging calls are nested (logging in toString())
            logEventFactory.recycle(logEvent);
        }
    }

    private List<Property> getProperties(
            final String loggerName,
            final String fqcn,
            final Marker marker,
            final Level level,
            final Message data,
            final Throwable t) {
        final List<Property> snapshot = properties;
        if (snapshot == null || !propertiesRequireLookup) {
            return snapshot;
        }
        return getPropertiesWithLookups(loggerName, fqcn, marker, level, data, t, snapshot);
    }

    private List<Property> getPropertiesWithLookups(
            final String loggerName,
            final String fqcn,
            final Marker marker,
            final Level level,
            final Message data,
            final Throwable t,
            final List<Property> props) {
        final List<Property> results = new ArrayList<>(props.size());
        final LogEvent event = Log4jLogEvent.newBuilder()
                .setMessage(data)
                .setMarker(marker)
                .setLevel(level)
                .setLoggerName(loggerName)
                .setLoggerFqcn(fqcn)
                .setThrown(t)
                .build();
        for (int i = 0; i < props.size(); i++) {
            final Property prop = props.get(i);
            final String value = prop.isValueNeedsLookup() // since LOG4J2-1575
                    ? config.getStrSubstitutor().replace(event, prop.getValue()) //
                    : prop.getValue();
            results.add(Property.createProperty(prop.getName(), value));
        }
        return results;
    }

    /**
     * Logs an event.
     *
     * @param event The log event.
     */
    public void log(final LogEvent event) {
        log(event, null);
    }

    /**
     * Logs an event.
     *
     * @param event     The log event.
     * @param predicate predicate for which LoggerConfig instances to append to.
     *                  Use a {@literal null} value instead of a true predicate.
     */
    protected void log(final LogEvent event, final Predicate<LoggerConfig> predicate) {
        if (!isFiltered(event)) {
            processLogEvent(event, predicate);
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

    /**
     * Logs an event, bypassing filters.
     *
     * @param event     The log event.
     * @param predicate predicate for which LoggerConfig instances to append to.
     *                  Use a {@literal null} value instead of a true predicate.
     */
    protected void processLogEvent(final LogEvent event, final Predicate<LoggerConfig> predicate) {
        event.setIncludeLocation(isIncludeLocation());
        if (predicate == null || predicate.test(this)) {
            callAppenders(event);
        }
        logParent(event, predicate);
    }

    public boolean requiresLocation() {
        if (!includeLocation) {
            return false;
        }
        AppenderControl[] controls = appenders.get();
        LoggerConfig loggerConfig = this;
        while (loggerConfig != null) {
            for (final AppenderControl control : controls) {
                if (control.getAppender().requiresLocation()) {
                    return true;
                }
            }
            if (loggerConfig.additive) {
                loggerConfig = loggerConfig.parent;
                if (loggerConfig != null) {
                    controls = loggerConfig.appenders.get();
                }
            } else {
                break;
            }
        }
        return false;
    }

    private void logParent(final LogEvent event, final Predicate<LoggerConfig> predicate) {
        if (additive && parent != null) {
            parent.log(event, predicate);
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

    // Note: for asynchronous loggers, includeLocation default is FALSE,
    // for synchronous loggers, includeLocation default is TRUE.
    protected static boolean includeLocation(final Boolean configuredValue, final Configuration configuration) {
        if (configuredValue == null) {
            final LoggerContext context = configuration.getLoggerContext();
            return context != null && context.includeLocation();
        }
        return configuredValue;
    }

    protected final boolean hasAppenders() {
        return !appenders.isEmpty();
    }

    /**
     * The root Logger.
     */
    @Configurable(printObject = true)
    @Plugin(ROOT)
    public static final class RootLogger extends LoggerConfig {

        @PluginFactory
        public static Builder newRootBuilder() {
            return new Builder();
        }

        private RootLogger() {
            super(Strings.EMPTY, Level.ERROR, false, null);
        }

        /**
         * Builds LoggerConfig instances.
         *            The type to build
         */
        public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<LoggerConfig> {

            protected static final boolean ADDITIVITY = true;

            private Level level;
            private String levelAndRefs;
            // TODO: Change to Boolean, once the DI starts supporting null Booleans.
            private @Nullable Boolean includeLocation;
            private AppenderRef[] refs;
            private Property[] properties;
            private Configuration config;
            private Filter filter;

            public Level getLevel() {
                return level;
            }

            public Builder setLevel(@PluginAttribute final Level level) {
                this.level = level;
                return this;
            }

            public String getLevelAndRefs() {
                return levelAndRefs;
            }

            public Builder setLevelAndRefs(@PluginAttribute final String levelAndRefs) {
                this.levelAndRefs = levelAndRefs;
                return this;
            }

            public @Nullable Boolean getIncludeLocation() {
                return includeLocation;
            }

            public Builder setIncludeLocation(final @Nullable Boolean includeLocation) {
                this.includeLocation = includeLocation;
                return this;
            }

            // TODO: remove this once https://github.com/apache/logging-log4j2/pull/2329 is solved.
            @Deprecated
            public Builder setIncludeLocation(final @PluginAttribute @Nullable String includeLocation) {
                return setIncludeLocation(includeLocation != null ? Boolean.valueOf(includeLocation) : null);
            }

            public AppenderRef[] getRefs() {
                return refs;
            }

            public Builder setRefs(@PluginElement final AppenderRef[] refs) {
                this.refs = refs;
                return this;
            }

            public Property[] getProperties() {
                return properties;
            }

            public Builder setProperties(@PluginElement final Property[] properties) {
                this.properties = properties;
                return this;
            }

            public Configuration getConfig() {
                return config;
            }

            public Builder setConfig(@PluginConfiguration final Configuration config) {
                this.config = config;
                return this;
            }

            public Filter getFilter() {
                return filter;
            }

            public Builder setFilter(@PluginElement final Filter filter) {
                this.filter = filter;
                return this;
            }

            @Override
            public LoggerConfig build() {
                final LevelAndRefs container = LoggerConfig.getLevelAndRefs(level, refs, levelAndRefs, config);
                return new LoggerConfig(
                        LogManager.ROOT_LOGGER_NAME,
                        container.refs,
                        filter,
                        container.level,
                        ADDITIVITY,
                        properties,
                        config,
                        includeLocation(getIncludeLocation(), config));
            }
        }
    }

    protected static LevelAndRefs getLevelAndRefs(
            final Level level, final AppenderRef[] refs, final String levelAndRefs, final Configuration config) {
        final LevelAndRefs result = new LevelAndRefs();
        if (levelAndRefs != null) {
            if (level != null) {
                LOGGER.warn("Level is ignored when levelAndRefs syntax is used.");
            }
            if (refs != null && refs.length > 0) {
                LOGGER.warn("Appender references are ignored when levelAndRefs syntax is used");
            }
            final String[] parts = Strings.splitList(levelAndRefs);
            result.level = Level.getLevel(parts[0]);
            if (parts.length > 1) {
                final List<AppenderRef> refList = new ArrayList<>();
                Arrays.stream(parts)
                        .skip(1)
                        .forEach((ref) -> refList.add(AppenderRef.createAppenderRef(ref, null, null)));
                result.refs = refList;
            }
        } else {
            result.level = level;
            result.refs = refs != null ? Arrays.asList(refs) : new ArrayList<>();
        }
        return result;
    }

    protected Configuration getConfiguration() {
        return config;
    }

    protected static class LevelAndRefs {
        public Level level;
        public List<AppenderRef> refs;
    }
}
