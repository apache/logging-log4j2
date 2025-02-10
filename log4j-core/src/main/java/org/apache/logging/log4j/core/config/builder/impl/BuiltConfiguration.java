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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.util.Patterns;

/**
 * A {@link Configuration} that is built (assembled) via a {@link ConfigurationBuilder}.
 * <p>
 *     This base implementation may be extended to enhance its functionality.
 * </p>
 * <h3>Important Notes for Extension:</h3>
 * <p>
 *     <ul>
 *         <li>This class is designed to be extended for custom configurations. Subclasses should maintain
 *         the contract of returning valid and non-{@code null} configuration components.</li>
 *         <li>Fields such as {@code rootComponent} are intentionally made accessible to subclasses. However,
 *         direct modification of such fields is discouraged to prevent inconsistencies.</li>
 *         <li>Where possible, subclasses should adhere to the public accessor methods and override provided hooks
 *         like {@link #setup()} to implement additional configuration logic.</li>
 * </ul>
 * </p>
 * <h3>Backward Compatibility</h3>
 * <p>
 *     Certain design decisions in this class, such as the use of
 *     a {@code protected} field for {@code rootComponent}, have been retained for historical reasons and due to
 *     guarantees of binary compatibility for users of this class. Users of this API should avoid directly manipulating
 *     internal fields where possible and instead rely on public or protected accessor methods where available.
 * </p>
 * <p>
 *     This implementation is neither immutable nor thread-safe.
 * </p>
 *
 * @since 2.4
 */
public class BuiltConfiguration extends AbstractConfiguration {

    private final StatusConfiguration statusConfig;

    protected Component rootComponent; // should be private and accessed via getter, but backwards-compatibility...

    private String contentType = "text";

    /**
     * Constructs a new instance.
     * <p>
     *     The configuration assembled using the {@link ConfigurationBuilder} is transported via the given
     *     {@code rootComponent} and used to populate this configuration's root {@code Node} in the
     *     {@link #setup} method implementation.
     * </p>
     * <p>
     *     This constructor is called via reflection from the {@link DefaultConfigurationBuilder}.
     * </p>
     *
     * @param loggerContext the logger-context (can be {@code null})
     * @param configurationSource the configuration-source
     * @param rootComponent the root-component created by the builder
     * @throws NullPointerException if the {@code source} or {@code rootComponent} argument is {@code null}
     */
    public BuiltConfiguration(
            final LoggerContext loggerContext,
            final ConfigurationSource configurationSource,
            final Component rootComponent) {

        super(
                loggerContext,
                Objects.requireNonNull(configurationSource, "The 'configurationSource' argument cannot be null"));

        Objects.requireNonNull(rootComponent, "The 'rootComponent' argument cannot be null");

        this.rootComponent = rootComponent;
        this.statusConfig = new StatusConfiguration().withStatus(getDefaultStatus());

        // process each of the root-component's attributes, dealing with special cases and assigning all to root node
        for (Map.Entry<String, String> entry : rootComponent.getAttributes().entrySet()) {
            final String key = entry.getKey().trim();
            final String value = entry.getValue();
            this.getRootNode().getAttributes().put(key, value); // all attributes get passed to root-node
            if ("advertise".equalsIgnoreCase(key)) {
                createAdvertiser(value, getConfigurationSource());
            } else if ("dest".equalsIgnoreCase(key)) {
                statusConfig.withDestination(value);
            } else if ("monitorInterval".equalsIgnoreCase(key)) {
                setMonitorInterval(value, TimeUnit.SECONDS);
            } else if ("name".equalsIgnoreCase(key)) {
                setName(value);
            } else if ("packages".equalsIgnoreCase(key)) {
                setPluginPackages(value);
            } else if ("shutdownHook".equalsIgnoreCase(key)) {
                setShutdownHook(value);
            } else if ("shutdownTimeout".equalsIgnoreCase(key)) {
                setShutdownTimeout(value, TimeUnit.MILLISECONDS);
            } else if ("status".equalsIgnoreCase(key)) {
                statusConfig.withStatus(value);
            }
        }

        this.statusConfig.initialize();
    }

    /**
     * {@inheritDoc}
     * <p>
     *     Converts the {@code Component} children of the root component to Log4j configuration nodes and
     *     subsequently invalidates the root component.
     * </p>
     */
    @Override
    public void setup() {

        final List<Node> children = rootNode.getChildren();

        if (this.rootComponent != null) {

            getChildComponent("Properties")
                    .filter(c -> !c.getComponents().isEmpty())
                    .ifPresent(c -> children.add(convertToNode(rootNode, c)));
            getChildComponent("Scripts")
                    .filter(c -> !c.getComponents().isEmpty())
                    .ifPresent(c -> children.add(convertToNode(rootNode, c)));
            getChildComponent("CustomLevels")
                    .filter(c -> !c.getComponents().isEmpty())
                    .ifPresent(c -> children.add(convertToNode(rootNode, c)));

            children.add(convertToNode(rootNode, getChildComponent("Loggers").orElse(new Component("Loggers"))));
            children.add(convertToNode(rootNode, getChildComponent("Appenders").orElse(new Component("Appenders"))));

            getChildComponent("Filters")
                    .filter(c -> !c.getComponents().isEmpty())
                    .ifPresent(c -> {
                        if (c.getComponents().size() == 1) {
                            children.add(
                                    convertToNode(rootNode, c.getComponents().get(0)));
                        } else {
                            children.add(convertToNode(rootNode, c));
                        }
                    });
        }

        this.rootComponent = null;
    }

    /**
     * Gets the content-type of the built configuration.
     * @return the content-type
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Sets the content-type of the build configuration.
     * @param contentType the content-type
     */
    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    /**
     * Creates and sets this configuration's {@link Advertiser} for the given configuration-source.
     * @param name the name of the advertiser
     * @param configSource the configuration-source
     */
    public void createAdvertiser(final String name, final ConfigurationSource configSource) {
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
        super.createAdvertiser(name, configSource, buffer, contentType);
    }

    /**
     * Returns the status-logger fallback listener configuration.
     * <p>
     *     Implementations should always return a non-{@code null} value.
     * </p>
     * @return the status configuration
     */
    public StatusConfiguration getStatusConfiguration() {
        return statusConfig;
    }

    /**
     * Sets the packages to search for plugin implementations.
     * @param packages a comma-separated list of package names
     */
    public void setPluginPackages(final String packages) {
        Objects.requireNonNull(packages, "The 'packages' argument cannot be null");
        List<String> packageList = Arrays.stream(packages.split(Patterns.COMMA_SEPARATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        pluginPackages.addAll(packageList);
    }

    /**
     * Sets the shutdown hook flag.
     * @param flag if "disable" the shutdown hook will be disabled; otherwise, all other values enable it
     */
    public void setShutdownHook(final String flag) {
        setShutdownHook(!"disable".equalsIgnoreCase(flag));
    }

    /**
     * Sets the enablement of the shutdown hook.
     * @param flag {@code true} to enable the shutdown hook (default); otherwise, {@code false} to disable
     */
    public void setShutdownHook(final boolean flag) {
        isShutdownHookEnabled = flag;
    }

    /**
     * Sets the shutdown timeout to the given value converted to milliseconds using the provided time-unit.
     * @param value the value
     * @param timeUnit the time-unit of the value (i.e. MILLISECONDS, SECONDS, MINUTES, etc.)
     */
    public void setShutdownTimeout(final String value, final TimeUnit timeUnit) {
        Objects.requireNonNull(value, "The 'value' argument cannot be null");
        Objects.requireNonNull(timeUnit, "The 'timeUnit' argument cannot be null");
        try {
            setShutdownTimeoutMillis(timeUnit.toMillis(Long.parseLong(value)));
        } catch (final Exception ex) {
            LOGGER.error("The given shudown timeoutt is invalid '{}'. Reason: {}", value, ex.getMessage());
        }
    }

    /**
     * Sets the shutdown timeout in milliseconds.
     * @param millis the number of milliseconds to set
     */
    public void setShutdownTimeoutMillis(final long millis) {
        this.shutdownTimeoutMillis = millis;
    }

    /**
     * Sets the monitor interval to the given value converted to seconds using the provided time-unit.
     * @param value the value
     * @param timeUnit the time-unit of the value (i.e. MILLISECONDS, SECONDS, MINUTES, etc.)
     */
    public void setMonitorInterval(final String value, final TimeUnit timeUnit) {
        Objects.requireNonNull(value, "The 'value' argument cannot be null");
        Objects.requireNonNull(timeUnit, "The 'timeUnit' argument cannot be null");
        try {
            setMonitorInterval(Math.toIntExact(timeUnit.toSeconds(Integer.parseInt(value))));
        } catch (final Exception ex) {
            LOGGER.error("The given monitor interval is invalid '{}'. Reason: {}", value, ex.getMessage());
        }
    }

    /**
     * Sets the monitor interval to the given number of seconds.
     * <p>
     *     If the given value is greater than 0 and this instance implements {@link Reconfigurable},
     *     this method will trigger the {@link #initializeWatchers(Reconfigurable, ConfigurationSource, int)}
     *     method.
     * </p>
     * @param seconds the number of seconds to set
     */
    public void setMonitorInterval(final int seconds) {
        if (this instanceof Reconfigurable && seconds > 0) {
            initializeWatchers((Reconfigurable) this, getConfigurationSource(), seconds);
        }
    }

    /**
     * Gets the root component.
     * <p>
     *     NOTE: After {@link #setup()} has been called this will always return an empty optional.
     * </p>
     * @return an optional containing the root component or that is <i>empty</i> if undefined
     */
    protected final Optional<Component> getRootComponent() {
        return Optional.ofNullable(rootComponent);
    }

    /**
     * Gets the child component of the root component with the given plugin-type.
     * @param pluginType the plugin-type to lookup
     * @return an optional containing the resolved child component or that is <i>empty</i> if not found
     */
    protected final Optional<Component> getChildComponent(final String pluginType) {
        Objects.requireNonNull(pluginType, "The 'pluginType' argument cannot be null");
        return Optional.ofNullable(getChildComponents().get(pluginType));
    }

    /**
     * Returns a map of all "named" child components.
     * @return an <i>immutable</i> map of child components keyed by name
     */
    protected final Map<String, Component> getChildComponents() {

        return this.rootComponent.getComponents().stream()
                .filter(c -> Objects.nonNull(c.getPluginType()))
                .collect(Collectors.toMap(Component::getPluginType, Function.identity()));
    }

    /**
     * Converts the given configuration {@link Component} to a runtime configuration {@link Node}
     * with the given parent node.
     * @param parent the target parent node
     * @param component the component to convert
     * @return the constructed node
     */
    protected Node convertToNode(final Node parent, final Component component) {
        Objects.requireNonNull(parent, "The 'parent' argument cannot be null");
        Objects.requireNonNull(component, "The 'component' argument cannot be null");
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
