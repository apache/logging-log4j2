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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerContextSelector;
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.MapLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * The base Configuration. Many configuration implementations will extend this class.
 */
public abstract class AbstractConfiguration extends AbstractFilterable implements Configuration {

    private static final long serialVersionUID = 1L;

    private static final int BUF_SIZE = 16384;

    /**
     * The root node of the configuration.
     */
    protected Node rootNode;

    /**
     * Listeners for configuration changes.
     */
    protected final List<ConfigurationListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * The ConfigurationMonitor that checks for configuration changes.
     */
    protected ConfigurationMonitor monitor = new DefaultConfigurationMonitor();

    /**
     * Packages found in configuration "packages" attribute.
     */
    protected final List<String> pluginPackages = new ArrayList<>();
    
    /**
     * The plugin manager.
     */
    protected PluginManager pluginManager;

    /**
     * Shutdown hook is enabled by default.
     */
    protected boolean isShutdownHookEnabled = true;

    /**
     * The Advertiser which exposes appender configurations to external systems.
     */
    private Advertiser advertiser = new DefaultAdvertiser();
    private Node advertiserNode = null;
    private Object advertisement;
    private String name;
    private ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<>();
    private ConcurrentMap<String, LoggerConfig> loggers = new ConcurrentHashMap<>();
    private List<CustomLevelConfig> customLevels = Collections.emptyList();
    private final ConcurrentMap<String, String> properties = new ConcurrentHashMap<>();
    private final StrLookup tempLookup = new Interpolator(properties);
    private final StrSubstitutor subst = new StrSubstitutor(tempLookup);
    private LoggerConfig root = new LoggerConfig();
    private final ConcurrentMap<String, Object> componentMap = new ConcurrentHashMap<>();
    private final ConfigurationSource configurationSource;
    private ScriptManager scriptManager;
    private ScheduledExecutorService executorService;
    private final WatchManager watchManager = new WatchManager();

    /**
     * Constructor.
     */
    protected AbstractConfiguration(final ConfigurationSource configurationSource) {
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource is null");
        componentMap.put(Configuration.CONTEXT_PROPERTIES, properties);
        pluginManager = new PluginManager(Node.CATEGORY);
        rootNode = new Node();
        setState(State.INITIALIZING);

    }

    @Override
    public ConfigurationSource getConfigurationSource() {
        return configurationSource;
    }

    @Override
    public List<String> getPluginPackages() {
        return pluginPackages;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public WatchManager getWatchManager() {
        return watchManager;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Initialize the configuration.
     */
    @Override
    public void initialize() {
        LOGGER.debug("Initializing configuration {}", this);
        if (watchManager.getIntervalSeconds() > 0) {
            executorService = new ScheduledThreadPoolExecutor(1);
            watchManager.setExecutorService(executorService);
        }
        scriptManager = new ScriptManager(watchManager);
        pluginManager.collectPlugins(pluginPackages);
        final PluginManager levelPlugins = new PluginManager(Level.CATEGORY);
        levelPlugins.collectPlugins(pluginPackages);
        final Map<String, PluginType<?>> plugins = levelPlugins.getPlugins();
        if (plugins != null) {
            for (final PluginType<?> type : plugins.values()) {
                try {
                    // Cause the class to be initialized if it isn't already.
                    Loader.initializeClass(type.getPluginClass().getName(), type.getPluginClass().getClassLoader());
                } catch (final Exception e) {
                    LOGGER.error("Unable to initialize {} due to {}", type.getPluginClass().getName(), e.getClass()
                            .getSimpleName(), e);
                }
            }
        }
        setup();
        setupAdvertisement();
        doConfigure();
        setState(State.INITIALIZED);
        LOGGER.debug("Configuration {} initialized", this);
    }

    /**
     * Start the configuration.
     */
    @Override
    public void start() {
        // Preserve the prior behavior of initializing during start if not initialized.
        if (getState().equals(State.INITIALIZING)) {
            initialize();
        }
        LOGGER.debug("Starting configuration {}", this);
        this.setStarting();
        if (watchManager.getIntervalSeconds() > 0) {
            watchManager.start();
        }
        final Set<LoggerConfig> alreadyStarted = new HashSet<>();
        for (final LoggerConfig logger : loggers.values()) {
            logger.start();
            alreadyStarted.add(logger);
        }
        for (final Appender appender : appenders.values()) {
            appender.start();
        }
        if (!alreadyStarted.contains(root)) { // LOG4J2-392
            root.start(); // LOG4J2-336
        }
        super.start();
        LOGGER.debug("Started configuration {} OK.", this);
    }

    /**
     * Tear down the configuration.
     */
    @Override
    public void stop() {
        this.setStopping();
        LOGGER.trace("Stopping {}...", this);

        for (final LoggerConfig loggerConfig : loggers.values()) {
            loggerConfig.getReliabilityStrategy().beforeStopConfiguration(this);
        }
        LOGGER.trace("AbstractConfiguration notified {} ReliabilityStrategies that config will be stopped.",
                loggers.size());

        // LOG4J2-392 first stop AsyncLogger Disruptor thread
        final LoggerContextFactory factory = LogManager.getFactory();
        if (factory instanceof Log4jContextFactory) {
            final ContextSelector selector = ((Log4jContextFactory) factory).getSelector();
            if (selector instanceof AsyncLoggerContextSelector) { // all loggers are async
                // TODO until LOG4J2-493 is fixed we can only stop AsyncLogger once!
                // but LoggerContext.setConfiguration will call config.stop()
                // every time the configuration changes...
                //
                // Uncomment the line below after LOG4J2-493 is fixed
                // AsyncLogger.stop();
                // LOGGER.trace("AbstractConfiguration stopped AsyncLogger disruptor.");
            }
        }
        // similarly, first stop AsyncLoggerConfig Disruptor thread(s)
        final Set<LoggerConfig> alreadyStopped = new HashSet<>();
        int asyncLoggerConfigCount = 0;
        for (final LoggerConfig logger : loggers.values()) {
            if (logger instanceof AsyncLoggerConfig) {
                // LOG4J2-520, LOG4J2-392:
                // Important: do not clear appenders until after all AsyncLoggerConfigs
                // have been stopped! Stopping the last AsyncLoggerConfig will
                // shut down the disruptor and wait for all enqueued events to be processed.
                // Only *after this* the appenders can be cleared or events will be lost.
                logger.stop();
                asyncLoggerConfigCount++;
                alreadyStopped.add(logger);
            }
        }
        if (root instanceof AsyncLoggerConfig & !alreadyStopped.contains(root)) { // LOG4J2-807
            root.stop();
            asyncLoggerConfigCount++;
            alreadyStopped.add(root);
        }
        LOGGER.trace("AbstractConfiguration stopped {} AsyncLoggerConfigs.", asyncLoggerConfigCount);

        // Stop the appenders in reverse order in case they still have activity.
        final Appender[] array = appenders.values().toArray(new Appender[appenders.size()]);

        // LOG4J2-511, LOG4J2-392 stop AsyncAppenders first
        int asyncAppenderCount = 0;
        for (int i = array.length - 1; i >= 0; --i) {
            if (array[i] instanceof AsyncAppender) {
                array[i].stop();
                asyncAppenderCount++;
            }
        }
        LOGGER.trace("AbstractConfiguration stopped {} AsyncAppenders.", asyncAppenderCount);

        for (final LoggerConfig loggerConfig : loggers.values()) {
            loggerConfig.getReliabilityStrategy().beforeStopAppenders();
        }
        LOGGER.trace("AbstractConfiguration notified {} ReliabilityStrategies that appenders will be stopped.",
                loggers.size());

        int appenderCount = 0;
        for (int i = array.length - 1; i >= 0; --i) {
            if (array[i].isStarted()) { // then stop remaining Appenders
                array[i].stop();
                appenderCount++;
            }
        }
        LOGGER.trace("AbstractConfiguration stopped {} Appenders.", appenderCount);

        int loggerCount = 0;
        for (final LoggerConfig loggerConfig : loggers.values()) {

            // AsyncLoggerConfigHelper decreases its ref count when an AsyncLoggerConfig is stopped.
            // Stopping the same AsyncLoggerConfig twice results in an incorrect ref count and
            // the shared Disruptor may be shut down prematurely, resulting in NPE or other errors.
            if (!alreadyStopped.contains(loggerConfig)) {
                loggerConfig.stop();
                loggerCount++;
            }
            loggerConfig.clearAppenders();
        }
        LOGGER.trace("AbstractConfiguration stopped {} LoggerConfigs.", loggerCount);

        if (watchManager.isStarted()) {
            watchManager.stop();
        }
        if (executorService != null) {
            executorService.shutdown();
        }

        // AsyncLoggerConfigHelper decreases its ref count when an AsyncLoggerConfig is stopped.
        // Stopping the same AsyncLoggerConfig twice results in an incorrect ref count and
        // the shared Disruptor may be shut down prematurely, resulting in NPE or other errors.
        if (!alreadyStopped.contains(root)) {
            root.stop();
        }
        super.stop();
        if (advertiser != null && advertisement != null) {
            advertiser.unadvertise(advertisement);
        }
        LOGGER.debug("Stopped {} OK", this);
    }

    @Override
    public boolean isShutdownHookEnabled() {
        return isShutdownHookEnabled;
    }

    protected void setup() {
    }

    protected Level getDefaultStatus() {
        final String statusLevel = PropertiesUtil.getProperties().getStringProperty(
                Constants.LOG4J_DEFAULT_STATUS_LEVEL, Level.ERROR.name());
        try {
            return Level.toLevel(statusLevel);
        } catch (final Exception ex) {
            return Level.ERROR;
        }
    }

    protected void createAdvertiser(final String advertiserString, final ConfigurationSource configSource,
            final byte[] buffer, final String contentType) {
        if (advertiserString != null) {
            final Node node = new Node(null, advertiserString, null);
            final Map<String, String> attributes = node.getAttributes();
            attributes.put("content", new String(buffer));
            attributes.put("contentType", contentType);
            attributes.put("name", "configuration");
            if (configSource.getLocation() != null) {
                attributes.put("location", configSource.getLocation());
            }
            advertiserNode = node;
        }
    }

    private void setupAdvertisement() {
        if (advertiserNode != null) {
            final String nodeName = advertiserNode.getName();
            final PluginType<?> type = pluginManager.getPluginType(nodeName);
            if (type != null) {
                final Class<? extends Advertiser> clazz = type.getPluginClass().asSubclass(Advertiser.class);
                try {
                    advertiser = clazz.newInstance();
                    advertisement = advertiser.advertise(advertiserNode.getAttributes());
                } catch (final InstantiationException e) {
                    LOGGER.error("InstantiationException attempting to instantiate advertiser: {}", nodeName, e);
                } catch (final IllegalAccessException e) {
                    LOGGER.error("IllegalAccessException attempting to instantiate advertiser: {}", nodeName, e);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getComponent(final String componentName) {
        return (T) componentMap.get(componentName);
    }

    @Override
    public void addComponent(final String componentName, final Object obj) {
        componentMap.putIfAbsent(componentName, obj);
    }

    protected void doConfigure() {
        if (rootNode.hasChildren() && rootNode.getChildren().get(0).getName().equalsIgnoreCase("Properties")) {
            final Node first = rootNode.getChildren().get(0);
            createConfiguration(first, null);
            if (first.getObject() != null) {
                subst.setVariableResolver((StrLookup) first.getObject());
            }
        } else {
            final Map<String, String> map = this.getComponent(CONTEXT_PROPERTIES);
            final StrLookup lookup = map == null ? null : new MapLookup(map);
            subst.setVariableResolver(new Interpolator(lookup, pluginPackages));
        }

        boolean setLoggers = false;
        boolean setRoot = false;
        for (final Node child : rootNode.getChildren()) {
            if (child.getName().equalsIgnoreCase("Properties")) {
                if (tempLookup == subst.getVariableResolver()) {
                    LOGGER.error("Properties declaration must be the first element in the configuration");
                }
                continue;
            }
            createConfiguration(child, null);
            if (child.getObject() == null) {
                continue;
            }
            if (child.getName().equalsIgnoreCase("Scripts")) {
                for (AbstractScript script : child.getObject(AbstractScript[].class)) {
                   scriptManager.addScript(script);
                }
            } else if (child.getName().equalsIgnoreCase("Appenders")) {
                appenders = child.getObject();
            } else if (child.isInstanceOf(Filter.class)) {
                addFilter(child.getObject(Filter.class));
            } else if (child.getName().equalsIgnoreCase("Loggers")) {
                final Loggers l = child.getObject();
                loggers = l.getMap();
                setLoggers = true;
                if (l.getRoot() != null) {
                    root = l.getRoot();
                    setRoot = true;
                }
            } else if (child.getName().equalsIgnoreCase("CustomLevels")) {
                customLevels = child.getObject(CustomLevels.class).getCustomLevels();
            } else if (child.isInstanceOf(CustomLevelConfig.class)) {
                final List<CustomLevelConfig> copy = new ArrayList<>(customLevels);
                copy.add(child.getObject(CustomLevelConfig.class));
                customLevels = copy;
            } else {
                LOGGER.error("Unknown object \"{}\" of type {} is ignored.", child.getName(), child.getObject()
                        .getClass().getName());
            }
        }

        if (!setLoggers) {
            LOGGER.warn("No Loggers were configured, using default. Is the Loggers element missing?");
            setToDefault();
            return;
        } else if (!setRoot) {
            LOGGER.warn("No Root logger was configured, creating default ERROR-level Root logger with Console appender");
            setToDefault();
            // return; // LOG4J2-219: creating default root=ok, but don't exclude configured Loggers
        }

        for (final Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            final LoggerConfig loggerConfig = entry.getValue();
            for (final AppenderRef ref : loggerConfig.getAppenderRefs()) {
                final Appender app = appenders.get(ref.getRef());
                if (app != null) {
                    loggerConfig.addAppender(app, ref.getLevel(), ref.getFilter());
                } else {
                    LOGGER.error("Unable to locate appender \"{}\" for logger config \"{}\"", ref.getRef(),
                            loggerConfig);
                }
            }

        }

        setParents();
    }

    private void setToDefault() {
        // TODO: reduce duplication between this method and DefaultConfiguration constructor
        setName(DefaultConfiguration.DEFAULT_NAME);
        final Layout<? extends Serializable> layout = PatternLayout.newBuilder()
                .withPattern(DefaultConfiguration.DEFAULT_PATTERN).withConfiguration(this).build();
        final Appender appender = ConsoleAppender.createDefaultAppenderForLayout(layout);
        appender.start();
        addAppender(appender);
        final LoggerConfig loggerConfig = getRootLogger();
        loggerConfig.addAppender(appender, null, null);

        final String levelName = PropertiesUtil.getProperties().getStringProperty(DefaultConfiguration.DEFAULT_LEVEL);
        final Level level = levelName != null && Level.getLevel(levelName) != null ? Level.getLevel(levelName)
                : Level.ERROR;
        loggerConfig.setLevel(level);
    }

    /**
     * Set the name of the configuration.
     * 
     * @param name The name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the name of the configuration.
     * 
     * @return the name of the configuration.
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Add a listener for changes on the configuration.
     * 
     * @param listener The ConfigurationListener to add.
     */
    @Override
    public void addListener(final ConfigurationListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a ConfigurationListener.
     * 
     * @param listener The ConfigurationListener to remove.
     */
    @Override
    public void removeListener(final ConfigurationListener listener) {
        listeners.remove(listener);
    }

    /**
     * Returns the Appender with the specified name.
     * 
     * @param appenderName The name of the Appender.
     * @return the Appender with the specified name or null if the Appender cannot be located.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Appender> T getAppender(final String appenderName) {
        return (T) appenders.get(appenderName);
    }

    /**
     * Returns a Map containing all the Appenders and their name.
     * 
     * @return A Map containing each Appender's name and the Appender object.
     */
    @Override
    public Map<String, Appender> getAppenders() {
        return appenders;
    }

    /**
     * Adds an Appender to the configuration.
     * 
     * @param appender The Appender to add.
     */
    @Override
    public void addAppender(final Appender appender) {
        appenders.putIfAbsent(appender.getName(), appender);
    }

    @Override
    public StrSubstitutor getStrSubstitutor() {
        return subst;
    }

    @Override
    public void setConfigurationMonitor(final ConfigurationMonitor monitor) {
        this.monitor = monitor;
    }

    @Override
    public ConfigurationMonitor getConfigurationMonitor() {
        return monitor;
    }

    @Override
    public void setAdvertiser(final Advertiser advertiser) {
        this.advertiser = advertiser;
    }

    @Override
    public Advertiser getAdvertiser() {
        return advertiser;
    }

    /**
     * Associates an Appender with a LoggerConfig. This method is synchronized in case a Logger with the same name is
     * being updated at the same time.
     *
     * Note: This method is not used when configuring via configuration. It is primarily used by unit tests.
     * 
     * @param logger The Logger the Appender will be associated with.
     * @param appender The Appender.
     */
    @Override
    public synchronized void addLoggerAppender(final org.apache.logging.log4j.core.Logger logger,
            final Appender appender) {
        final String loggerName = logger.getName();
        appenders.putIfAbsent(appender.getName(), appender);
        final LoggerConfig lc = getLoggerConfig(loggerName);
        if (lc.getName().equals(loggerName)) {
            lc.addAppender(appender, null, null);
        } else {
            final LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), lc.isAdditive());
            nlc.addAppender(appender, null, null);
            nlc.setParent(lc);
            loggers.putIfAbsent(loggerName, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }

    /**
     * Associates a Filter with a LoggerConfig. This method is synchronized in case a Logger with the same name is being
     * updated at the same time.
     *
     * Note: This method is not used when configuring via configuration. It is primarily used by unit tests.
     * 
     * @param logger The Logger the Footer will be associated with.
     * @param filter The Filter.
     */
    @Override
    public synchronized void addLoggerFilter(final org.apache.logging.log4j.core.Logger logger, final Filter filter) {
        final String loggerName = logger.getName();
        final LoggerConfig lc = getLoggerConfig(loggerName);
        if (lc.getName().equals(loggerName)) {
            lc.addFilter(filter);
        } else {
            final LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), lc.isAdditive());
            nlc.addFilter(filter);
            nlc.setParent(lc);
            loggers.putIfAbsent(loggerName, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }

    /**
     * Marks a LoggerConfig as additive. This method is synchronized in case a Logger with the same name is being
     * updated at the same time.
     *
     * Note: This method is not used when configuring via configuration. It is primarily used by unit tests.
     * 
     * @param logger The Logger the Appender will be associated with.
     * @param additive True if the LoggerConfig should be additive, false otherwise.
     */
    @Override
    public synchronized void setLoggerAdditive(final org.apache.logging.log4j.core.Logger logger, final boolean additive) {
        final String loggerName = logger.getName();
        final LoggerConfig lc = getLoggerConfig(loggerName);
        if (lc.getName().equals(loggerName)) {
            lc.setAdditive(additive);
        } else {
            final LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), additive);
            nlc.setParent(lc);
            loggers.putIfAbsent(loggerName, nlc);
            setParents();
            logger.getContext().updateLoggers();
        }
    }

    /**
     * Remove an Appender. First removes any associations between LoggerConfigs and the Appender, removes the Appender
     * from this appender list and then stops the appender. This method is synchronized in case an Appender with the
     * same name is being added during the removal.
     * 
     * @param appenderName the name of the appender to remove.
     */
    public synchronized void removeAppender(final String appenderName) {
        for (final LoggerConfig logger : loggers.values()) {
            logger.removeAppender(appenderName);
        }
        final Appender app = appenders.remove(appenderName);

        if (app != null) {
            app.stop();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.logging.log4j.core.config.Configuration#getCustomLevels()
     */
    @Override
    public List<CustomLevelConfig> getCustomLevels() {
        return Collections.unmodifiableList(customLevels);
    }

    /**
     * Locates the appropriate LoggerConfig for a Logger name. This will remove tokens from the package name as
     * necessary or return the root LoggerConfig if no other matches were found.
     * 
     * @param loggerName The Logger name.
     * @return The located LoggerConfig.
     */
    @Override
    public LoggerConfig getLoggerConfig(final String loggerName) {
        LoggerConfig loggerConfig = loggers.get(loggerName);
        if (loggerConfig != null) {
            return loggerConfig;
        }
        String substr = loggerName;
        while ((substr = NameUtil.getSubName(substr)) != null) {
            loggerConfig = loggers.get(substr);
            if (loggerConfig != null) {
                return loggerConfig;
            }
        }
        return root;
    }

    /**
     * Returns the root Logger.
     * 
     * @return the root Logger.
     */
    @Override
    public LoggerConfig getRootLogger() {
        return root;
    }

    /**
     * Returns a Map of all the LoggerConfigs.
     * 
     * @return a Map with each entry containing the name of the Logger and the LoggerConfig.
     */
    @Override
    public Map<String, LoggerConfig> getLoggers() {
        return Collections.unmodifiableMap(loggers);
    }

    /**
     * Returns the LoggerConfig with the specified name.
     * 
     * @param loggerName The Logger name.
     * @return The LoggerConfig or null if no match was found.
     */
    public LoggerConfig getLogger(final String loggerName) {
        return loggers.get(loggerName);
    }

    /**
     * Add a loggerConfig. The LoggerConfig must already be configured with Appenders, Filters, etc. After addLogger is
     * called LoggerContext.updateLoggers must be called.
     *
     * @param loggerName The name of the Logger.
     * @param loggerConfig The LoggerConfig.
     */
    @Override
    public synchronized void addLogger(final String loggerName, final LoggerConfig loggerConfig) {
        loggers.putIfAbsent(loggerName, loggerConfig);
        setParents();
    }

    /**
     * Remove a LoggerConfig.
     *
     * @param loggerName The name of the Logger.
     */
    @Override
    public synchronized void removeLogger(final String loggerName) {
        loggers.remove(loggerName);
        setParents();
    }

    @Override
    public void createConfiguration(final Node node, final LogEvent event) {
        final PluginType<?> type = node.getType();
        if (type != null && type.isDeferChildren()) {
            node.setObject(createPluginObject(type, node, event));
        } else {
            for (final Node child : node.getChildren()) {
                createConfiguration(child, event);
            }

            if (type == null) {
                if (node.getParent() != null) {
                    LOGGER.error("Unable to locate plugin for {}", node.getName());
                }
            } else {
                node.setObject(createPluginObject(type, node, event));
            }
        }
    }

    /**
     * Invokes a static factory method to either create the desired object or to create a builder object that creates
     * the desired object. In the case of a factory method, it should be annotated with
     * {@link org.apache.logging.log4j.core.config.plugins.PluginFactory}, and each parameter should be annotated with
     * an appropriate plugin annotation depending on what that parameter describes. Parameters annotated with
     * {@link org.apache.logging.log4j.core.config.plugins.PluginAttribute} must be a type that can be converted from a
     * string using one of the {@link org.apache.logging.log4j.core.config.plugins.convert.TypeConverter TypeConverters}
     * . Parameters with {@link org.apache.logging.log4j.core.config.plugins.PluginElement} may be any plugin class or
     * an array of a plugin class. Collections and Maps are currently not supported, although the factory method that is
     * called can create these from an array.
     *
     * Plugins can also be created using a builder class that implements
     * {@link org.apache.logging.log4j.core.util.Builder}. In that case, a static method annotated with
     * {@link org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute} should create the builder class, and
     * the various fields in the builder class should be annotated similarly to the method parameters. However, instead
     * of using PluginAttribute, one should use
     * {@link org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute} where the default value can be
     * specified as the default field value instead of as an additional annotation parameter.
     *
     * In either case, there are also annotations for specifying a
     * {@link org.apache.logging.log4j.core.config.Configuration} (
     * {@link org.apache.logging.log4j.core.config.plugins.PluginConfiguration}) or a
     * {@link org.apache.logging.log4j.core.config.Node} (
     * {@link org.apache.logging.log4j.core.config.plugins.PluginNode}).
     *
     * Although the happy path works, more work still needs to be done to log incorrect parameters. These will generally
     * result in unhelpful InvocationTargetExceptions.
     *
     * @param type the type of plugin to create.
     * @param node the corresponding configuration node for this plugin to create.
     * @param event the LogEvent that spurred the creation of this plugin
     * @return the created plugin object or {@code null} if there was an error setting it up.
     * @see org.apache.logging.log4j.core.config.plugins.util.PluginBuilder
     * @see org.apache.logging.log4j.core.config.plugins.visitors.PluginVisitor
     * @see org.apache.logging.log4j.core.config.plugins.convert.TypeConverter
     */
    private Object createPluginObject(final PluginType<?> type, final Node node, final LogEvent event) {
        final Class<?> clazz = type.getPluginClass();

        if (Map.class.isAssignableFrom(clazz)) {
            try {
                return createPluginMap(node);
            } catch (final Exception e) {
                LOGGER.warn("Unable to create Map for {} of class {}", type.getElementName(), clazz, e);
            }
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            try {
                return createPluginCollection(node);
            } catch (final Exception e) {
                LOGGER.warn("Unable to create List for {} of class {}", type.getElementName(), clazz, e);
            }
        }

        return new PluginBuilder(type).withConfiguration(this).withConfigurationNode(node).forLogEvent(event).build();
    }

    private static Map<String, ?> createPluginMap(final Node node) {
        final Map<String, Object> map = new LinkedHashMap<>();
        for (final Node child : node.getChildren()) {
            final Object object = child.getObject();
            map.put(child.getName(), object);
        }
        return map;
    }

    private static Collection<?> createPluginCollection(final Node node) {
        final List<Node> children = node.getChildren();
        final Collection<Object> list = new ArrayList<>(children.size());
        for (final Node child : children) {
            final Object object = child.getObject();
            list.add(object);
        }
        return list;
    }

    private void setParents() {
        for (final Map.Entry<String, LoggerConfig> entry : loggers.entrySet()) {
            final LoggerConfig logger = entry.getValue();
            String key = entry.getKey();
            if (!key.isEmpty()) {
                final int i = key.lastIndexOf('.');
                if (i > 0) {
                    key = key.substring(0, i);
                    LoggerConfig parent = getLoggerConfig(key);
                    if (parent == null) {
                        parent = root;
                    }
                    logger.setParent(parent);
                } else {
                    logger.setParent(root);
                }
            }
        }
    }

    /**
     * Reads an InputStream using buffered reads into a byte array buffer. The given InputStream will remain open after
     * invocation of this method.
     *
     * @param is the InputStream to read into a byte array buffer.
     * @return a byte array of the InputStream contents.
     * @throws IOException if the {@code read} method of the provided InputStream throws this exception.
     */
    protected static byte[] toByteArray(final InputStream is) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        final byte[] data = new byte[BUF_SIZE];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }

}
