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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Version;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.async.AsyncLoggerConfig;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDisruptor;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactoryConfig;
import org.apache.logging.log4j.core.config.arbiters.Arbiter;
import org.apache.logging.log4j.core.config.arbiters.SelectArbiter;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.Log4jProperties;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.InterpolatorFactory;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.RuntimeStrSubstitutor;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.script.ScriptManagerFactory;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.core.util.Watcher;
import org.apache.logging.log4j.core.util.WatcherFactory;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.NameUtil;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.ServiceRegistry;

/**
 * The base Configuration. Many configuration implementations will extend this class.
 */
public abstract class AbstractConfiguration extends AbstractFilterable implements Configuration {

    /**
     * The root node of the configuration.
     */
    protected Node rootNode = new Node();

    /**
     * Listeners for configuration changes.
     */
    protected final List<ConfigurationListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Packages found in configuration "packages" attribute.
     */
    protected final List<String> pluginPackages = new ArrayList<>();

    /**
     * Core plugins.
     */
    protected PluginNamespace corePlugins;

    /**
     * Shutdown hook is enabled by default.
     */
    protected boolean isShutdownHookEnabled = true;

    /**
     * Shutdown timeout in milliseconds.
     */
    protected long shutdownTimeoutMillis;

    /**
     * The Script manager.
     */
    protected ScriptManager scriptManager;

    protected final Injector injector;
    /**
     * The Advertiser which exposes appender configurations to external systems.
     */
    private Advertiser advertiser = new DefaultAdvertiser();
    private Node advertiserNode;
    private Object advertisement;
    private String name;
    private ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<>();
    private ConcurrentMap<String, LoggerConfig> loggerConfigs = new ConcurrentHashMap<>();
    private List<CustomLevelConfig> customLevels = List.of();
    private final ConcurrentMap<String, String> properties = new ConcurrentHashMap<>();
    private final InterpolatorFactory interpolatorFactory;
    private final Interpolator tempLookup;
    private final StrSubstitutor runtimeStrSubstitutor;
    private final StrSubstitutor configurationStrSubstitutor;
    private LoggerConfig root = new LoggerConfig();
    private final ConcurrentMap<String, Object> componentMap = new ConcurrentHashMap<>();
    private final ConfigurationSource configurationSource;
    private final ConfigurationScheduler configurationScheduler;
    private final WatchManager watchManager;
    private AsyncLoggerConfigDisruptor asyncLoggerConfigDisruptor;
    private AsyncWaitStrategyFactory asyncWaitStrategyFactory;
    private final WeakReference<LoggerContext> loggerContext;

    /**
     * Constructor.
     */
    protected AbstractConfiguration(final LoggerContext loggerContext, final ConfigurationSource configurationSource) {
        this.loggerContext = new WeakReference<>(loggerContext);
        // The loggerContext is null for the NullConfiguration class.
        // this.loggerContext = new WeakReference(Objects.requireNonNull(loggerContext, "loggerContext is null"));
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource is null");
        if (loggerContext != null) {
            injector = loggerContext.getInjector();
        } else {
            // for NullConfiguration
            injector = DI.createInjector();
            injector.init();
        }
        componentMap.put(Configuration.CONTEXT_PROPERTIES, properties);
        interpolatorFactory = injector.getInstance(InterpolatorFactory.class);
        tempLookup = interpolatorFactory.newInterpolator(new PropertiesLookup(properties));
        tempLookup.setLoggerContext(loggerContext);
        runtimeStrSubstitutor = new RuntimeStrSubstitutor(tempLookup);
        configurationStrSubstitutor = new ConfigurationStrSubstitutor(runtimeStrSubstitutor);
        configurationScheduler = injector.getInstance(ConfigurationScheduler.class);
        watchManager = injector.getInstance(WatchManager.class);
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

    public void setScriptManager(final ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        injector.registerBinding(ScriptManager.KEY, this::getScriptManager);
    }

    public PluginNamespace getCorePlugins() {
        return corePlugins;
    }

    public void setCorePlugins(final PluginNamespace corePlugins) {
        this.corePlugins = corePlugins;
        injector.registerBinding(Core.PLUGIN_NAMESPACE_KEY, this::getCorePlugins);
    }

    @Override
    public WatchManager getWatchManager() {
        return watchManager;
    }

    @Override
    public ConfigurationScheduler getScheduler() {
        return configurationScheduler;
    }

    public Node getRootNode() {
        return rootNode;
    }

    @Override
    public AsyncLoggerConfigDelegate getAsyncLoggerConfigDelegate() {
        // lazily instantiate only when requested by AsyncLoggers:
        // loading AsyncLoggerConfigDisruptor requires LMAX Disruptor jar on classpath
        if (asyncLoggerConfigDisruptor == null) {
            asyncLoggerConfigDisruptor = new AsyncLoggerConfigDisruptor(asyncWaitStrategyFactory);
        }
        return asyncLoggerConfigDisruptor;
    }

    @Override
    public AsyncWaitStrategyFactory getAsyncWaitStrategyFactory() {
        return asyncWaitStrategyFactory;
    }

    /**
     * Initialize the configuration.
     */
    @Override
    public void initialize() {
        LOGGER.debug("{} initializing configuration {}", Version.getProductString(), this);
        injector.registerBinding(Configuration.KEY, () -> this);
        runtimeStrSubstitutor.setConfiguration(this);
        configurationStrSubstitutor.setConfiguration(this);
        initializeScriptManager();
        injector.registerBindingIfAbsent(Keys.PLUGIN_PACKAGES_KEY, this::getPluginPackages);
        corePlugins = injector.getInstance(Core.PLUGIN_NAMESPACE_KEY);
        final PluginNamespace levelPlugins = injector.getInstance(new @Namespace(Level.CATEGORY) Key<>() {});
        levelPlugins.forEach(type -> {
            final Class<?> pluginClass = type.getPluginClass();
            try {
                // Cause the class to be initialized if it isn't already.
                Class.forName(pluginClass.getName(), true, pluginClass.getClassLoader());
            } catch (final Exception e) {
                LOGGER.error("Unable to initialize {} due to {}", pluginClass.getName(), e.getClass()
                        .getSimpleName(), e);
            }
        });
        setup();
        setupAdvertisement();
        doConfigure();
        setState(State.INITIALIZED);
        LOGGER.debug("Configuration {} initialized", this);
    }

    private void initializeScriptManager() {
        try {
            ServiceRegistry.getInstance()
                    .getServices(ScriptManagerFactory.class, MethodHandles.lookup(), null)
                    .stream()
                    .findFirst()
                    .ifPresent(factory -> setScriptManager(factory.createScriptManager(this, getWatchManager())));
        } catch (final LinkageError | Exception e) {
            // LOG4J2-1920 ScriptEngineManager is not available in Android
            LOGGER.info("Cannot initialize scripting support because this JRE does not support it.", e);
        }
    }

    protected void initializeWatchers(final Reconfigurable reconfigurable, final ConfigurationSource configSource,
                 final int monitorIntervalSeconds) {
        if (configSource != null && (configSource.getFile() != null || configSource.getURL() != null)) {
            if (monitorIntervalSeconds > 0) {
                watchManager.setIntervalSeconds(monitorIntervalSeconds);
                if (configSource.getFile() != null) {
                    final Source cfgSource = new Source(configSource);
                    final long lastModified = configSource.getFile().lastModified();
                    final ConfigurationFileWatcher watcher = new ConfigurationFileWatcher(this, reconfigurable,
                            listeners, lastModified);
                    watchManager.watch(cfgSource, watcher);
                } else {
                    if (configSource.getURL() != null) {
                        monitorSource(reconfigurable, configSource);
                    }
                }
            } else if (watchManager.hasEventListeners() && configSource.getURL() != null && monitorIntervalSeconds >= 0) {
                monitorSource(reconfigurable, configSource);
            }
        }
    }

    private void monitorSource(final Reconfigurable reconfigurable, final ConfigurationSource configSource) {
        if (configSource.getLastModified() > 0) {
            final Source cfgSource = new Source(configSource);
            final Key<WatcherFactory> key = Key.forClass(WatcherFactory.class);
            injector.registerBindingIfAbsent(key, Lazy.lazy(() ->
                    new WatcherFactory(injector.getInstance(Watcher.PLUGIN_CATEGORY_KEY))));
            final Watcher watcher = injector.getInstance(key)
                    .newWatcher(cfgSource, this, reconfigurable, listeners, configSource.getLastModified());
            if (watcher != null) {
                watchManager.watch(cfgSource, watcher);
            }
        } else {
            LOGGER.info("{} does not support dynamic reconfiguration", configSource.getURI());
        }
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
        if (watchManager.getIntervalSeconds() >= 0) {
            watchManager.start();
        }
        if (hasAsyncLoggers()) {
            asyncLoggerConfigDisruptor.start();
        }
        final Set<LoggerConfig> alreadyStarted = new HashSet<>();
        for (final LoggerConfig logger : loggerConfigs.values()) {
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

    private boolean hasAsyncLoggers() {
        if (root instanceof AsyncLoggerConfig) {
            return true;
        }
        for (final LoggerConfig logger : loggerConfigs.values()) {
            if (logger instanceof AsyncLoggerConfig) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tear down the configuration.
     */
    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        this.setStopping();
        super.stop(timeout, timeUnit, false);
        LOGGER.trace("Stopping {}...", this);

        // Stop the components that are closest to the application first:
        // 1. Notify all LoggerConfigs' ReliabilityStrategy that the configuration will be stopped.
        // 2. Stop the LoggerConfig objects (this may stop nested Filters)
        // 3. Stop the AsyncLoggerConfigDelegate. This shuts down the AsyncLoggerConfig Disruptor
        //    and waits until all events in the RingBuffer have been processed.
        // 4. Stop all AsyncAppenders. This shuts down the associated thread and
        //    waits until all events in the queue have been processed. (With optional timeout.)
        // 5. Notify all LoggerConfigs' ReliabilityStrategy that appenders will be stopped.
        //    This guarantees that any event received by a LoggerConfig before reconfiguration
        //    are passed on to the Appenders before the Appenders are stopped.
        // 6. Stop the remaining running Appenders. (It should now be safe to do so.)
        // 7. Notify all LoggerConfigs that their Appenders can be cleaned up.

        for (final LoggerConfig loggerConfig : loggerConfigs.values()) {
            loggerConfig.getReliabilityStrategy().beforeStopConfiguration(this);
        }
        root.getReliabilityStrategy().beforeStopConfiguration(this);

        final String cls = getClass().getSimpleName();
        LOGGER.trace("{} notified {} ReliabilityStrategies that config will be stopped.", cls, loggerConfigs.size()
                + 1);

        if (!loggerConfigs.isEmpty()) {
            LOGGER.trace("{} stopping {} LoggerConfigs.", cls, loggerConfigs.size());
            for (final LoggerConfig logger : loggerConfigs.values()) {
                logger.stop(timeout, timeUnit);
            }
        }
        LOGGER.trace("{} stopping root LoggerConfig.", cls);
        if (!root.isStopped()) {
            root.stop(timeout, timeUnit);
        }

        if (hasAsyncLoggers()) {
            LOGGER.trace("{} stopping AsyncLoggerConfigDisruptor.", cls);
            asyncLoggerConfigDisruptor.stop(timeout, timeUnit);
        }

        LOGGER.trace("{} notifying ReliabilityStrategies that appenders will be stopped.", cls);
        for (final LoggerConfig loggerConfig : loggerConfigs.values()) {
            loggerConfig.getReliabilityStrategy().beforeStopAppenders();
        }
        root.getReliabilityStrategy().beforeStopAppenders();

        // Stop the appenders in reverse order in case they still have activity.
        final Appender[] array = appenders.values().toArray(Appender.EMPTY_ARRAY);
        final List<Appender> async = getAsyncAppenders(array);
        if (!async.isEmpty()) {
            // LOG4J2-511, LOG4J2-392 stop AsyncAppenders first
            LOGGER.trace("{} stopping {} AsyncAppenders.", cls, async.size());
            for (final Appender appender : async) {
                appender.stop(timeout, timeUnit);
            }
        }

        LOGGER.trace("{} stopping remaining Appenders.", cls);
        int appenderCount = 0;
        for (int i = array.length - 1; i >= 0; --i) {
            if (array[i].isStarted()) { // then stop remaining Appenders
                array[i].stop(timeout, timeUnit);
                appenderCount++;
            }
        }
        LOGGER.trace("{} stopped {} remaining Appenders.", cls, appenderCount);

        LOGGER.trace("{} cleaning Appenders from {} LoggerConfigs.", cls, loggerConfigs.size() + 1);
        for (final LoggerConfig loggerConfig : loggerConfigs.values()) {

            // LOG4J2-520, LOG4J2-392:
            // Important: do not clear appenders until after all AsyncLoggerConfigs
            // have been stopped! Stopping the last AsyncLoggerConfig will
            // shut down the disruptor and wait for all enqueued events to be processed.
            // Only *after this* the appenders can be cleared or events will be lost.
            loggerConfig.clearAppenders();
        }
        root.clearAppenders();

        if (watchManager.isStarted()) {
            watchManager.stop(timeout, timeUnit);
        }
        configurationScheduler.stop(timeout, timeUnit);

        if (advertiser != null && advertisement != null) {
            advertiser.unadvertise(advertisement);
        }
        setStopped();
        LOGGER.debug("Stopped {} OK", this);
        return true;
    }

    private List<Appender> getAsyncAppenders(final Appender[] all) {
        final List<Appender> result = new ArrayList<>();
        for (int i = all.length - 1; i >= 0; --i) {
            if (all[i] instanceof AsyncAppender) {
                result.add(all[i]);
            }
        }
        return result;
    }

    @Override
    public boolean isShutdownHookEnabled() {
        return isShutdownHookEnabled;
    }

    @Override
    public long getShutdownTimeoutMillis() {
        return shutdownTimeoutMillis;
    }

    public void setup() {
        // default does nothing, subclasses do work.
    }

    protected Level getDefaultStatus() {
        return injector.getInstance(Constants.DEFAULT_STATUS_LEVEL_KEY);
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
            final PluginType<?> type = corePlugins.get(nodeName);
            if (type != null) {
                advertiser = injector.getInstance(type.getPluginClass().asSubclass(Advertiser.class));
                advertisement = advertiser.advertise(advertiserNode.getAttributes());
            }
        }
    }

    @Override
    public <T> T getComponent(final String componentName) {
        return Cast.cast(componentMap.get(componentName));
    }

    @Override
    public <T> Supplier<T> getFactory(final Key<T> key) {
        return injector.getFactory(key);
    }

    @Override
    public void addComponent(final String componentName, final Object obj) {
        componentMap.putIfAbsent(componentName, obj);
    }

    protected void preConfigure(final Node node) {
        try {
            for (final Node child : node.getChildren()) {
                if (child.getType() == null) {
                    LOGGER.error("Unable to locate plugin type for {}", child.getName());
                    continue;
                }
                final Class<?> clazz = child.getType().getPluginClass();
                if (clazz.isAnnotationPresent(Scheduled.class)) {
                    configurationScheduler.incrementScheduledItems();
                }
                preConfigure(child);
            }
        } catch (final Exception ex) {
            LOGGER.error("Error capturing node data for node {}", node.getName(), ex);
        }
    }


    /**
     * Process conditions by evaluating them and including the children of conditions that are true
     * and discarding those that are not.
     * @param node The node to evaluate.
     */
    protected void processConditionals(final Node node) {
        try {
            final List<Node> addList = new ArrayList<>();
            final List<Node> removeList = new ArrayList<>();
            for (final Node child : node.getChildren()) {
                final PluginType<?> type = child.getType();
                if (type != null && Arbiter.ELEMENT_TYPE.equals(type.getElementType())) {
                    final Class<?> clazz = type.getPluginClass();
                    if (SelectArbiter.class.isAssignableFrom(clazz)) {
                        removeList.add(child);
                        addList.addAll(processSelect(child, type));
                    } else if (Arbiter.class.isAssignableFrom(clazz)) {
                        removeList.add(child);
                        try {
                            final Arbiter condition = injector.configure(child);
                            if (condition.isCondition()) {
                                addList.addAll(child.getChildren());
                                processConditionals(child);
                            }
                        } catch (final Exception inner) {
                            LOGGER.error("Exception processing {}: Ignoring and including children",
                                    type.getPluginClass());
                            processConditionals(child);
                        }
                    } else {
                        LOGGER.error("Encountered Condition Plugin that does not implement Condition: {}",
                                child.getName());
                        processConditionals(child);
                    }
                } else {
                    processConditionals(child);
                }
            }
            if (!removeList.isEmpty()) {
                final List<Node> children = node.getChildren();
                children.removeAll(removeList);
                children.addAll(addList);
                for (final Node grandChild : addList) {
                    grandChild.setParent(node);
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Error capturing node data for node {}", node.getName(), ex);
        }
    }

    /**
     * Handle Select nodes. This finds the first child condition that returns true and attaches its children
     * to the parent of the Select Node. Other Nodes are discarded.
     * @param selectNode The Select Node.
     * @param type The PluginType of the Select Node.
     * @return The list of Nodes to be added to the parent.
     */
    protected List<Node> processSelect(final Node selectNode, final PluginType<?> type) {
        final List<Node> addList = new ArrayList<>();
        final SelectArbiter select = injector.configure(selectNode);
        final List<Arbiter> conditions = new ArrayList<>();
        for (final Node child : selectNode.getChildren()) {
            final PluginType<?> nodeType = child.getType();
            if (nodeType != null) {
                if (Arbiter.class.isAssignableFrom(nodeType.getPluginClass())) {
                    final Arbiter condition = injector.configure(child);
                    conditions.add(condition);
                } else {
                    LOGGER.error("Invalid Node {} for Select. Must be a Condition",
                            child.getName());
                }
            } else {
                LOGGER.error("No PluginType for node {}", child.getName());
            }
        }
        final Arbiter condition = select.evaluateConditions(conditions);
        if (condition != null) {
            for (final Node child : selectNode.getChildren()) {
                if (condition == child.getObject()) {
                    addList.addAll(child.getChildren());
                    processConditionals(child);
                }
            }
        }
        return addList;
    }


    protected void doConfigure() {
        injector.registerBinding(Keys.SUBSTITUTOR_KEY, () -> configurationStrSubstitutor::replace);
        injector.registerBinding(LoggerContext.KEY, () -> loggerContext);
        processConditionals(rootNode);
        preConfigure(rootNode);
        configurationScheduler.start();
        if (rootNode.hasChildren() && rootNode.getChildren().get(0).getName().equalsIgnoreCase("Properties")) {
            final Node first = rootNode.getChildren().get(0);
            createConfiguration(first, null);
            if (first.getObject() != null) {
                StrLookup lookup = first.getObject();
                if (lookup instanceof LoggerContextAware) {
                    ((LoggerContextAware) lookup).setLoggerContext(loggerContext.get());
                }
                runtimeStrSubstitutor.setVariableResolver(lookup);
                configurationStrSubstitutor.setVariableResolver(lookup);
            }
        } else {
            final Map<String, String> map = this.getComponent(CONTEXT_PROPERTIES);
            final StrLookup lookup = map == null ? null : new PropertiesLookup(map);
            Interpolator interpolator = interpolatorFactory.newInterpolator(lookup);
            interpolator.setLoggerContext(loggerContext.get());
            runtimeStrSubstitutor.setVariableResolver(interpolator);
            configurationStrSubstitutor.setVariableResolver(interpolator);
        }

        boolean setLoggers = false;
        boolean setRoot = false;
        for (final Node child : rootNode.getChildren()) {
            if (child.getName().equalsIgnoreCase("Properties")) {
                if (tempLookup == runtimeStrSubstitutor.getVariableResolver()) {
                    LOGGER.error("Properties declaration must be the first element in the configuration");
                }
                continue;
            }
            createConfiguration(child, null);
            if (child.getObject() == null) {
                continue;
            }
            if (child.getName().equalsIgnoreCase("Scripts")) {
                if (scriptManager != null) {
                    scriptManager.addScripts(child);
                }
            } else if (child.getName().equalsIgnoreCase("Appenders")) {
                appenders = child.getObject();
            } else if (child.isInstanceOf(Filter.class)) {
                addFilter(child.getObject(Filter.class));
            } else if (child.getName().equalsIgnoreCase("Loggers")) {
                final Loggers l = child.getObject();
                loggerConfigs = l.getMap();
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
            } else if (child.isInstanceOf(AsyncWaitStrategyFactoryConfig.class)) {
                AsyncWaitStrategyFactoryConfig awsfc = child.getObject(AsyncWaitStrategyFactoryConfig.class);
                asyncWaitStrategyFactory = awsfc.createWaitStrategyFactory();
            } else {
                final List<String> expected = Arrays.asList("\"Appenders\"", "\"Loggers\"", "\"Properties\"",
                        "\"Scripts\"", "\"CustomLevels\"");
                LOGGER.error("Unknown object \"{}\" of type {} is ignored: try nesting it inside one of: {}.",
                        child.getName(), child.getObject().getClass().getName(), expected);
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

        for (final Map.Entry<String, LoggerConfig> entry : loggerConfigs.entrySet()) {
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

    protected void setToDefault() {
        // LOG4J2-1176 facilitate memory leak investigation
        setName(DefaultConfiguration.DEFAULT_NAME + "@" + Integer.toHexString(hashCode()));
        final Layout<? extends Serializable> layout = PatternLayout.newBuilder()
                .setPattern(DefaultConfiguration.DEFAULT_PATTERN)
                .setConfiguration(this)
                .build();
        final Appender appender = ConsoleAppender.createDefaultAppenderForLayout(layout);
        appender.start();
        addAppender(appender);
        final LoggerConfig rootLoggerConfig = getRootLogger();
        rootLoggerConfig.addAppender(appender, null, null);

        final Level defaultLevel = Level.ERROR;
        final String levelName = PropertiesUtil.getProperties().getStringProperty(Log4jProperties.CONFIG_DEFAULT_LEVEL,
                defaultLevel.name());
        final Level level = Level.valueOf(levelName);
        rootLoggerConfig.setLevel(level != null ? level : defaultLevel);
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
    public <T extends Appender> T getAppender(final String appenderName) {
        return appenderName != null ? Cast.cast(appenders.get(appenderName)) : null;
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
        if (appender != null) {
            appenders.putIfAbsent(appender.getName(), appender);
        }
    }

    @Override
    public StrSubstitutor getStrSubstitutor() {
        return runtimeStrSubstitutor;
    }

    @Override
    public StrSubstitutor getConfigurationStrSubstitutor() {
        return configurationStrSubstitutor;
    }

    @Override
    public void setAdvertiser(final Advertiser advertiser) {
        this.advertiser = advertiser;
    }

    @Override
    public Advertiser getAdvertiser() {
        return advertiser;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.logging.log4j.core.config.ReliabilityStrategyFactory#getReliabilityStrategy(org.apache.logging.log4j
     * .core.config.LoggerConfig)
     */
    @Override
    public ReliabilityStrategy getReliabilityStrategy(final LoggerConfig loggerConfig) {
        return ReliabilityStrategyFactory.getReliabilityStrategy(loggerConfig);
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
        if (appender == null || logger == null) {
            return;
        }
        final String loggerName = logger.getName();
        appenders.putIfAbsent(appender.getName(), appender);
        final LoggerConfig lc = getLoggerConfig(loggerName);
        if (lc.getName().equals(loggerName)) {
            lc.addAppender(appender, null, null);
        } else {
            final LoggerConfig nlc = new LoggerConfig(loggerName, lc.getLevel(), lc.isAdditive());
            nlc.addAppender(appender, null, null);
            nlc.setParent(lc);
            loggerConfigs.putIfAbsent(loggerName, nlc);
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
            loggerConfigs.putIfAbsent(loggerName, nlc);
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
            loggerConfigs.putIfAbsent(loggerName, nlc);
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
        for (final LoggerConfig logger : loggerConfigs.values()) {
            logger.removeAppender(appenderName);
        }
        final Appender app = appenderName != null ? appenders.remove(appenderName) : null;

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
        LoggerConfig loggerConfig = loggerConfigs.get(loggerName);
        if (loggerConfig != null) {
            return loggerConfig;
        }
        String substr = loggerName;
        while ((substr = NameUtil.getSubName(substr)) != null) {
            loggerConfig = loggerConfigs.get(substr);
            if (loggerConfig != null) {
                return loggerConfig;
            }
        }
        return root;
    }

    @Override
    public LoggerContext getLoggerContext() {
        return loggerContext.get();
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
        return Collections.unmodifiableMap(loggerConfigs);
    }

    /**
     * Returns the LoggerConfig with the specified name.
     *
     * @param loggerName The Logger name.
     * @return The LoggerConfig or null if no match was found.
     */
    public LoggerConfig getLogger(final String loggerName) {
        return loggerConfigs.get(loggerName);
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
        loggerConfigs.putIfAbsent(loggerName, loggerConfig);
        setParents();
    }

    /**
     * Remove a LoggerConfig.
     *
     * @param loggerName The name of the Logger.
     */
    @Override
    public synchronized void removeLogger(final String loggerName) {
        loggerConfigs.remove(loggerName);
        setParents();
    }

    @Override
    public void createConfiguration(final Node node, final LogEvent event) {
        final Function<String, String> stringSubstitutionStrategy;
        if (event == null) {
            stringSubstitutionStrategy = configurationStrSubstitutor::replace;
        } else {
            stringSubstitutionStrategy = str -> runtimeStrSubstitutor.replace(event, str);
        }
        final Injector injector = this.injector.copy().registerBinding(Keys.SUBSTITUTOR_KEY, () -> stringSubstitutionStrategy);
        injector.configure(node);
    }

    /**
     * This method is used by Arbiters to create specific children.
     * @param node The Node.
     * @return The created object or null;
     */
    public Object createPluginObject(final Node node) {
        if (this.getState().equals(State.INITIALIZING)) {
            final Injector injector =
                    this.injector.copy().registerBinding(Keys.SUBSTITUTOR_KEY, () -> configurationStrSubstitutor::replace);
            return injector.configure(node);
        }
        LOGGER.warn("Plugin Object creation is not allowed after initialization");
        return null;
    }

    /**
     * This method is used by Arbiters to create specific children.
     * @param type The PluginType.
     * @param node The Node.
     * @return The created object or null;
     * @deprecated use {@link #createPluginObject(Node)}
     */
    @Deprecated
    public Object createPluginObject(final PluginType<?> type, final Node node) {
        return createPluginObject(node);
    }

    private void setParents() {
        for (final Map.Entry<String, LoggerConfig> entry : loggerConfigs.entrySet()) {
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
     * @deprecated use {@link InputStream#readAllBytes()}
     */
    @Deprecated(since = "3.0.0")
    protected static byte[] toByteArray(final InputStream is) throws IOException {
        return is.readAllBytes();
    }

    @Override
    public NanoClock getNanoClock() {
        return injector.getInstance(NanoClock.class);
    }

    @Override
    public void setNanoClock(final NanoClock nanoClock) {
        injector.registerBinding(NanoClock.KEY, () -> nanoClock);
    }
}
