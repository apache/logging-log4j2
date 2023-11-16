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
package org.apache.log4j.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.builders.BuilderManager;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.util.LoaderUtil;

/**
 * Constructs a configuration based on Log4j 1 properties.
 */
public class PropertiesConfiguration extends Log4j1Configuration {

    private static final String CATEGORY_PREFIX = "log4j.category.";
    private static final String LOGGER_PREFIX = "log4j.logger.";
    private static final String ADDITIVITY_PREFIX = "log4j.additivity.";
    private static final String ROOT_CATEGORY_PREFIX = "log4j.rootCategory";
    private static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
    private static final String APPENDER_PREFIX = "log4j.appender.";
    private static final String LOGGER_REF = "logger-ref";
    private static final String ROOT_REF = "root-ref";
    private static final String APPENDER_REF_TAG = "appender-ref";

    /**
     * If property set to true, then hierarchy will be reset before configuration.
     */
    private static final String RESET_KEY = "log4j.reset";

    public static final String THRESHOLD_KEY = "log4j.threshold";
    public static final String DEBUG_KEY = "log4j.debug";

    private static final String INTERNAL_ROOT_NAME = "root";

    private final Map<String, Appender> registry = new HashMap<>();
    private Properties properties;

    /**
     * Constructs a new instance.
     *
     * @param loggerContext The LoggerContext.
     * @param source The ConfigurationSource.
     * @param monitorIntervalSeconds The monitoring interval in seconds.
     */
    public PropertiesConfiguration(
            final LoggerContext loggerContext, final ConfigurationSource source, final int monitorIntervalSeconds) {
        super(loggerContext, source, monitorIntervalSeconds);
    }

    /**
     * Constructs a new instance.
     *
     * @param loggerContext The LoggerContext.
     * @param properties The ConfigurationSource, may be null.
     */
    public PropertiesConfiguration(final LoggerContext loggerContext, final Properties properties) {
        super(loggerContext, ConfigurationSource.NULL_SOURCE, 0);
        this.properties = properties;
    }

    /**
     * Constructs a new instance.
     *
     * @param loggerContext The LoggerContext.
     * @param properties The ConfigurationSource.
     */
    public PropertiesConfiguration(org.apache.logging.log4j.spi.LoggerContext loggerContext, Properties properties) {
        this((LoggerContext) loggerContext, properties);
    }

    @Override
    public void doConfigure() {
        if (properties == null) {
            properties = new Properties();
            final InputStream inputStream = getConfigurationSource().getInputStream();
            if (inputStream != null) {
                try {
                    properties.load(inputStream);
                } catch (final Exception e) {
                    LOGGER.error(
                            "Could not read configuration file [{}].",
                            getConfigurationSource().toString(),
                            e);
                    return;
                }
            }
        }
        // If we reach here, then the config file is alright.
        doConfigure(properties);
    }

    @Override
    public Configuration reconfigure() {
        try {
            final ConfigurationSource source = getConfigurationSource().resetInputStream();
            if (source == null) {
                return null;
            }
            final Configuration config =
                    new PropertiesConfigurationFactory().getConfiguration(getLoggerContext(), source);
            return config == null || config.getState() != State.INITIALIZING ? null : config;
        } catch (final IOException ex) {
            LOGGER.error("Cannot locate file {}: {}", getConfigurationSource(), ex);
        }
        return null;
    }

    /**
     * Reads a configuration from a file. <b>The existing configuration is not cleared nor reset.</b> If you require a
     * different behavior, then call {@link LogManager#resetConfiguration resetConfiguration} method before calling
     * <code>doConfigure</code>.
     * <p>
     * The configuration file consists of statements in the format <code>key=value</code>. The syntax of different
     * configuration elements are discussed below.
     * </p>
     * <p>
     * The level value can consist of the string values OFF, FATAL, ERROR, WARN, INFO, DEBUG, ALL or a <em>custom level</em>
     * value. A custom level value can be specified in the form level#classname. By default the repository-wide threshold is
     * set to the lowest possible value, namely the level <code>ALL</code>.
     * </p>
     *
     * <h3>Appender configuration</h3>
     * <p>
     * Appender configuration syntax is:
     * </p>
     * <pre>
     * # For appender named <i>appenderName</i>, set its class.
     * # Note: The appender name can contain dots.
     * log4j.appender.appenderName=fully.qualified.name.of.appender.class
     *
     * # Set appender specific options.
     * log4j.appender.appenderName.option1=value1
     * ...
     * log4j.appender.appenderName.optionN=valueN
     * </pre>
     * <p>
     * For each named appender you can configure its {@link Layout}. The syntax for configuring an appender's layout is:
     * </p>
     * <pre>
     * log4j.appender.appenderName.layout=fully.qualified.name.of.layout.class
     * log4j.appender.appenderName.layout.option1=value1
     * ....
     * log4j.appender.appenderName.layout.optionN=valueN
     * </pre>
     * <p>
     * The syntax for adding {@link Filter}s to an appender is:
     * </p>
     * <pre>
     * log4j.appender.appenderName.filter.ID=fully.qualified.name.of.filter.class
     * log4j.appender.appenderName.filter.ID.option1=value1
     * ...
     * log4j.appender.appenderName.filter.ID.optionN=valueN
     * </pre>
     * <p>
     * The first line defines the class name of the filter identified by ID; subsequent lines with the same ID specify
     * filter option - value pairs. Multiple filters are added to the appender in the lexicographic order of IDs.
     * </p>
     * <p>
     * The syntax for adding an {@link ErrorHandler} to an appender is:
     * </p>
     * <pre>
     * log4j.appender.appenderName.errorhandler=fully.qualified.name.of.errorhandler.class
     * log4j.appender.appenderName.errorhandler.appender-ref=appenderName
     * log4j.appender.appenderName.errorhandler.option1=value1
     * ...
     * log4j.appender.appenderName.errorhandler.optionN=valueN
     * </pre>
     *
     * <h3>Configuring loggers</h3>
     * <p>
     * The syntax for configuring the root logger is:
     * </p>
     * <pre>
     * log4j.rootLogger=[level], appenderName, appenderName, ...
     * </pre>
     * <p>
     * This syntax means that an optional <em>level</em> can be supplied followed by appender names separated by commas.
     * </p>
     * <p>
     * The level value can consist of the string values OFF, FATAL, ERROR, WARN, INFO, DEBUG, ALL or a <em>custom level</em>
     * value. A custom level value can be specified in the form <code>level#classname</code>.
     * </p>
     * <p>
     * If a level value is specified, then the root level is set to the corresponding level. If no level value is specified,
     * then the root level remains untouched.
     * </p>
     * <p>
     * The root logger can be assigned multiple appenders.
     * </p>
     * <p>
     * Each <i>appenderName</i> (separated by commas) will be added to the root logger. The named appender is defined using
     * the appender syntax defined above.
     * </p>
     * <p>
     * For non-root categories the syntax is almost the same:
     * </p>
     * <pre>
     * log4j.logger.logger_name=[level|INHERITED|NULL], appenderName, appenderName, ...
     * </pre>
     * <p>
     * The meaning of the optional level value is discussed above in relation to the root logger. In addition however, the
     * value INHERITED can be specified meaning that the named logger should inherit its level from the logger hierarchy.
     * </p>
     * <p>
     * If no level value is supplied, then the level of the named logger remains untouched.
     * </p>
     * <p>
     * By default categories inherit their level from the hierarchy. However, if you set the level of a logger and later
     * decide that that logger should inherit its level, then you should specify INHERITED as the value for the level value.
     * NULL is a synonym for INHERITED.
     * </p>
     * <p>
     * Similar to the root logger syntax, each <i>appenderName</i> (separated by commas) will be attached to the named
     * logger.
     * </p>
     * <p>
     * See the <a href="../../../../manual.html#additivity">appender additivity rule</a> in the user manual for the meaning
     * of the <code>additivity</code> flag.
     * </p>
     * <pre>
     * # Set options for appender named "A1". # Appender "A1" will be a SyslogAppender
     * log4j.appender.A1=org.apache.log4j.net.SyslogAppender
     *
     * # The syslog daemon resides on www.abc.net log4j.appender.A1.SyslogHost=www.abc.net
     *
     * # A1's layout is a PatternLayout, using the conversion pattern # <b>%r %-5p %c{2} %M.%L %x - %m\n</b>. Thus, the log
     * output will # include # the relative time since the start of the application in # milliseconds, followed by the level
     * of the log request, # followed by the two rightmost components of the logger name, # followed by the callers method
     * name, followed by the line number, # the nested diagnostic context and finally the message itself. # Refer to the
     * documentation of {@link PatternLayout} for further information # on the syntax of the ConversionPattern key.
     * log4j.appender.A1.layout=org.apache.log4j.PatternLayout log4j.appender.A1.layout.ConversionPattern=%-4r %-5p %c{2}
     * %M.%L %x - %m\n
     *
     * # Set options for appender named "A2" # A2 should be a RollingFileAppender, with maximum file size of 10 MB # using
     * at most one backup file. A2's layout is TTCC, using the # ISO8061 date format with context printing enabled.
     * log4j.appender.A2=org.apache.log4j.RollingFileAppender log4j.appender.A2.MaxFileSize=10MB
     * log4j.appender.A2.MaxBackupIndex=1 log4j.appender.A2.layout=org.apache.log4j.TTCCLayout
     * log4j.appender.A2.layout.ContextPrinting=enabled log4j.appender.A2.layout.DateFormat=ISO8601
     *
     * # Root logger set to DEBUG using the A2 appender defined above. log4j.rootLogger=DEBUG, A2
     *
     * # Logger definitions: # The SECURITY logger inherits is level from root. However, it's output # will go to A1
     * appender defined above. It's additivity is non-cumulative. log4j.logger.SECURITY=INHERIT, A1
     * log4j.additivity.SECURITY=false
     *
     * # Only warnings or above will be logged for the logger "SECURITY.access". # Output will go to A1.
     * log4j.logger.SECURITY.access=WARN
     *
     *
     * # The logger "class.of.the.day" inherits its level from the # logger hierarchy. Output will go to the appender's of
     * the root # logger, A2 in this case. log4j.logger.class.of.the.day=INHERIT
     * </pre>
     * <p>
     * Refer to the <b>setOption</b> method in each Appender and Layout for class specific options.
     * </p>
     * <p>
     * Use the <code>#</code> or <code>!</code> characters at the beginning of a line for comments.
     * </p>
     */
    private void doConfigure(final Properties properties) {
        String status = "error";
        String value = properties.getProperty(DEBUG_KEY);
        if (value == null) {
            value = properties.getProperty("log4j.configDebug");
            if (value != null) {
                LOGGER.warn("[log4j.configDebug] is deprecated. Use [log4j.debug] instead.");
            }
        }

        if (value != null) {
            status = OptionConverter.toBoolean(value, false) ? "debug" : "error";
        }

        final StatusConfiguration statusConfig = new StatusConfiguration().withStatus(status);
        statusConfig.initialize();

        // if log4j.reset=true then reset hierarchy
        final String reset = properties.getProperty(RESET_KEY);
        if (reset != null && OptionConverter.toBoolean(reset, false)) {
            LogManager.resetConfiguration();
        }

        final String threshold = OptionConverter.findAndSubst(THRESHOLD_KEY, properties);
        if (threshold != null) {
            final Level level = OptionConverter.convertLevel(threshold.trim(), Level.ALL);
            addFilter(ThresholdFilter.createFilter(level, Result.NEUTRAL, Result.DENY));
        }

        configureRoot(properties);
        parseLoggers(properties);

        LOGGER.debug("Finished configuring.");
    }

    // --------------------------------------------------------------------------
    // Internal stuff
    // --------------------------------------------------------------------------

    private void configureRoot(final Properties props) {
        String effectiveFrefix = ROOT_LOGGER_PREFIX;
        String value = OptionConverter.findAndSubst(ROOT_LOGGER_PREFIX, props);

        if (value == null) {
            value = OptionConverter.findAndSubst(ROOT_CATEGORY_PREFIX, props);
            effectiveFrefix = ROOT_CATEGORY_PREFIX;
        }

        if (value == null) {
            LOGGER.debug("Could not find root logger information. Is this OK?");
        } else {
            final LoggerConfig root = getRootLogger();
            parseLogger(props, root, effectiveFrefix, INTERNAL_ROOT_NAME, value);
        }
    }

    /**
     * Parses non-root elements, such non-root categories and renderers.
     */
    private void parseLoggers(final Properties props) {
        final Enumeration<?> enumeration = props.propertyNames();
        while (enumeration.hasMoreElements()) {
            final String key = Objects.toString(enumeration.nextElement(), null);
            if (key.startsWith(CATEGORY_PREFIX) || key.startsWith(LOGGER_PREFIX)) {
                String loggerName = null;
                if (key.startsWith(CATEGORY_PREFIX)) {
                    loggerName = key.substring(CATEGORY_PREFIX.length());
                } else if (key.startsWith(LOGGER_PREFIX)) {
                    loggerName = key.substring(LOGGER_PREFIX.length());
                }
                final String value = OptionConverter.findAndSubst(key, props);
                LoggerConfig loggerConfig = getLogger(loggerName);
                if (loggerConfig == null) {
                    final boolean additivity = getAdditivityForLogger(props, loggerName);
                    loggerConfig = new LoggerConfig(loggerName, org.apache.logging.log4j.Level.ERROR, additivity);
                    addLogger(loggerName, loggerConfig);
                }
                parseLogger(props, loggerConfig, key, loggerName, value);
            }
        }
    }

    /**
     * Parses the additivity option for a non-root category.
     */
    private boolean getAdditivityForLogger(final Properties props, final String loggerName) {
        boolean additivity = true;
        final String key = ADDITIVITY_PREFIX + loggerName;
        final String value = OptionConverter.findAndSubst(key, props);
        LOGGER.debug("Handling {}=[{}]", key, value);
        // touch additivity only if necessary
        if ((value != null) && (!value.equals(""))) {
            additivity = OptionConverter.toBoolean(value, true);
        }
        return additivity;
    }

    /**
     * This method must work for the root category as well.
     */
    private void parseLogger(
            final Properties props,
            final LoggerConfig loggerConfig,
            final String optionKey,
            final String loggerName,
            final String value) {

        LOGGER.debug("Parsing for [{}] with value=[{}].", loggerName, value);
        // We must skip over ',' but not white space
        final StringTokenizer st = new StringTokenizer(value, ",");

        // If value is not in the form ", appender.." or "", then we should set the level of the logger.

        if (!(value.startsWith(",") || value.equals(""))) {

            // just to be on the safe side...
            if (!st.hasMoreTokens()) {
                return;
            }

            final String levelStr = st.nextToken();
            LOGGER.debug("Level token is [{}].", levelStr);

            final org.apache.logging.log4j.Level level = levelStr == null
                    ? org.apache.logging.log4j.Level.ERROR
                    : OptionConverter.convertLevel(levelStr, org.apache.logging.log4j.Level.DEBUG);
            loggerConfig.setLevel(level);
            LOGGER.debug("Logger {} level set to {}", loggerName, level);
        }

        Appender appender;
        String appenderName;
        while (st.hasMoreTokens()) {
            appenderName = st.nextToken().trim();
            if (appenderName == null || appenderName.equals(",")) {
                continue;
            }
            LOGGER.debug("Parsing appender named \"{}\".", appenderName);
            appender = parseAppender(props, appenderName);
            if (appender != null) {
                LOGGER.debug("Adding appender named [{}] to loggerConfig [{}].", appenderName, loggerConfig.getName());
                loggerConfig.addAppender(getAppender(appenderName), null, null);
            } else {
                LOGGER.debug("Appender named [{}] not found.", appenderName);
            }
        }
    }

    public Appender parseAppender(final Properties props, final String appenderName) {
        Appender appender = registry.get(appenderName);
        if ((appender != null)) {
            LOGGER.debug("Appender \"" + appenderName + "\" was already parsed.");
            return appender;
        }
        // Appender was not previously initialized.
        final String prefix = APPENDER_PREFIX + appenderName;
        final String layoutPrefix = prefix + ".layout";
        final String filterPrefix = APPENDER_PREFIX + appenderName + ".filter.";
        final String className = OptionConverter.findAndSubst(prefix, props);
        if (className == null) {
            LOGGER.debug("Appender \"" + appenderName + "\" does not exist.");
            return null;
        }
        appender = manager.parseAppender(appenderName, className, prefix, layoutPrefix, filterPrefix, props, this);
        if (appender == null) {
            appender = buildAppender(appenderName, className, prefix, layoutPrefix, filterPrefix, props);
        } else {
            registry.put(appenderName, appender);
            addAppender(AppenderAdapter.adapt(appender));
        }
        return appender;
    }

    private Appender buildAppender(
            final String appenderName,
            final String className,
            final String prefix,
            final String layoutPrefix,
            final String filterPrefix,
            final Properties props) {
        final Appender appender = newInstanceOf(className, "Appender");
        if (appender == null) {
            return null;
        }
        appender.setName(appenderName);
        appender.setLayout(parseLayout(layoutPrefix, appenderName, props));
        final String errorHandlerPrefix = prefix + ".errorhandler";
        final String errorHandlerClass = OptionConverter.findAndSubst(errorHandlerPrefix, props);
        if (errorHandlerClass != null) {
            final ErrorHandler eh = parseErrorHandler(props, errorHandlerPrefix, errorHandlerClass, appender);
            if (eh != null) {
                appender.setErrorHandler(eh);
            }
        }
        appender.addFilter(parseAppenderFilters(props, filterPrefix, appenderName));
        final String[] keys = new String[] {layoutPrefix};
        addProperties(appender, keys, props, prefix);
        addAppender(AppenderAdapter.adapt(appender));
        registry.put(appenderName, appender);
        return appender;
    }

    public Layout parseLayout(final String layoutPrefix, final String appenderName, final Properties props) {
        final String layoutClass = OptionConverter.findAndSubst(layoutPrefix, props);
        if (layoutClass == null) {
            return null;
        }
        Layout layout = manager.parse(layoutClass, layoutPrefix, props, this, BuilderManager.INVALID_LAYOUT);
        if (layout == null) {
            layout = buildLayout(layoutPrefix, layoutClass, appenderName, props);
        }
        return layout;
    }

    private Layout buildLayout(
            final String layoutPrefix, final String className, final String appenderName, final Properties props) {
        final Layout layout = newInstanceOf(className, "Layout");
        if (layout == null) {
            return null;
        }
        LOGGER.debug("Parsing layout options for \"{}\".", appenderName);
        PropertySetter.setProperties(layout, props, layoutPrefix + ".");
        LOGGER.debug("End of parsing for \"{}\".", appenderName);
        return layout;
    }

    public ErrorHandler parseErrorHandler(
            final Properties props,
            final String errorHandlerPrefix,
            final String errorHandlerClass,
            final Appender appender) {
        final ErrorHandler eh = newInstanceOf(errorHandlerClass, "ErrorHandler");
        final String[] keys = new String[] {
            // @formatter:off
            errorHandlerPrefix + "." + ROOT_REF,
            errorHandlerPrefix + "." + LOGGER_REF,
            errorHandlerPrefix + "." + APPENDER_REF_TAG
        };
        // @formatter:on
        addProperties(eh, keys, props, errorHandlerPrefix);
        return eh;
    }

    public void addProperties(final Object obj, final String[] keys, final Properties props, final String prefix) {
        final Properties edited = new Properties();
        props.stringPropertyNames().stream()
                .filter(name -> {
                    if (name.startsWith(prefix)) {
                        for (final String key : keys) {
                            if (name.equals(key)) {
                                return false;
                            }
                        }
                        return true;
                    }
                    return false;
                })
                .forEach(name -> edited.put(name, props.getProperty(name)));
        PropertySetter.setProperties(obj, edited, prefix + ".");
    }

    public Filter parseAppenderFilters(final Properties props, final String filterPrefix, final String appenderName) {
        // extract filters and filter options from props into a hashtable mapping
        // the property name defining the filter class to a list of pre-parsed
        // name-value pairs associated to that filter
        final int fIdx = filterPrefix.length();
        final SortedMap<String, List<NameValue>> filters = new TreeMap<>();
        final Enumeration<?> e = props.keys();
        String name = "";
        while (e.hasMoreElements()) {
            final String key = (String) e.nextElement();
            if (key.startsWith(filterPrefix)) {
                final int dotIdx = key.indexOf('.', fIdx);
                String filterKey = key;
                if (dotIdx != -1) {
                    filterKey = key.substring(0, dotIdx);
                    name = key.substring(dotIdx + 1);
                }
                final List<NameValue> filterOpts = filters.computeIfAbsent(filterKey, k -> new ArrayList<>());
                if (dotIdx != -1) {
                    final String value = OptionConverter.findAndSubst(key, props);
                    filterOpts.add(new NameValue(name, value));
                }
            }
        }

        Filter head = null;
        for (final Map.Entry<String, List<NameValue>> entry : filters.entrySet()) {
            final String clazz = props.getProperty(entry.getKey());
            Filter filter = null;
            if (clazz != null) {
                filter = manager.parse(clazz, entry.getKey(), props, this, BuilderManager.INVALID_FILTER);
                if (filter == null) {
                    LOGGER.debug("Filter key: [{}] class: [{}] props: {}", entry.getKey(), clazz, entry.getValue());
                    filter = buildFilter(clazz, appenderName, entry.getValue());
                }
            }
            head = FilterAdapter.addFilter(head, filter);
        }
        return head;
    }

    private Filter buildFilter(final String className, final String appenderName, final List<NameValue> props) {
        final Filter filter = newInstanceOf(className, "Filter");
        if (filter != null) {
            final PropertySetter propSetter = new PropertySetter(filter);
            for (final NameValue property : props) {
                propSetter.setProperty(property.key, property.value);
            }
            propSetter.activate();
        }
        return filter;
    }

    public TriggeringPolicy parseTriggeringPolicy(final Properties props, final String policyPrefix) {
        final String policyClass = OptionConverter.findAndSubst(policyPrefix, props);
        if (policyClass == null) {
            return null;
        }
        return manager.parse(policyClass, policyPrefix, props, this, null);
    }

    private static <T> T newInstanceOf(final String className, final String type) {
        try {
            return LoaderUtil.newInstanceOf(className);
        } catch (ReflectiveOperationException ex) {
            LOGGER.error(
                    "Unable to create {} {} due to {}:{}",
                    type,
                    className,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex);
            return null;
        }
    }

    private static class NameValue {
        String key, value;

        NameValue(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
