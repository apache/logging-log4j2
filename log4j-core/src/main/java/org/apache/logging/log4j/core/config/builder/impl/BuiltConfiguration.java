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
import java.util.List;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.jspecify.annotations.Nullable;

/**
 * This is the general version of the Configuration created by the Builder. It may be extended to
 * enhance its functionality.
 *
 * @since 2.4
 */
public class BuiltConfiguration extends AbstractConfiguration {
    private final StatusConfiguration statusConfig;
    protected Component rootComponent;
    private Component loggersComponent;
    private Component appendersComponent;
    private Component filtersComponent;
    private Component propertiesComponent;
    private Component customLevelsComponent;
    private Component scriptsComponent;
    private String contentType = "text";

    public BuiltConfiguration(
            final @Nullable LoggerContext loggerContext,
            final ConfigurationSource source,
            final Component rootComponent) {
        super(
                loggerContext,
                source,
                loggerContext != null ? loggerContext.getEnvironment() : PropertiesUtil.getProperties(),
                loggerContext != null
                        ? (ConfigurableInstanceFactory) loggerContext.getInstanceFactory()
                        : DI.createInitializedFactory());
        statusConfig = new StatusConfiguration().setStatus(getDefaultStatus());
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
            }
        }
        this.rootComponent = rootComponent;
    }

    @Override
    public void setup() {
        final List<Node> children = rootNode.getChildren();
        if (propertiesComponent.getComponents().size() > 0) {
            children.add(convertToNode(rootNode, propertiesComponent));
        }
        if (scriptsComponent.getComponents().size() > 0) {
            children.add(convertToNode(rootNode, scriptsComponent));
        }
        if (customLevelsComponent.getComponents().size() > 0) {
            children.add(convertToNode(rootNode, customLevelsComponent));
        }
        children.add(convertToNode(rootNode, loggersComponent));
        children.add(convertToNode(rootNode, appendersComponent));
        if (filtersComponent.getComponents().size() > 0) {
            if (filtersComponent.getComponents().size() == 1) {
                children.add(
                        convertToNode(rootNode, filtersComponent.getComponents().get(0)));
            } else {
                children.add(convertToNode(rootNode, filtersComponent));
            }
        }
        rootComponent = null;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    public void createAdvertiser(final String advertiserString, final ConfigurationSource configSource) {
        byte[] buffer = null;
        try {
            if (configSource != null) {
                final InputStream is = configSource.getInputStream();
                if (is != null) {
                    buffer = is.readAllBytes();
                }
            }
        } catch (final IOException ioe) {
            LOGGER.warn("Unable to read configuration source {}", configSource);
        }
        super.createAdvertiser(advertiserString, configSource, buffer, contentType);
    }

    public StatusConfiguration getStatusConfiguration() {
        return statusConfig;
    }

    public void setShutdownHook(final String flag) {
        isShutdownHookEnabled = !"disable".equalsIgnoreCase(flag);
    }

    public void setShutdownTimeoutMillis(final long shutdownTimeoutMillis) {
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
    }

    public void setMonitorInterval(final int intervalSeconds) {
        if (this instanceof Reconfigurable && intervalSeconds > 0) {
            initializeWatchers((Reconfigurable) this, getConfigurationSource(), intervalSeconds);
        }
    }

    protected Node convertToNode(final Node parent, final Component component) {
        final String name = component.getPluginType();
        final PluginType<?> pluginType = corePlugins.get(name);
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
