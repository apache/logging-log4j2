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
package org.apache.logging.log4j.core.config.composite;

import static org.apache.logging.log4j.util.Strings.toRootUpperCase;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Patterns;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.core.util.Watcher;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * A Composite Configuration.
 */
public class CompositeConfiguration extends AbstractConfiguration implements Reconfigurable {

    /**
     * Allow the ConfigurationFactory class to be specified as a system property.
     */
    public static final String MERGE_STRATEGY_PROPERTY = "log4j.mergeStrategy";

    private final List<? extends AbstractConfiguration> configurations;

    private MergeStrategy mergeStrategy;

    /**
     * Construct the CompositeConfiguration.
     *
     * @param configurations The List of Configurations to merge.
     */
    public CompositeConfiguration(final List<? extends AbstractConfiguration> configurations) {
        super(configurations.get(0).getLoggerContext(), ConfigurationSource.COMPOSITE_SOURCE);
        rootNode = configurations.get(0).getRootNode();
        this.configurations = configurations;
        final String mergeStrategyClassName = PropertiesUtil.getProperties()
                .getStringProperty(MERGE_STRATEGY_PROPERTY, DefaultMergeStrategy.class.getName());
        try {
            mergeStrategy = Loader.newInstanceOf(mergeStrategyClassName);
        } catch (ClassNotFoundException
                | IllegalAccessException
                | NoSuchMethodException
                | InvocationTargetException
                | InstantiationException ex) {
            mergeStrategy = new DefaultMergeStrategy();
        }
        for (final AbstractConfiguration config : configurations) {
            mergeStrategy.mergeRootProperties(rootNode, config);
        }
        final StatusConfiguration statusConfig = new StatusConfiguration().withStatus(getDefaultStatus());
        for (final Map.Entry<String, String> entry : rootNode.getAttributes().entrySet()) {
            final String key = entry.getKey();
            final String value = getConfigurationStrSubstitutor().replace(entry.getValue());
            if ("status".equalsIgnoreCase(key)) {
                statusConfig.withStatus(toRootUpperCase(value));
            } else if ("dest".equalsIgnoreCase(key)) {
                statusConfig.withDestination(value);
            } else if ("shutdownHook".equalsIgnoreCase(key)) {
                isShutdownHookEnabled = !"disable".equalsIgnoreCase(value);
            } else if ("shutdownTimeout".equalsIgnoreCase(key)) {
                shutdownTimeoutMillis = Long.parseLong(value);
            } else if ("packages".equalsIgnoreCase(key)) {
                pluginPackages.addAll(Arrays.asList(value.split(Patterns.COMMA_SEPARATOR)));
            } else if ("name".equalsIgnoreCase(key)) {
                setName(value);
            }
        }
        statusConfig.initialize();
    }

    @Override
    public void setup() {
        final AbstractConfiguration targetConfiguration = configurations.get(0);
        staffChildConfiguration(targetConfiguration);
        final WatchManager watchManager = getWatchManager();
        final WatchManager targetWatchManager = targetConfiguration.getWatchManager();
        if (targetWatchManager.getIntervalSeconds() > 0) {
            watchManager.setIntervalSeconds(targetWatchManager.getIntervalSeconds());
            final Map<Source, Watcher> watchers = targetWatchManager.getConfigurationWatchers();
            for (final Map.Entry<Source, Watcher> entry : watchers.entrySet()) {
                watchManager.watch(
                        entry.getKey(),
                        entry.getValue()
                                .newWatcher(this, listeners, entry.getValue().getLastModified()));
            }
        }
        for (final AbstractConfiguration sourceConfiguration : configurations.subList(1, configurations.size())) {
            staffChildConfiguration(sourceConfiguration);
            final Node sourceRoot = sourceConfiguration.getRootNode();
            mergeStrategy.mergConfigurations(rootNode, sourceRoot, getPluginManager());
            if (LOGGER.isEnabled(Level.ALL)) {
                final StringBuilder sb = new StringBuilder();
                printNodes("", rootNode, sb);
                System.out.println(sb.toString());
            }
            final int monitorInterval = sourceConfiguration.getWatchManager().getIntervalSeconds();
            if (monitorInterval > 0) {
                final int currentInterval = watchManager.getIntervalSeconds();
                if (currentInterval <= 0 || monitorInterval < currentInterval) {
                    watchManager.setIntervalSeconds(monitorInterval);
                }
                final WatchManager sourceWatchManager = sourceConfiguration.getWatchManager();
                final Map<Source, Watcher> watchers = sourceWatchManager.getConfigurationWatchers();
                for (final Map.Entry<Source, Watcher> entry : watchers.entrySet()) {
                    watchManager.watch(
                            entry.getKey(),
                            entry.getValue()
                                    .newWatcher(
                                            this, listeners, entry.getValue().getLastModified()));
                }
            }
        }
    }

    @Override
    public Configuration reconfigure() {
        LOGGER.debug("Reconfiguring composite configuration");
        final List<AbstractConfiguration> configs = new ArrayList<>();
        final ConfigurationFactory factory = ConfigurationFactory.getInstance();
        for (final AbstractConfiguration config : configurations) {
            final ConfigurationSource source = config.getConfigurationSource();
            final URI sourceURI = source.getURI();
            Configuration currentConfig = config;
            if (sourceURI == null) {
                LOGGER.warn(
                        "Unable to determine URI for configuration {}, changes to it will be ignored",
                        config.getName());
            } else {
                currentConfig = factory.getConfiguration(getLoggerContext(), config.getName(), sourceURI);
                if (currentConfig == null) {
                    LOGGER.warn("Unable to reload configuration {}, changes to it will be ignored", config.getName());
                }
            }
            configs.add((AbstractConfiguration) currentConfig);
        }

        return new CompositeConfiguration(configs);
    }

    private void staffChildConfiguration(final AbstractConfiguration childConfiguration) {
        childConfiguration.setPluginManager(pluginManager);
        childConfiguration.setScriptManager(scriptManager);
        childConfiguration.setup();
    }

    private void printNodes(final String indent, final Node node, final StringBuilder sb) {
        sb.append(indent)
                .append(node.getName())
                .append(" type: ")
                .append(node.getType())
                .append("\n");
        sb.append(indent).append(node.getAttributes().toString()).append("\n");
        for (final Node child : node.getChildren()) {
            printNodes(indent + "  ", child, sb);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + " [configurations=" + configurations
                + ", mergeStrategy=" + mergeStrategy + ", rootNode=" + rootNode + ", listeners=" + listeners
                + ", pluginPackages=" + pluginPackages + ", pluginManager=" + pluginManager + ", isShutdownHookEnabled="
                + isShutdownHookEnabled + ", shutdownTimeoutMillis=" + shutdownTimeoutMillis + ", scriptManager="
                + scriptManager + "]";
    }
}
