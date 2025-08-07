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

import aQute.bnd.annotation.Cardinality;
import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;
import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Version;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.arbiters.Arbiter;
import org.apache.logging.log4j.core.config.arbiters.SelectArbiter;
import org.apache.logging.log4j.core.filter.AbstractFilterable;
import org.apache.logging.log4j.core.impl.CoreProperties.ConfigurationProperties;
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
import org.apache.logging.log4j.kit.env.PropertyEnvironment;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.spi.ConfigurableInstanceFactoryPostProcessor;
import org.apache.logging.log4j.plugins.di.spi.StringValueResolver;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.util.OrderedComparator;
import org.apache.logging.log4j.util.Cast;
import org.apache.logging.log4j.util.Lazy;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * The base Configuration. Many configuration implementations will extend this class.
 */
@NullMarked
@ServiceConsumer(value = ScriptManagerFactory.class, cardinality = Cardinality.SINGLE, resolution = Resolution.OPTIONAL)
@ServiceConsumer(value = ConfigurableInstanceFactoryPostProcessor.class, cardinality = Cardinality.MULTIPLE)
public abstract class AbstractConfiguration extends AbstractFilterable implements Configuration {

    private static final List<String> EXPECTED_ELEMENTS =
            List.of("\"Appenders\"", "\"Loggers\"", "\"Properties\"", "\"Scripts\"", "\"CustomLevels\"");

    /**
     * The instance factory for this configuration. This may be a child factory to a LoggerContext
     * in most cases, though this might be a root level factory for null configurations.
     */
    protected final ConfigurableInstanceFactory instanceFactory;

    /**
     * The configuration processor for transforming a node tree into plugin instances.
     */
    protected final ConfigurationProcessor configurationProcessor;

    /**
     * The root node of the configuration.
     */
    protected Node rootNode = new Node();

    /**
     * Listeners for configuration changes.
     */
    protected final List<Consumer<Reconfigurable>> listeners = new CopyOnWriteArrayList<>();

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
    // TODO(ms): consider initializing these final fields via injectMembers (and make non-final)
    private final InterpolatorFactory interpolatorFactory;
    private final Interpolator tempLookup;
    private final StrSubstitutor runtimeStrSubstitutor;
    private final StrSubstitutor configurationStrSubstitutor;
    private LoggerConfig root;
    private final ConcurrentMap<String, Object> componentMap = new ConcurrentHashMap<>();
    private final ConfigurationSource configurationSource;
    private final ConfigurationScheduler configurationScheduler;
    private final WatchManager watchManager;
    private final WeakReference<LoggerContext> loggerContext;
    private final PropertyEnvironment environment;
    private final Lock configLock = new ReentrantLock();
    private final List<ConfigurationExtension> extensions = new CopyOnWriteArrayList<>();

    /**
     * Constructor.
     */
    protected AbstractConfiguration(final LoggerContext loggerContext, final ConfigurationSource configurationSource) {
        this(
                Objects.requireNonNull(loggerContext),
                configurationSource,
                loggerContext.getEnvironment(),
                (ConfigurableInstanceFactory) loggerContext.getInstanceFactory());
    }

    protected AbstractConfiguration(
            final @Nullable LoggerContext loggerContext,
            final ConfigurationSource configurationSource,
            final PropertyEnvironment environment,
            final ConfigurableInstanceFactory parentInstanceFactory) {
        this.loggerContext = new WeakReference<>(loggerContext);
        this.configurationSource = Objects.requireNonNull(configurationSource, "configurationSource is null");
        // The scheduler is shared by all configurations
        this.configurationScheduler = parentInstanceFactory.getInstance(ConfigurationScheduler.class);
        this.environment = environment;
        this.instanceFactory = parentInstanceFactory.newChildInstanceFactory();
        this.watchManager = new WatchManager(configurationScheduler, LOGGER);

        configurationProcessor = new ConfigurationProcessor(instanceFactory);
        instanceFactory.registerBinding(Configuration.KEY, Lazy.weak(this));
        // Post-process the factory, after registering itself
        ServiceLoaderUtil.safeStream(
                        ConfigurableInstanceFactoryPostProcessor.class,
                        ServiceLoader.load(
                                ConfigurableInstanceFactoryPostProcessor.class, LoggerContext.class.getClassLoader()),
                        LOGGER)
                .sorted(Comparator.comparing(
                        ConfigurableInstanceFactoryPostProcessor::getClass, OrderedComparator.INSTANCE))
                .forEachOrdered(processor -> processor.postProcessFactory(instanceFactory));

        instanceFactory.registerExtension(new ConfigurationAwarePostProcessor(Lazy.weak(this)));
        componentMap.put(Configuration.CONTEXT_PROPERTIES, properties);
        interpolatorFactory = instanceFactory.getInstance(InterpolatorFactory.class);
        tempLookup = interpolatorFactory.newInterpolator(new PropertiesLookup(properties));
        instanceFactory.injectMembers(tempLookup);
        runtimeStrSubstitutor = new RuntimeStrSubstitutor(tempLookup);
        configurationStrSubstitutor = new ConfigurationStrSubstitutor(runtimeStrSubstitutor);
        // Root logger
        root = new LoggerConfig(Strings.EMPTY, Level.ERROR, true, this);
        setState(State.INITIALIZING);
    }

    @Override
    public ConfigurationSource getConfigurationSource() {
        return configurationSource;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    protected ConfigurableInstanceFactory getInstanceFactory() {
        return instanceFactory;
    }

    @Override
    public PropertyEnvironment getEnvironment() {
        return environment;
    }

    @Override
    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    @Inject // TODO(ms): consider injecting here
    public void setScriptManager(final ScriptManager scriptManager) {
        this.scriptManager = scriptManager;
        instanceFactory.registerBinding(ScriptManager.KEY, this::getScriptManager);
    }

    public PluginNamespace getCorePlugins() {
        return corePlugins;
    }

    @Inject // TODO(ms): consider injecting here
    public void setCorePlugins(@Namespace(Node.CORE_NAMESPACE) final PluginNamespace corePlugins) {
        this.corePlugins = corePlugins;
        instanceFactory.registerBinding(Core.PLUGIN_NAMESPACE_KEY, this::getCorePlugins);
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

    /**
     * Initialize the configuration.
     */
    @Override
    public void initialize() {
        LOGGER.debug("{} initializing configuration {}", Version.getProductString(), this);
        runtimeStrSubstitutor.setConfiguration(this);
        configurationStrSubstitutor.setConfiguration(this);
        initializeScriptManager();
        // TODO(ms): this should use injectMembers()
        corePlugins = instanceFactory.getInstance(Core.PLUGIN_NAMESPACE_KEY);
        final PluginNamespace levelPlugins = instanceFactory.getInstance(new @Namespace(Level.CATEGORY) Key<>() {});
        levelPlugins.forEach(type -> {
            final Class<?> pluginClass = type.getPluginClass();
            try {
                // Cause the class to be initialized if it isn't already.
                Class.forName(pluginClass.getName(), true, pluginClass.getClassLoader());
            } catch (final Exception e) {
                LOGGER.error(
                        "Unable to initialize {} due to {}",
                        pluginClass.getName(),
                        e.getClass().getSimpleName(),
                        e);
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
            ServiceLoaderUtil.safeStream(
                            ScriptManagerFactory.class,
                            ServiceLoader.load(
                                    ScriptManagerFactory.class, AbstractConfiguration.class.getClassLoader()),
                            LOGGER)
                    .findFirst()
                    .ifPresent(factory -> setScriptManager(factory.createScriptManager(this, getWatchManager())));
        } catch (final LinkageError | Exception e) {
            // LOG4J2-1920 ScriptEngineManager is not available in Android
            LOGGER.info("Cannot initialize scripting support because this JRE does not support it.", e);
        }
    }

    protected void initializeWatchers(
            final Reconfigurable reconfigurable,
            final ConfigurationSource configSource,
            final int monitorIntervalSeconds) {
        if (configSource != null && (configSource.getFile() != null || configSource.getURL() != null)) {
            if (monitorIntervalSeconds > 0) {
                watchManager.setIntervalSeconds(monitorIntervalSeconds);
                File file = configSource.getFile();
                if (file != null) {
                    final Source cfgSource = new Source(file);
                    final long lastModified = file.lastModified();
                    final ConfigurationFileWatcher watcher =
                            new ConfigurationFileWatcher(this, reconfigurable, listeners, lastModified);
                    watchManager.watch(cfgSource, watcher);
                } else {
                    if (configSource.getURL() != null) {
                        monitorSource(reconfigurable, configSource);
                    }
                }
            } else if (watchManager.hasEventListeners()
                    && configSource.getURL() != null
                    && monitorIntervalSeconds >= 0) {
                monitorSource(reconfigurable, configSource);
            }
        }
    }

    private void monitorSource(final Reconfigurable reconfigurable, final ConfigurationSource configSource) {
        URI uri = configSource.getURI();
        if (uri != null && configSource.getLastModified() > 0) {
            File file = configSource.getFile();
            final Source cfgSource = file != null ? new Source(file) : new Source(uri);
            final Watcher watcher = instanceFactory
                    .getInstance(WatcherFactory.class)
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
        if (getState() == State.INITIALIZING) {
            initialize();
        }
        LOGGER.info("Starting configuration {}...", this);
        this.setStarting();
        if (watchManager.getIntervalSeconds() >= 0) {
            LOGGER.info(
                    "Start watching for changes to {} every {} seconds",
                    getConfigurationSource(),
                    watchManager.getIntervalSeconds());
            watchManager.start();
        }
        for (final ConfigurationExtension extension : extensions) {
            if (extension instanceof LifeCycle lifecycle) {
                lifecycle.start();
            }
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
        LOGGER.info("Configuration {} started.", this);
    }

    /**
     * Tear down the configuration.
     */
    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        LOGGER.info("Stopping configuration {}...", this);
        this.setStopping();
        super.stop(timeout, timeUnit, false);

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

        for (final ConfigurationExtension extension : extensions) {
            if (extension instanceof LifeCycle lifecycle) {
                lifecycle.stop(timeout, timeUnit);
            }
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
        LOGGER.info("Configuration {} stopped.", this);
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

    @SuppressWarnings("deprecation")
    protected Level getDefaultStatus() {
        return instanceFactory.getInstance(Constants.STATUS_LOGGER_LEVEL_KEY);
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
            final PluginType<?> type = corePlugins.get(nodeName);
            if (type != null) {
                advertiser = instanceFactory.getInstance(type.getPluginClass().asSubclass(Advertiser.class));
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
        return instanceFactory.getFactory(key);
    }

    @Override
    public <T> void setComponent(final Key<T> key, final Supplier<? extends T> supplier) {
        instanceFactory.registerBinding(key, supplier);
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
                            final Arbiter condition = configurationProcessor.processNodeTree(child);
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
        final SelectArbiter select = configurationProcessor.processNodeTree(selectNode);
        final List<Arbiter> conditions = new ArrayList<>();
        for (final Node child : selectNode.getChildren()) {
            final PluginType<?> nodeType = child.getType();
            if (nodeType != null) {
                if (Arbiter.class.isAssignableFrom(nodeType.getPluginClass())) {
                    final Arbiter condition = configurationProcessor.processNodeTree(child);
                    conditions.add(condition);
                } else {
                    LOGGER.error("Invalid Node {} for Select. Must be a Condition", child.getName());
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
        instanceFactory.registerBinding(StringValueResolver.KEY, this::getConfigurationStrSubstitutor);
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
            final Interpolator interpolator = interpolatorFactory.newInterpolator(lookup);
            instanceFactory.injectMembers(interpolator);
            runtimeStrSubstitutor.setVariableResolver(interpolator);
            configurationStrSubstitutor.setVariableResolver(interpolator);
        }

        boolean setLoggers = false;
        boolean setRoot = false;
        for (final Node child : rootNode.getChildren()) {
            if ("Properties".equalsIgnoreCase(child.getName())) {
                if (tempLookup == runtimeStrSubstitutor.getVariableResolver()) {
                    LOGGER.error("Properties declaration must be the first element in the configuration");
                }
                continue;
            }
            createConfiguration(child, null);
            if (child.getObject() == null) {
                LOGGER.warn(
                        "Configuration element \"{}\" is ignored: try nesting it inside one of: {}.",
                        child.getName(),
                        EXPECTED_ELEMENTS);
                continue;
            }
            if ("Scripts".equalsIgnoreCase(child.getName())) {
                if (scriptManager != null) {
                    scriptManager.addScripts(child);
                }
            } else if ("Appenders".equalsIgnoreCase(child.getName())) {
                appenders = child.getObject();
            } else if (child.isInstanceOf(Filter.class)) {
                addFilter(child.getObject(Filter.class));
            } else if (child.isInstanceOf(Loggers.class)) {
                final Loggers l = child.getObject(Loggers.class);
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
            } else if (child.isInstanceOf(ConfigurationExtension.class)) {
                addExtension(child.getObject(ConfigurationExtension.class));
            } else {
                LOGGER.error(
                        "Unknown object \"{}\" of type {} is ignored: try nesting it inside one of: {}.",
                        child.getName(),
                        child.getObject().getClass().getName(),
                        EXPECTED_ELEMENTS);
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
            loggerConfig.initialize();
        }

        setParents();
    }

    protected void setToDefault() {
        // LOG4J2-1176 facilitate memory leak investigation
        setName(DefaultConfiguration.DEFAULT_NAME + "@" + Integer.toHexString(hashCode()));
        final Appender appender = ConsoleAppender.createDefaultAppenderForLayout(DefaultLayout.INSTANCE);
        appender.start();
        addAppender(appender);
        final LoggerConfig rootLoggerConfig = getRootLogger();
        rootLoggerConfig.addAppender(appender, null, null);
        final Level defaultLevel =
                environment.getProperty(ConfigurationProperties.class).level();
        rootLoggerConfig.setLevel(defaultLevel != null ? defaultLevel : Level.ERROR);
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
     * Add a listener for changes to the source of this configuration.
     *
     * @param listener The listener to add.
     */
    @Override
    public void addListener(final Consumer<Reconfigurable> listener) {
        listeners.add(listener);
    }

    /**
     * Remove an existing listener for configuration source updates.
     *
     * @param listener The listener to remove.
     */
    @Override
    public void removeListener(final Consumer<Reconfigurable> listener) {
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
        final String strategy =
                getEnvironment().getProperty(ConfigurationProperties.class).reliabilityStrategy();
        return ReliabilityStrategyFactory.getReliabilityStrategy(loggerConfig, strategy);
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
    public void addLoggerAppender(final org.apache.logging.log4j.core.Logger logger, final Appender appender) {
        if (appender == null || logger == null) {
            return;
        }
        configLock.lock();
        try {
            final String loggerName = logger.getName();
            appenders.putIfAbsent(appender.getName(), appender);
            final LoggerConfig lc = getLoggerConfig(loggerName);
            if (lc.getName().equals(loggerName)) {
                lc.addAppender(appender, null, null);
            } else {
                final Level level = lc.getLevel();
                final boolean additivity = lc.isAdditive();
                final LoggerConfig nlc = new LoggerConfig(loggerName, level, additivity, this);
                nlc.addAppender(appender, null, null);
                nlc.setParent(lc);
                loggerConfigs.putIfAbsent(loggerName, nlc);
                setParents();
                logger.getContext().updateLoggers();
            }
        } finally {
            configLock.unlock();
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
    public void addLoggerFilter(final org.apache.logging.log4j.core.Logger logger, final Filter filter) {
        configLock.lock();
        try {
            final String loggerName = logger.getName();
            final LoggerConfig lc = getLoggerConfig(loggerName);
            if (lc.getName().equals(loggerName)) {
                lc.addFilter(filter);
            } else {
                final Level level = lc.getLevel();
                final boolean additivity = lc.isAdditive();
                final LoggerConfig nlc = new LoggerConfig(loggerName, level, additivity, this);
                nlc.addFilter(filter);
                nlc.setParent(lc);
                loggerConfigs.putIfAbsent(loggerName, nlc);
                setParents();
                logger.getContext().updateLoggers();
            }
        } finally {
            configLock.unlock();
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
    public void setLoggerAdditive(final org.apache.logging.log4j.core.Logger logger, final boolean additive) {
        configLock.lock();
        try {
            final String loggerName = logger.getName();
            final LoggerConfig lc = getLoggerConfig(loggerName);
            if (lc.getName().equals(loggerName)) {
                lc.setAdditive(additive);
            } else {
                final Level level = lc.getLevel();
                final LoggerConfig nlc = new LoggerConfig(loggerName, level, additive, this);
                nlc.setParent(lc);
                loggerConfigs.putIfAbsent(loggerName, nlc);
                setParents();
                logger.getContext().updateLoggers();
            }
        } finally {
            configLock.unlock();
        }
    }

    /**
     * Remove an Appender. First removes any associations between LoggerConfigs and the Appender, removes the Appender
     * from this appender list and then stops the appender. This method is synchronized in case an Appender with the
     * same name is being added during the removal.
     *
     * @param appenderName the name of the appender to remove.
     */
    public void removeAppender(final String appenderName) {
        configLock.lock();
        try {
            for (final LoggerConfig logger : loggerConfigs.values()) {
                logger.removeAppender(appenderName);
            }
            final Appender app = appenderName != null ? appenders.remove(appenderName) : null;

            if (app != null) {
                app.stop();
            }
        } finally {
            configLock.unlock();
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
        while ((substr = getSubName(substr)) != null) {
            loggerConfig = loggerConfigs.get(substr);
            if (loggerConfig != null) {
                return loggerConfig;
            }
        }
        return root;
    }

    private static @Nullable String getSubName(final String name) {
        if (Strings.isEmpty(name)) {
            return null;
        }
        final int i = name.lastIndexOf('.');
        return i > 0 ? name.substring(0, i) : Strings.EMPTY;
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
    public void addLogger(final String loggerName, final LoggerConfig loggerConfig) {
        configLock.lock();
        try {
            loggerConfigs.putIfAbsent(loggerName, loggerConfig);
            setParents();
        } finally {
            configLock.unlock();
        }
    }

    /**
     * Remove a LoggerConfig.
     *
     * @param loggerName The name of the Logger.
     */
    @Override
    public void removeLogger(final String loggerName) {
        configLock.lock();
        try {
            loggerConfigs.remove(loggerName);
            setParents();
        } finally {
            configLock.unlock();
        }
    }

    @Override
    public void createConfiguration(final Node node, final LogEvent event) {
        final StringValueResolver stringSubstitutionStrategy;
        if (event == null) {
            stringSubstitutionStrategy = configurationStrSubstitutor;
        } else {
            stringSubstitutionStrategy = str -> runtimeStrSubstitutor.replace(event, str);
        }
        instanceFactory.registerBinding(StringValueResolver.KEY, () -> stringSubstitutionStrategy);
        try {
            configurationProcessor.processNodeTree(node);
        } finally {
            instanceFactory.removeBinding(StringValueResolver.KEY);
        }
    }

    /**
     * This method is used by Arbiters to create specific children.
     * @param node The Node.
     * @return The created object or null;
     */
    public Object createPluginObject(final Node node) {
        if (this.getState().equals(State.INITIALIZING)) {
            instanceFactory.registerBinding(StringValueResolver.KEY, this::getConfigurationStrSubstitutor);
            try {
                return configurationProcessor.processNodeTree(node);
            } finally {
                instanceFactory.removeBinding(StringValueResolver.KEY);
            }
        }
        LOGGER.warn("Plugin Object creation is not allowed after initialization");
        return null;
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

    @Override
    public NanoClock getNanoClock() {
        return instanceFactory.getInstance(NanoClock.class);
    }

    @Override // TODO(ms): consider injecting here
    public void setNanoClock(final NanoClock nanoClock) {
        instanceFactory.registerBinding(NanoClock.KEY, () -> nanoClock);
    }

    @Override
    public <T extends ConfigurationExtension> T addExtensionIfAbsent(
            final Class<T> extensionType, final Supplier<? extends T> supplier) {
        for (final ConfigurationExtension extension : extensions) {
            if (extensionType.isInstance(extension)) {
                return extensionType.cast(extension);
            }
        }
        return addExtension(supplier.get());
    }

    private <T extends ConfigurationExtension> T addExtension(final T extension) {
        extensions.add(Objects.requireNonNull(extension));
        return extension;
    }

    @Override
    public <T extends ConfigurationExtension> T getExtension(final Class<T> extensionType) {
        T result = null;
        for (final ConfigurationExtension extension : extensions) {
            if (extensionType.isInstance(extension)) {
                if (result == null) {
                    result = extensionType.cast(extension);
                } else {
                    LOGGER.warn(
                            "Multiple configuration elements found for type {}. Only the first will be used.",
                            extensionType.getName());
                }
            }
        }
        return result;
    }
}
