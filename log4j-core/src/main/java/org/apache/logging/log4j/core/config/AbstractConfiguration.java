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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LifeCycle2;
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
import org.apache.logging.log4j.core.config.plugins.util.PluginBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginManager;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.lookup.ConfigurationStrSubstitutor;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.PropertiesLookup;
import org.apache.logging.log4j.core.lookup.RuntimeStrSubstitutor;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.script.ScriptRef;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.DummyNanoClock;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.NameUtil;
import org.apache.logging.log4j.core.util.NanoClock;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.core.util.Watcher;
import org.apache.logging.log4j.core.util.WatcherFactory;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * The base Configuration. Many configuration implementations will extend this class.
 */
public abstract class AbstractConfiguration extends AbstractFilterable implements Configuration {

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
     * Shutdown timeout in milliseconds.
     */
    protected long shutdownTimeoutMillis;

    /**
     * The Script manager.
     */
    protected ScriptManager scriptManager;

    /**
     * The Advertiser which exposes appender configurations to external systems.
     */
    private Advertiser advertiser = new DefaultAdvertiser();

    private Node advertiserNode;
    private Object advertisement;
    private String name;
    private ConcurrentMap<String, Appender> appenders = new ConcurrentHashMap<>();
    private ConcurrentMap<String, LoggerConfig> loggerConfigs = new ConcurrentHashMap<>();
    private List<CustomLevelConfig> customLevels = Collections.emptyList();
    private final ConcurrentMap<String, String> propertyMap = new ConcurrentHashMap<>();
    private final Interpolator tempLookup = new Interpolator(propertyMap);
    private final StrSubstitutor runtimeStrSubstitutor = new RuntimeStrSubstitutor(tempLookup);
    private final StrSubstitutor configurationStrSubstitutor = new ConfigurationStrSubstitutor(runtimeStrSubstitutor);
    private LoggerConfig root = new LoggerConfig();
    private final ConcurrentMap<String, Object> componentMap = new ConcurrentHashMap<>();
    private final ConfigurationSource configurationSource;
    private final ConfigurationScheduler configurationScheduler = new ConfigurationScheduler();
    private final WatchManager watchManager = new WatchManager(configurationScheduler);
    private AsyncLoggerConfigDisruptor asyncLoggerConfigDisruptor;
    private AsyncWaitStrategyFactory asyncWaitStrategyFactory;
    private NanoClock nanoClock = new DummyNanoClock();
    private final WeakReference<LoggerContext> loggerContext;

    /**
     * Constructor.
     */
    protected AbstractConfiguration(final LoggerContext loggerContext, final ConfigurationSource configurationSource) {
        this.loggerContext = new WeakReference<>(loggerContext);
        tempLookup.setLoggerContext(loggerContext);
        // The loggerContext is null for the NullConfiguration class.
        // this.loggerContext = new WeakReference(Objects.requireNonNull(loggerContext, "loggerContext is null"));
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource is null");
        componentMap.put(Configuration.CONTEXT_PROPERTIES, propertyMap);
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
        return propertyMap;
    }

    @Override
    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public void setScriptManager(final ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public void setPluginManager(final PluginManager pluginManager) {
        this.pluginManager = pluginManager;
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
        LOGGER.debug(Version.getProductString() + " initializing configuration {}", this);
        runtimeStrSubstitutor.setConfiguration(this);
        configurationStrSubstitutor.setConfiguration(this);
        final String scriptLanguages = PropertiesUtil.getProperties().getStringProperty(Constants.SCRIPT_LANGUAGES);
        if (scriptLanguages != null) {
            try {
                scriptManager = new ScriptManager(this, watchManager, scriptLanguages);
            } catch (final LinkageError | Exception e) {
                // LOG4J2-1920 ScriptEngineManager is not available in Android
                LOGGER.info("Cannot initialize scripting support because this JRE does not support it.", e);
            }
        }
        pluginManager.collectPlugins(pluginPackages);
        final PluginManager levelPlugins = new PluginManager(Level.CATEGORY);
        levelPlugins.collectPlugins(pluginPackages);
        final Map<String, PluginType<?>> plugins = levelPlugins.getPlugins();
        if (plugins != null) {
            for (final PluginType<?> type : plugins.values()) {
                try {
                    // Cause the class to be initialized if it isn't already.
                    Loader.initializeClass(
                            type.getPluginClass().getName(),
                            type.getPluginClass().getClassLoader());
                } catch (final Exception e) {
                    LOGGER.error(
                            "Unable to initialize {} due to {}",
                            type.getPluginClass().getName(),
                            e.getClass().getSimpleName(),
                            e);
                }
            }
        }
        setup();
        setupAdvertisement();
        doConfigure();
        setState(State.INITIALIZED);
        LOGGER.debug("Configuration {} initialized", this);
    }

    protected void initializeWatchers(
            final Reconfigurable reconfigurable,
            final ConfigurationSource configSource,
            final int monitorIntervalSeconds) {
        if (configSource != null && (configSource.getFile() != null || configSource.getURL() != null)) {
            if (monitorIntervalSeconds > 0) {
                watchManager.setIntervalSeconds(monitorIntervalSeconds);
                if (configSource.getFile() != null) {
                    final Source cfgSource = new Source(configSource);
                    final long lastModifeid = configSource.getFile().lastModified();
                    final ConfigurationFileWatcher watcher =
                            new ConfigurationFileWatcher(this, reconfigurable, listeners, lastModifeid);
                    watchManager.watch(cfgSource, watcher);
                } else if (configSource.getURL() != null) {
                    monitorSource(reconfigurable, configSource);
                }
            } else if (watchManager.hasEventListeners()
                    && configSource.getURL() != null
                    && monitorIntervalSeconds >= 0) {
                monitorSource(reconfigurable, configSource);
            }
        }
    }

    private void monitorSource(final Reconfigurable reconfigurable, final ConfigurationSource configSource) {
        if (configSource.getLastModified() > 0) {
            final Source cfgSource = new Source(configSource);
            final Watcher watcher = WatcherFactory.getInstance(pluginPackages)
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
        LOGGER.trace(
                "{} notified {} ReliabilityStrategies that config will be stopped.", cls, loggerConfigs.size() + 1);

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
                if (appender instanceof LifeCycle2) {
                    ((LifeCycle2) appender).stop(timeout, timeUnit);
                } else {
                    appender.stop();
                }
            }
        }

        LOGGER.trace("{} stopping remaining Appenders.", cls);
        int appenderCount = 0;
        for (int i = array.length - 1; i >= 0; --i) {
            if (array[i].isStarted()) { // then stop remaining Appenders
                if (array[i] instanceof LifeCycle2) {
                    ((LifeCycle2) array[i]).stop(timeout, timeUnit);
                } else {
                    array[i].stop();
                }
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
        final String statusLevel = PropertiesUtil.getProperties()
                .getStringProperty(Constants.LOG4J_DEFAULT_STATUS_LEVEL, Level.ERROR.name());
        try {
            return Level.toLevel(statusLevel);
        } catch (final Exception ex) {
            return Level.ERROR;
        }
    }

    protected void createAdvertiser(
            final String advertiserString,
            final ConfigurationSource configSource,
            final byte[] buffer,
            final String contentType) {
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
                    advertiser = LoaderUtil.newInstanceOf(clazz);
                    advertisement = advertiser.advertise(advertiserNode.getAttributes());
                } catch (final ReflectiveOperationException e) {
                    LOGGER.error(
                            "{} attempting to instantiate advertiser: {}",
                            e.getClass().getSimpleName(),
                            nodeName,
                            e);
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

    protected void preConfigure(final Node node) {
        try {
            for (final Node child : node.getChildren()) {
                if (child.getType() == null) {
                    LOGGER.error("Unable to locate plugin type for " + child.getName());
                    continue;
                }
                final Class<?> clazz = child.getType().getPluginClass();
                if (clazz.isAnnotationPresent(Scheduled.class)) {
                    configurationScheduler.incrementScheduledItems();
                }
                preConfigure(child);
            }
        } catch (final Exception ex) {
            LOGGER.error("Error capturing node data for node " + node.getName(), ex);
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
                if (type != null && Arbiter.ELEMENT_TYPE.equals(type.getElementName())) {
                    final Class<?> clazz = type.getPluginClass();
                    if (SelectArbiter.class.isAssignableFrom(clazz)) {
                        removeList.add(child);
                        addList.addAll(processSelect(child, type));
                    } else if (Arbiter.class.isAssignableFrom(clazz)) {
                        removeList.add(child);
                        try {
                            final Arbiter condition = (Arbiter) createPluginObject(type, child, null);
                            if (condition.isCondition()) {
                                addList.addAll(child.getChildren());
                                processConditionals(child);
                            }
                        } catch (final Exception inner) {
                            LOGGER.error(
                                    "Exception processing {}: Ignoring and including children", type.getPluginClass());
                            processConditionals(child);
                        }
                    } else {
                        LOGGER.error(
                                "Encountered Condition Plugin that does not implement Condition: {}", child.getName());
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
                for (Node grandChild : addList) {
                    grandChild.setParent(node);
                }
            }
        } catch (final Exception ex) {
            LOGGER.error("Error capturing node data for node " + node.getName(), ex);
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
        final SelectArbiter select = (SelectArbiter) createPluginObject(type, selectNode, null);
        final List<Arbiter> conditions = new ArrayList<>();
        for (Node child : selectNode.getChildren()) {
            final PluginType<?> nodeType = child.getType();
            if (nodeType != null) {
                if (Arbiter.class.isAssignableFrom(nodeType.getPluginClass())) {
                    final Arbiter condition = (Arbiter) createPluginObject(nodeType, child, null);
                    conditions.add(condition);
                    child.setObject(condition);
                } else {
                    LOGGER.error("Invalid Node {} for Select. Must be a Condition", child.getName());
                }
            } else {
                LOGGER.error("No PluginType for node {}", child.getName());
            }
        }
        final Arbiter condition = select.evaluateConditions(conditions);
        if (condition != null) {
            for (Node child : selectNode.getChildren()) {
                if (condition == child.getObject()) {
                    addList.addAll(child.getChildren());
                    processConditionals(child);
                }
            }
        }
        return addList;
    }

    protected void doConfigure() {
        processConditionals(rootNode);
        preConfigure(rootNode);
        configurationScheduler.start();
        // Find the "Properties" node first
        boolean hasProperties = false;
        for (final Node node : rootNode.getChildren()) {
            if ("Properties".equalsIgnoreCase(node.getName())) {
                hasProperties = true;
                createConfiguration(node, null);
                if (node.getObject() != null) {
                    final StrLookup lookup = node.getObject();
                    runtimeStrSubstitutor.setVariableResolver(lookup);
                    configurationStrSubstitutor.setVariableResolver(lookup);
                }
                break;
            }
        }
        if (!hasProperties) {
            final Map<String, String> map = this.getComponent(CONTEXT_PROPERTIES);
            final StrLookup lookup = map == null ? null : new PropertiesLookup(map);
            final Interpolator interpolator = new Interpolator(lookup, pluginPackages);
            interpolator.setConfiguration(this);
            interpolator.setLoggerContext(loggerContext.get());
            runtimeStrSubstitutor.setVariableResolver(interpolator);
            configurationStrSubstitutor.setVariableResolver(interpolator);
        }

        boolean setLoggers = false;
        boolean setRoot = false;
        for (final Node child : rootNode.getChildren()) {
            if ("Properties".equalsIgnoreCase(child.getName())) {
                // We already used this node
                continue;
            }
            createConfiguration(child, null);
            if (child.getObject() == null) {
                continue;
            }
            if ("Scripts".equalsIgnoreCase(child.getName())) {
                for (final AbstractScript script : child.getObject(AbstractScript[].class)) {
                    if (script instanceof ScriptRef) {
                        LOGGER.error(
                                "Script reference to {} not added. Scripts definition cannot contain script references",
                                script.getName());
                    } else if (scriptManager != null) {
                        scriptManager.addScript(script);
                    }
                }
            } else if ("Appenders".equalsIgnoreCase(child.getName())) {
                appenders = child.getObject();
            } else if (child.isInstanceOf(Filter.class)) {
                addFilter(child.getObject(Filter.class));
            } else if (child.isInstanceOf(Loggers.class)) {
                final Loggers l = child.getObject();
                loggerConfigs = l.getMap();
                setLoggers = true;
                if (l.getRoot() != null) {
                    root = l.getRoot();
                    setRoot = true;
                }
            } else if (child.isInstanceOf(CustomLevels.class)) {
                customLevels = child.getObject(CustomLevels.class).getCustomLevels();
            } else if (child.isInstanceOf(CustomLevelConfig.class)) {
                final List<CustomLevelConfig> copy = new ArrayList<>(customLevels);
                copy.add(child.getObject(CustomLevelConfig.class));
                customLevels = copy;
            } else if (child.isInstanceOf(AsyncWaitStrategyFactoryConfig.class)) {
                final AsyncWaitStrategyFactoryConfig awsfc = child.getObject(AsyncWaitStrategyFactoryConfig.class);
                asyncWaitStrategyFactory = awsfc.createWaitStrategyFactory();
            } else {
                final List<String> expected = Arrays.asList(
                        "\"Appenders\"", "\"Loggers\"", "\"Properties\"", "\"Scripts\"", "\"CustomLevels\"");
                LOGGER.error(
                        "Unknown object \"{}\" of type {} is ignored: try nesting it inside one of: {}.",
                        child.getName(),
                        child.getObject().getClass().getName(),
                        expected);
            }
        }

        if (!setLoggers) {
            LOGGER.warn("No Loggers were configured, using default. Is the Loggers element missing?");
            setToDefault();
            return;
        } else if (!setRoot) {
            LOGGER.warn(
                    "No Root logger was configured, creating default ERROR-level Root logger with Console appender");
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
                    LOGGER.error(
                            "Unable to locate appender \"{}\" for logger config \"{}\"", ref.getRef(), loggerConfig);
                }
            }
        }

        setParents();
    }

    public static Level getDefaultLevel() {
        final String levelName = PropertiesUtil.getProperties()
                .getStringProperty(DefaultConfiguration.DEFAULT_LEVEL, Level.ERROR.name());
        return Level.valueOf(levelName);
    }

    protected void setToDefault() {
        // LOG4J2-1176 facilitate memory leak investigation
        setName(DefaultConfiguration.DEFAULT_NAME + "@" + Integer.toHexString(hashCode()));
        final Layout<? extends Serializable> layout = PatternLayout.newBuilder()
                .withPattern(DefaultConfiguration.DEFAULT_PATTERN)
                .withConfiguration(this)
                .build();
        final Appender appender = ConsoleAppender.createDefaultAppenderForLayout(layout);
        appender.start();
        addAppender(appender);
        final LoggerConfig rootLoggerConfig = getRootLogger();
        rootLoggerConfig.addAppender(appender, null, null);

        rootLoggerConfig.setLevel(getDefaultLevel());
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
        return appenderName != null ? (T) appenders.get(appenderName) : null;
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
    public synchronized void addLoggerAppender(
            final org.apache.logging.log4j.core.Logger logger, final Appender appender) {
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
    public synchronized void setLoggerAdditive(
            final org.apache.logging.log4j.core.Logger logger, final boolean additive) {
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
     * This method is used by Arbiters to create specific children.
     * @param type The PluginType.
     * @param node The Node.
     * @return The created object or null;
     */
    public Object createPluginObject(final PluginType<?> type, final Node node) {
        if (this.getState().equals(State.INITIALIZING)) {
            return createPluginObject(type, node, null);
        } else {
            LOGGER.warn("Plugin Object creation is not allowed after initialization");
            return null;
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

        return new PluginBuilder(type)
                .withConfiguration(this)
                .withConfigurationNode(node)
                .forLogEvent(event)
                .build();
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

    @Override
    public NanoClock getNanoClock() {
        return nanoClock;
    }

    @Override
    public void setNanoClock(final NanoClock nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }
}
