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
package org.apache.log4j;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.FileWatchdog;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.or.RendererMap;
import org.apache.log4j.spi.Configurator;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggerFactory;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.log4j.spi.OptionHandler;
import org.apache.log4j.spi.RendererSupport;
import org.apache.log4j.spi.ThrowableRenderer;
import org.apache.log4j.spi.ThrowableRendererSupport;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.util.StackLocatorUtil;

/**
 * Configures Log4j from properties.
 */
public class PropertyConfigurator implements Configurator {

    static class NameValue {
        String key, value;

        public NameValue(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }

    static class PropertyWatchdog extends FileWatchdog {

        private final ClassLoader classLoader;

        PropertyWatchdog(final String fileName, final ClassLoader classLoader) {
            super(fileName);
            this.classLoader = classLoader;
        }

        /**
         * Call {@link PropertyConfigurator#configure(String)} with the <code>filename</code> to reconfigure log4j.
         */
        @Override
        public void doOnChange() {
            new PropertyConfigurator().doConfigure(filename, LogManager.getLoggerRepository(), classLoader);
        }
    }

    class SortedKeyEnumeration implements Enumeration {

        private final Enumeration e;

        public SortedKeyEnumeration(final Hashtable ht) {
            final Enumeration f = ht.keys();
            final Vector keys = new Vector(ht.size());
            for (int i, last = 0; f.hasMoreElements(); ++last) {
                final String key = (String) f.nextElement();
                for (i = 0; i < last; ++i) {
                    final String s = (String) keys.get(i);
                    if (key.compareTo(s) <= 0) {
                        break;
                    }
                }
                keys.add(i, key);
            }
            e = keys.elements();
        }

        @Override
        public boolean hasMoreElements() {
            return e.hasMoreElements();
        }

        @Override
        public Object nextElement() {
            return e.nextElement();
        }
    }

    static final String CATEGORY_PREFIX = "log4j.category.";
    static final String LOGGER_PREFIX = "log4j.logger.";
    static final String FACTORY_PREFIX = "log4j.factory";
    static final String ADDITIVITY_PREFIX = "log4j.additivity.";
    static final String ROOT_CATEGORY_PREFIX = "log4j.rootCategory";
    static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
    static final String APPENDER_PREFIX = "log4j.appender.";
    static final String RENDERER_PREFIX = "log4j.renderer.";
    static final String THRESHOLD_PREFIX = "log4j.threshold";

    private static final String THROWABLE_RENDERER_PREFIX = "log4j.throwableRenderer";
    private static final String LOGGER_REF = "logger-ref";
    private static final String ROOT_REF = "root-ref";
    private static final String APPENDER_REF_TAG = "appender-ref";

    /**
     * Key for specifying the {@link org.apache.log4j.spi.LoggerFactory LoggerFactory}. Currently set to
     * "<code>log4j.loggerFactory</code>".
     */
    public static final String LOGGER_FACTORY_KEY = "log4j.loggerFactory";

    /**
     * If property set to true, then hierarchy will be reset before configuration.
     */
    private static final String RESET_KEY = "log4j.reset";

    private static final String INTERNAL_ROOT_NAME = "root";

    /**
     * Reads configuration options from an InputStream.
     *
     * @param inputStream The input stream
     */
    public static void configure(final InputStream inputStream) {
        new PropertyConfigurator()
                .doConfigure(inputStream, LogManager.getLoggerRepository(), StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Reads configuration options from <code>properties</code>.
     *
     * See {@link #doConfigure(String, LoggerRepository)} for the expected format.
     *
     * @param properties The properties
     */
    public static void configure(final Properties properties) {
        new PropertyConfigurator()
                .doConfigure(properties, LogManager.getLoggerRepository(), StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Reads configuration options from configuration file.
     *
     * @param fileName The configuration file.
     */
    public static void configure(final String fileName) {
        new PropertyConfigurator()
                .doConfigure(fileName, LogManager.getLoggerRepository(), StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Reads configuration options from url <code>configURL</code>.
     *
     * @param configURL The configuration URL
     */
    public static void configure(final URL configURL) {
        new PropertyConfigurator()
                .doConfigure(configURL, LogManager.getLoggerRepository(), StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Like {@link #configureAndWatch(String, long)} except that the default delay as defined by FileWatchdog.DEFAULT_DELAY
     * is used.
     *
     * @param configFilename A file in key=value format.
     */
    public static void configureAndWatch(final String configFilename) {
        configureAndWatch(configFilename, FileWatchdog.DEFAULT_DELAY, StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Reads the configuration file <code>configFilename</code> if it exists. Moreover, a thread will be created that will
     * periodically check if <code>configFilename</code> has been created or modified. The period is determined by the
     * <code>delay</code> argument. If a change or file creation is detected, then <code>configFilename</code> is read to
     * configure log4j.
     *
     * @param configFilename A file in key=value format.
     * @param delayMillis The delay in milliseconds to wait between each check.
     */
    public static void configureAndWatch(final String configFilename, final long delayMillis) {
        configureAndWatch(configFilename, delayMillis, StackLocatorUtil.getCallerClassLoader(2));
    }

    static void configureAndWatch(final String configFilename, final long delay, final ClassLoader classLoader) {
        final PropertyWatchdog watchdog = new PropertyWatchdog(configFilename, classLoader);
        watchdog.setDelay(delay);
        watchdog.start();
    }

    private static Configuration reconfigure(final Configuration configuration) {
        org.apache.logging.log4j.core.config.Configurator.reconfigure(configuration);
        return configuration;
    }

    /**
     * Used internally to keep track of configured appenders.
     */
    protected Hashtable registry = new Hashtable(11);

    private LoggerRepository repository;

    protected LoggerFactory loggerFactory = new DefaultCategoryFactory();

    /**
     * Checks the provided <code>Properties</code> object for a {@link org.apache.log4j.spi.LoggerFactory LoggerFactory}
     * entry specified by {@link #LOGGER_FACTORY_KEY}. If such an entry exists, an attempt is made to create an instance
     * using the default constructor. This instance is used for subsequent Category creations within this configurator.
     *
     * @see #parseCatsAndRenderers
     */
    protected void configureLoggerFactory(final Properties properties) {
        final String factoryClassName = OptionConverter.findAndSubst(LOGGER_FACTORY_KEY, properties);
        if (factoryClassName != null) {
            LogLog.debug("Setting category factory to [" + factoryClassName + "].");
            loggerFactory = (LoggerFactory)
                    OptionConverter.instantiateByClassName(factoryClassName, LoggerFactory.class, loggerFactory);
            PropertySetter.setProperties(loggerFactory, properties, FACTORY_PREFIX + ".");
        }
    }

    void configureRootCategory(final Properties properties, final LoggerRepository loggerRepository) {
        String effectiveFrefix = ROOT_LOGGER_PREFIX;
        String value = OptionConverter.findAndSubst(ROOT_LOGGER_PREFIX, properties);

        if (value == null) {
            value = OptionConverter.findAndSubst(ROOT_CATEGORY_PREFIX, properties);
            effectiveFrefix = ROOT_CATEGORY_PREFIX;
        }

        if (value == null) {
            LogLog.debug("Could not find root logger information. Is this OK?");
        } else {
            final Logger root = loggerRepository.getRootLogger();
            synchronized (root) {
                parseCategory(properties, root, effectiveFrefix, INTERNAL_ROOT_NAME, value);
            }
        }
    }

    /**
     * Reads configuration options from an InputStream.
     *
     * @param inputStream The input stream
     * @param loggerRepository The hierarchy
     */
    @Override
    public void doConfigure(final InputStream inputStream, final LoggerRepository loggerRepository) {
        doConfigure(inputStream, loggerRepository, StackLocatorUtil.getCallerClassLoader(2));
    }

    Configuration doConfigure(
            final InputStream inputStream, final LoggerRepository loggerRepository, final ClassLoader classLoader) {
        return doConfigure(loadProperties(inputStream), loggerRepository, classLoader);
    }

    /**
     * Reads configuration options from <code>properties</code>.
     *
     * See {@link #doConfigure(String, LoggerRepository)} for the expected format.
     *
     * @param properties The properties
     * @param loggerRepository The hierarchy
     */
    public void doConfigure(final Properties properties, final LoggerRepository loggerRepository) {
        doConfigure(properties, loggerRepository, StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Reads configuration options from <code>properties</code>.
     *
     * See {@link #doConfigure(String, LoggerRepository)} for the expected format.
     *
     * @param properties The properties
     * @param loggerRepository The hierarchy
     */
    Configuration doConfigure(
            final Properties properties, final LoggerRepository loggerRepository, final ClassLoader classLoader) {
        final PropertiesConfiguration configuration =
                new PropertiesConfiguration(LogManager.getContext(classLoader), properties);
        configuration.doConfigure();

        repository = loggerRepository;
        //      String value = properties.getProperty(LogLog.DEBUG_KEY);
        //      if (value == null) {
        //          value = properties.getProperty("log4j.configDebug");
        //          if (value != null) {
        //              LogLog.warn("[log4j.configDebug] is deprecated. Use [log4j.debug] instead.");
        //          }
        //      }
        //
        //      if (value != null) {
        //          LogLog.setInternalDebugging(OptionConverter.toBoolean(value, true));
        //      }
        //
        //      //
        //      // if log4j.reset=true then
        //      // reset hierarchy
        //      final String reset = properties.getProperty(RESET_KEY);
        //      if (reset != null && OptionConverter.toBoolean(reset, false)) {
        //          hierarchy.resetConfiguration();
        //      }
        //
        //      final String thresholdStr = OptionConverter.findAndSubst(THRESHOLD_PREFIX, properties);
        //      if (thresholdStr != null) {
        //          hierarchy.setThreshold(OptionConverter.toLevel(thresholdStr, (Level) Level.ALL));
        //          LogLog.debug("Hierarchy threshold set to [" + hierarchy.getThreshold() + "].");
        //      }
        //
        //      configureRootCategory(properties, hierarchy);
        //      configureLoggerFactory(properties);
        //      parseCatsAndRenderers(properties, hierarchy);
        //
        // We don't want to hold references to appenders preventing their
        // garbage collection.
        registry.clear();

        return reconfigure(configuration);
    }

    /**
     * Reads configuration options from configuration file.
     *
     * @param fileName The configuration file
     * @param loggerRepository The hierarchy
     */
    public void doConfigure(final String fileName, final LoggerRepository loggerRepository) {
        doConfigure(fileName, loggerRepository, StackLocatorUtil.getCallerClassLoader(2));
    }

    /**
     * Reads configuration options from configuration file.
     *
     * @param fileName The configuration file
     * @param loggerRepository The hierarchy
     */
    @SuppressFBWarnings(value = "PATH_TRAVERSAL_IN", justification = "The filename comes from a system property.")
    Configuration doConfigure(
            final String fileName, final LoggerRepository loggerRepository, final ClassLoader classLoader) {
        try (final InputStream inputStream = Files.newInputStream(Paths.get(fileName))) {
            return doConfigure(inputStream, loggerRepository, classLoader);
        } catch (final Exception e) {
            if (e instanceof InterruptedIOException || e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LogLog.error("Could not read configuration file [" + fileName + "].", e);
            LogLog.error("Ignoring configuration file [" + fileName + "].");
            return null;
        }
    }

    /**
     * Read configuration options from url <code>configURL</code>.
     *
     * @param url The configuration URL
     * @param loggerRepository The hierarchy
     */
    @Override
    public void doConfigure(final URL url, final LoggerRepository loggerRepository) {
        doConfigure(url, loggerRepository, StackLocatorUtil.getCallerClassLoader(2));
    }

    Configuration doConfigure(final URL url, final LoggerRepository loggerRepository, final ClassLoader classLoader) {
        LogLog.debug("Reading configuration from URL " + url);
        try {
            final URLConnection urlConnection = UrlConnectionFactory.createConnection(url);
            try (final InputStream inputStream = urlConnection.getInputStream()) {
                return doConfigure(inputStream, loggerRepository, classLoader);
            }
        } catch (final IOException e) {
            LogLog.error("Could not read configuration file from URL [" + url + "].", e);
            LogLog.error("Ignoring configuration file [" + url + "].");
            return null;
        }
    }

    private Properties loadProperties(final InputStream inputStream) {
        final Properties loaded = new Properties();
        try {
            loaded.load(inputStream);
        } catch (final IOException | IllegalArgumentException e) {
            if (e instanceof InterruptedIOException) {
                Thread.currentThread().interrupt();
            }
            LogLog.error("Could not read configuration file from InputStream [" + inputStream + "].", e);
            LogLog.error("Ignoring configuration InputStream [" + inputStream + "].");
            return null;
        }
        return loaded;
    }

    /**
     * Parse the additivity option for a non-root category.
     */
    void parseAdditivityForLogger(final Properties properties, final Logger logger, final String loggerName) {
        final String value = OptionConverter.findAndSubst(ADDITIVITY_PREFIX + loggerName, properties);
        LogLog.debug("Handling " + ADDITIVITY_PREFIX + loggerName + "=[" + value + "]");
        // touch additivity only if necessary
        if ((value != null) && (!value.equals(""))) {
            final boolean additivity = OptionConverter.toBoolean(value, true);
            LogLog.debug("Setting additivity for \"" + loggerName + "\" to " + additivity);
            logger.setAdditivity(additivity);
        }
    }

    Appender parseAppender(final Properties properties, final String appenderName) {
        Appender appender = registryGet(appenderName);
        if ((appender != null)) {
            LogLog.debug("Appender \"" + appenderName + "\" was already parsed.");
            return appender;
        }
        // Appender was not previously initialized.
        final String prefix = APPENDER_PREFIX + appenderName;
        final String layoutPrefix = prefix + ".layout";

        appender =
                (Appender) OptionConverter.instantiateByKey(properties, prefix, org.apache.log4j.Appender.class, null);
        if (appender == null) {
            LogLog.error("Could not instantiate appender named \"" + appenderName + "\".");
            return null;
        }
        appender.setName(appenderName);

        if (appender instanceof OptionHandler) {
            if (appender.requiresLayout()) {
                final Layout layout =
                        (Layout) OptionConverter.instantiateByKey(properties, layoutPrefix, Layout.class, null);
                if (layout != null) {
                    appender.setLayout(layout);
                    LogLog.debug("Parsing layout options for \"" + appenderName + "\".");
                    // configureOptionHandler(layout, layoutPrefix + ".", props);
                    PropertySetter.setProperties(layout, properties, layoutPrefix + ".");
                    LogLog.debug("End of parsing for \"" + appenderName + "\".");
                }
            }
            final String errorHandlerPrefix = prefix + ".errorhandler";
            final String errorHandlerClass = OptionConverter.findAndSubst(errorHandlerPrefix, properties);
            if (errorHandlerClass != null) {
                final ErrorHandler eh = (ErrorHandler)
                        OptionConverter.instantiateByKey(properties, errorHandlerPrefix, ErrorHandler.class, null);
                if (eh != null) {
                    appender.setErrorHandler(eh);
                    LogLog.debug("Parsing errorhandler options for \"" + appenderName + "\".");
                    parseErrorHandler(eh, errorHandlerPrefix, properties, repository);
                    final Properties edited = new Properties();
                    final String[] keys = new String[] {
                        errorHandlerPrefix + "." + ROOT_REF,
                        errorHandlerPrefix + "." + LOGGER_REF,
                        errorHandlerPrefix + "." + APPENDER_REF_TAG
                    };
                    for (final Object element : properties.entrySet()) {
                        final Map.Entry entry = (Map.Entry) element;
                        int i = 0;
                        for (; i < keys.length; i++) {
                            if (keys[i].equals(entry.getKey())) {
                                break;
                            }
                        }
                        if (i == keys.length) {
                            edited.put(entry.getKey(), entry.getValue());
                        }
                    }
                    PropertySetter.setProperties(eh, edited, errorHandlerPrefix + ".");
                    LogLog.debug("End of errorhandler parsing for \"" + appenderName + "\".");
                }
            }
            // configureOptionHandler((OptionHandler) appender, prefix + ".", props);
            PropertySetter.setProperties(appender, properties, prefix + ".");
            LogLog.debug("Parsed \"" + appenderName + "\" options.");
        }
        parseAppenderFilters(properties, appenderName, appender);
        registryPut(appender);
        return appender;
    }

    void parseAppenderFilters(final Properties properties, final String appenderName, final Appender appender) {
        // extract filters and filter options from props into a hashtable mapping
        // the property name defining the filter class to a list of pre-parsed
        // name-value pairs associated to that filter
        final String filterPrefix = APPENDER_PREFIX + appenderName + ".filter.";
        final int fIdx = filterPrefix.length();
        final Hashtable filters = new Hashtable();
        final Enumeration e = properties.keys();
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
                Vector filterOpts = (Vector) filters.get(filterKey);
                if (filterOpts == null) {
                    filterOpts = new Vector();
                    filters.put(filterKey, filterOpts);
                }
                if (dotIdx != -1) {
                    final String value = OptionConverter.findAndSubst(key, properties);
                    filterOpts.add(new NameValue(name, value));
                }
            }
        }

        // sort filters by IDs, insantiate filters, set filter options,
        // add filters to the appender
        final Enumeration g = new SortedKeyEnumeration(filters);
        Filter head = null;
        while (g.hasMoreElements()) {
            final String key = (String) g.nextElement();
            final String clazz = properties.getProperty(key);
            if (clazz != null) {
                LogLog.debug("Filter key: [" + key + "] class: [" + properties.getProperty(key) + "] props: "
                        + filters.get(key));
                final Filter filter = (Filter) OptionConverter.instantiateByClassName(clazz, Filter.class, null);
                if (filter != null) {
                    final PropertySetter propSetter = new PropertySetter(filter);
                    final Vector v = (Vector) filters.get(key);
                    final Enumeration filterProps = v.elements();
                    while (filterProps.hasMoreElements()) {
                        final NameValue kv = (NameValue) filterProps.nextElement();
                        propSetter.setProperty(kv.key, kv.value);
                    }
                    propSetter.activate();
                    LogLog.debug("Adding filter of type [" + filter.getClass() + "] to appender named ["
                            + appender.getName() + "].");
                    head = FilterAdapter.addFilter(head, filter);
                }
            } else {
                LogLog.warn("Missing class definition for filter: [" + key + "]");
            }
        }
        appender.addFilter(head);
    }

    /**
     * This method must work for the root category as well.
     */
    void parseCategory(
            final Properties properties,
            final Logger logger,
            final String optionKey,
            final String loggerName,
            final String value) {

        LogLog.debug("Parsing for [" + loggerName + "] with value=[" + value + "].");
        // We must skip over ',' but not white space
        final StringTokenizer st = new StringTokenizer(value, ",");

        // If value is not in the form ", appender.." or "", then we should set
        // the level of the loggeregory.

        if (!(value.startsWith(",") || value.equals(""))) {

            // just to be on the safe side...
            if (!st.hasMoreTokens()) {
                return;
            }

            final String levelStr = st.nextToken();
            LogLog.debug("Level token is [" + levelStr + "].");

            // If the level value is inherited, set category level value to
            // null. We also check that the user has not specified inherited for the
            // root category.
            if (INHERITED.equalsIgnoreCase(levelStr) || NULL.equalsIgnoreCase(levelStr)) {
                if (loggerName.equals(INTERNAL_ROOT_NAME)) {
                    LogLog.warn("The root logger cannot be set to null.");
                } else {
                    logger.setLevel(null);
                }
            } else {
                logger.setLevel(OptionConverter.toLevel(levelStr, Log4j1Configuration.DEFAULT_LEVEL));
            }
            LogLog.debug("Category " + loggerName + " set to " + logger.getLevel());
        }

        // Begin by removing all existing appenders.
        logger.removeAllAppenders();

        Appender appender;
        String appenderName;
        while (st.hasMoreTokens()) {
            appenderName = st.nextToken().trim();
            if (appenderName == null || appenderName.equals(",")) {
                continue;
            }
            LogLog.debug("Parsing appender named \"" + appenderName + "\".");
            appender = parseAppender(properties, appenderName);
            if (appender != null) {
                logger.addAppender(appender);
            }
        }
    }

    /**
     * Parse non-root elements, such non-root categories and renderers.
     */
    protected void parseCatsAndRenderers(final Properties properties, final LoggerRepository loggerRepository) {
        final Enumeration enumeration = properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            final String key = (String) enumeration.nextElement();
            if (key.startsWith(CATEGORY_PREFIX) || key.startsWith(LOGGER_PREFIX)) {
                String loggerName = null;
                if (key.startsWith(CATEGORY_PREFIX)) {
                    loggerName = key.substring(CATEGORY_PREFIX.length());
                } else if (key.startsWith(LOGGER_PREFIX)) {
                    loggerName = key.substring(LOGGER_PREFIX.length());
                }
                final String value = OptionConverter.findAndSubst(key, properties);
                final Logger logger = loggerRepository.getLogger(loggerName, loggerFactory);
                synchronized (logger) {
                    parseCategory(properties, logger, key, loggerName, value);
                    parseAdditivityForLogger(properties, logger, loggerName);
                }
            } else if (key.startsWith(RENDERER_PREFIX)) {
                final String renderedClass = key.substring(RENDERER_PREFIX.length());
                final String renderingClass = OptionConverter.findAndSubst(key, properties);
                if (loggerRepository instanceof RendererSupport) {
                    RendererMap.addRenderer((RendererSupport) loggerRepository, renderedClass, renderingClass);
                }
            } else if (key.equals(THROWABLE_RENDERER_PREFIX)) {
                if (loggerRepository instanceof ThrowableRendererSupport) {
                    final ThrowableRenderer tr = (ThrowableRenderer) OptionConverter.instantiateByKey(
                            properties, THROWABLE_RENDERER_PREFIX, org.apache.log4j.spi.ThrowableRenderer.class, null);
                    if (tr == null) {
                        LogLog.error("Could not instantiate throwableRenderer.");
                    } else {
                        final PropertySetter setter = new PropertySetter(tr);
                        setter.setProperties(properties, THROWABLE_RENDERER_PREFIX + ".");
                        ((ThrowableRendererSupport) loggerRepository).setThrowableRenderer(tr);
                    }
                }
            }
        }
    }

    private void parseErrorHandler(
            final ErrorHandler errorHandler,
            final String errorHandlerPrefix,
            final Properties props,
            final LoggerRepository loggerRepository) {
        if (errorHandler != null && loggerRepository != null) {
            final boolean rootRef = OptionConverter.toBoolean(
                    OptionConverter.findAndSubst(errorHandlerPrefix + ROOT_REF, props), false);
            if (rootRef) {
                errorHandler.setLogger(loggerRepository.getRootLogger());
            }
            final String loggerName = OptionConverter.findAndSubst(errorHandlerPrefix + LOGGER_REF, props);
            if (loggerName != null) {
                final Logger logger = loggerFactory == null
                        ? loggerRepository.getLogger(loggerName)
                        : loggerRepository.getLogger(loggerName, loggerFactory);
                errorHandler.setLogger(logger);
            }
            final String appenderName = OptionConverter.findAndSubst(errorHandlerPrefix + APPENDER_REF_TAG, props);
            if (appenderName != null) {
                final Appender backup = parseAppender(props, appenderName);
                if (backup != null) {
                    errorHandler.setBackupAppender(backup);
                }
            }
        }
    }

    Appender registryGet(final String name) {
        return (Appender) registry.get(name);
    }

    void registryPut(final Appender appender) {
        registry.put(appender.getName(), appender);
    }
}
