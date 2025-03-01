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
package org.apache.logging.log4j.core.config.builder.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.plugins.util.PluginType;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.util.Patterns;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Configuration} implementation that is built using a {@link ConfigurationBuilder} implementaion.
 * <p>
 *   This class may be extended to enhance its functionality.
 * </p>
 *
 * @since 2.4
 */
public class BuiltConfiguration extends AbstractConfiguration {

    private static final String DEFAULT_CONTENT_TYPE = "text";

    private final StatusConfiguration statusConfig;
    private String contentType = DEFAULT_CONTENT_TYPE;

    protected @Nullable Component rootComponent;
    private @Nullable Component loggersComponent = null;
    private @Nullable Component appendersComponent = null;
    private @Nullable Component filtersComponent = null;
    private @Nullable Component propertiesComponent = null;
    private @Nullable Component customLevelsComponent = null;
    private @Nullable Component scriptsComponent;

    /**
     * Constructs a new built-configuration instance.
     * <p>
     *   <strong>Important:</strong> this constructor is invoked via reflection in {@link DefaultConfigurationBuilder}.
     *   Do not change its signature!
     * </p>
     * @param loggerContext the logger-context
     * @param source the configuration-source
     * @param rootComponent the root component to build the configuration's node tree from
     */
    public BuiltConfiguration(
            final @Nullable LoggerContext loggerContext,
            final @Nullable ConfigurationSource source,
            final Component rootComponent) {

        super(loggerContext, source);

        Objects.requireNonNull(rootComponent, "The 'rootComponent' argument must not be null.");

        statusConfig = new StatusConfiguration().withStatus(getDefaultStatus());

        for (final Component component : rootComponent.getComponents()) {
            switch (component.getPluginType()) {
                case "Scripts": {
                    scriptsComponent = component;
                    break;
                }
                case "Loggers": {
                    loggersComponent = component;
                    break;
                }
                case "Appenders": {
                    appendersComponent = component;
                    break;
                }
                case "Filters": {
                    filtersComponent = component;
                    break;
                }
                case "Properties": {
                    propertiesComponent = component;
                    break;
                }
                case "CustomLevels": {
                    customLevelsComponent = component;
                    break;
                }
                default: {
                    // NO-OP
                    break;
                }
            }
        }

        this.rootComponent = rootComponent;
    }

    /** {@inheritDoc} */
    @Override
    public void setup() {

        final List<Node> children = rootNode.getChildren();

        if (propertiesComponent != null && !propertiesComponent.getComponents().isEmpty()) {
            children.add(convertToNode(rootNode, propertiesComponent));
        }

        if (scriptsComponent != null && !scriptsComponent.getComponents().isEmpty()) {
            children.add(convertToNode(rootNode, scriptsComponent));
        }

        if (customLevelsComponent != null
                && !customLevelsComponent.getComponents().isEmpty()) {
            children.add(convertToNode(rootNode, customLevelsComponent));
        }

        if (loggersComponent != null && !loggersComponent.getComponents().isEmpty()) {
            children.add(convertToNode(rootNode, loggersComponent));
        }

        if (appendersComponent != null && !appendersComponent.getComponents().isEmpty()) {
            children.add(convertToNode(rootNode, appendersComponent));
        }

        if (filtersComponent != null && !filtersComponent.getComponents().isEmpty()) {
            if (filtersComponent.getComponents().size() == 1) {
                children.add(
                        convertToNode(rootNode, filtersComponent.getComponents().get(0)));
            } else {
                children.add(convertToNode(rootNode, filtersComponent));
            }
        }

        rootComponent = null;
    }

    /**
     * Creates an advertiser node.
     *
     * @param advertiserString the advertiser string
     * @param configSource the configuration source
     */
    public void createAdvertiser(
            final @Nullable String advertiserString, final @Nullable ConfigurationSource configSource) {
        byte[] buffer = null;
        try {
            if (configSource != null) {
                final InputStream is = configSource.getInputStream();
                if (is != null) {
                    buffer = toByteArray(is);
                }
            }
        } catch (final IOException ioe) {
            LOGGER.warn("Unable to read configuration source {}", configSource);
        }
        super.createAdvertiser(advertiserString, configSource, buffer, contentType);
    }

    /**
     * Gets the content type.
     *
     * @return returns the content-type
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Returns the status configuration of the {@code StatusLogger} fallback listener.
     * @return the status configurations
     */
    public StatusConfiguration getStatusConfiguration() {
        return statusConfig;
    }

    /**
     * Sets the content-type.
     * <p>
     *   If the given {@code contentType} is {@code null}, the default content-type "{@code text}" will be assigned.
     * </p>
     *
     * @param contentType the content-type
     */
    public void setContentType(final @Nullable String contentType) {
        this.contentType = (contentType != null) ? contentType : DEFAULT_CONTENT_TYPE;
    }

    /**
     * Sets the plugin-packages to scan for Log4j plugins.
     * @param packages a comma-separated list of package names
     */
    public void setPluginPackages(final @Nullable String packages) {
        if (packages != null) {
            pluginPackages.addAll(Arrays.asList(packages.split(Patterns.COMMA_SEPARATOR)));
        }
    }

    /**
     * Sets the shutdown-hook flag.
     * @param flag "{@code disable}" to disable the shutdown-hook; otherwise, enabled
     */
    public void setShutdownHook(final String flag) {
        isShutdownHookEnabled = !"disable".equalsIgnoreCase(flag);
    }

    /**
     * Sets the shutdown time in milliseconds.
     * <p>
     *   The shutdown-timeout specifies how long appenders and background tasks will get to shut down when the
     *   JVM is shutting down.
     * </p>
     * <p>
     *   The default is zero which means that each appender uses its default timeout, and doesn't wait for background
     *   tasks. Not all appenders will honor this, it is a hint and not an absolute guarantee that the shutdown
     *   procedure will not take longer.
     * </p>
     * <p>
     *   Setting the shutdown-timeout too low increase the risk of losing outstanding log events that have not yet
     *   been written to the final destination.
     * </p>
     * <p>
     *   This setting is ignored if {@link #setShutdownHook(String)} has been set to "disable".
     * </p>
     * @param shutdownTimeoutMillis the shutdown timeout (in milliseconds)
     */
    public void setShutdownTimeoutMillis(final long shutdownTimeoutMillis) {
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
    }

    /**
     * Sets the configuration's "monitorInterval" attribute.
     * <p>
     *   The monitor interval specifies the number of seconds between checks for changes to the configuration source.
     * </p>
     *
     * @param intervalSeconds the monitor interval (in seconds)
     */
    public void setMonitorInterval(final int intervalSeconds) {
        if (this instanceof Reconfigurable && intervalSeconds > 0) {
            initializeWatchers((Reconfigurable) this, getConfigurationSource(), intervalSeconds);
        }
    }

    /**
     * Converts the given {@link Component} into a {@link Node} with the given {@code parent}.
     * <p>
     *   This method will recurse through the children of the given component building a node tree.
     * </p>
     * <p>
     *   Note: This method assigns the specified parent within the new child node; however, it does not
     *   <i>add</i> the new node to the parent.  That is performed by the caller (which in most cases this
     *   method due to recursion).
     * </p>
     *
     * @param parent the parent node
     * @param component the component to convert
     * @return the converted node
     */
    protected Node convertToNode(final Node parent, final Component component) {
        final String name = component.getPluginType();
        final PluginType<?> pluginType = pluginManager.getPluginType(name);
        final Node node = new Node(parent, name, pluginType);
        node.getAttributes().putAll(component.getAttributes());
        node.setValue(component.getValue());
        final List<Node> children = node.getChildren();
        for (final Component child : component.getComponents()) {
            children.add(convertToNode(node, child));
        }
        return node;
    }
}
