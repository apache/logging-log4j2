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
package org.apache.logging.log4j.core.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.core.util.Cancellable;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.core.util.ShutdownCallbackRegistry;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Singleton;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;

import static org.apache.logging.log4j.util.Constants.isWebApp;

/**
 * Factory to locate a ContextSelector and then load a LoggerContext.
 */
@Singleton
public class Log4jContextFactory implements LoggerContextFactory, ShutdownCallbackRegistry {

    private static final StatusLogger LOGGER = StatusLogger.getLogger();

    private final Injector injector;
    private final ContextSelector selector;
    private final ShutdownCallbackRegistry shutdownCallbackRegistry;

    /**
     * Initializes the ContextSelector from system property {@link Constants#LOG4J_CONTEXT_SELECTOR}.
     */
    public Log4jContextFactory() {
        injector = DI.createInjector();
        injector.init();
        this.selector = injector.getInstance(ContextSelector.KEY);
        this.shutdownCallbackRegistry = injector.getInstance(ShutdownCallbackRegistry.KEY);
        LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
        initializeShutdownCallbackRegistry();
    }

    /**
     * Initializes this factory's ContextSelector with the specified selector.
     * @param selector the selector to use
     */
    public Log4jContextFactory(final ContextSelector selector) {
        Objects.requireNonNull(selector, "No ContextSelector provided");
        injector = DI.createInjector();
        injector.init();
        injector.registerBinding(ContextSelector.KEY, () -> selector);
        this.selector = injector.getInstance(ContextSelector.KEY);
        this.shutdownCallbackRegistry = injector.getInstance(ShutdownCallbackRegistry.KEY);
        LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
        initializeShutdownCallbackRegistry();
    }

    /**
     * Constructs a Log4jContextFactory using the ContextSelector from {@link Constants#LOG4J_CONTEXT_SELECTOR}
     * and the provided ShutdownRegistrationStrategy.
     *
     * @param shutdownCallbackRegistry the ShutdownRegistrationStrategy to use
     * @since 2.1
     */
    public Log4jContextFactory(final ShutdownCallbackRegistry shutdownCallbackRegistry) {
        Objects.requireNonNull(shutdownCallbackRegistry, "No ShutdownCallbackRegistry provided");
        injector = DI.createInjector();
        injector.init();
        injector.registerBinding(ShutdownCallbackRegistry.KEY, () -> shutdownCallbackRegistry);
        this.selector = injector.getInstance(ContextSelector.KEY);
        this.shutdownCallbackRegistry = injector.getInstance(ShutdownCallbackRegistry.KEY);
        LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
        initializeShutdownCallbackRegistry();
    }

    /**
     * Constructs a Log4jContextFactory using the provided ContextSelector and ShutdownRegistrationStrategy.
     *
     * @param selector                     the selector to use
     * @param shutdownCallbackRegistry the ShutdownRegistrationStrategy to use
     * @since 2.1
     */
    public Log4jContextFactory(final ContextSelector selector,
                               final ShutdownCallbackRegistry shutdownCallbackRegistry) {
        Objects.requireNonNull(selector, "No ContextSelector provided");
        Objects.requireNonNull(shutdownCallbackRegistry, "No ShutdownCallbackRegistry provided");
        injector = DI.createInjector();
        injector.init();
        injector.registerBinding(ContextSelector.KEY, () -> selector)
                .registerBinding(ShutdownCallbackRegistry.KEY, () -> shutdownCallbackRegistry);
        this.selector = injector.getInstance(ContextSelector.KEY);
        this.shutdownCallbackRegistry = injector.getInstance(ShutdownCallbackRegistry.KEY);
        LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
        initializeShutdownCallbackRegistry();
    }

    @Inject
    public Log4jContextFactory(final Injector injector, final ContextSelector selector, final ShutdownCallbackRegistry registry) {
        this.injector = injector;
        this.selector = selector;
        this.shutdownCallbackRegistry = registry;
        LOGGER.debug("Using ShutdownCallbackRegistry {}", this.shutdownCallbackRegistry.getClass());
        initializeShutdownCallbackRegistry();
    }

    public Log4jContextFactory(final Injector injector) {
        this(injector, injector.getInstance(ContextSelector.KEY), injector.getInstance(ShutdownCallbackRegistry.KEY));
    }

    private void initializeShutdownCallbackRegistry() {
        if (isShutdownHookEnabled() && this.shutdownCallbackRegistry instanceof LifeCycle) {
            try {
                ((LifeCycle) this.shutdownCallbackRegistry).start();
            } catch (final IllegalStateException e) {
                LOGGER.error("Cannot start ShutdownCallbackRegistry, already shutting down.");
                throw e;
            } catch (final RuntimeException e) {
                LOGGER.error("There was an error starting the ShutdownCallbackRegistry.", e);
            }
        }
    }

    /**
     * Loads the LoggerContext using the ContextSelector.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @return The LoggerContext.
     */
    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
                                    final boolean currentContext) {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext);
        if (externalContext != null && ctx.getExternalContext() == null) {
            ctx.setExternalContext(externalContext);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED) {
            ctx.start();
        }
        return ctx;
    }

    /**
     * Loads the LoggerContext using the ContextSelector.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param source The configuration source.
     * @return The LoggerContext.
     */
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
                                    final boolean currentContext, final ConfigurationSource source) {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext, null);
        if (externalContext != null && ctx.getExternalContext() == null) {
            ctx.setExternalContext(externalContext);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED) {
            if (source != null) {
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                final Configuration config = injector.getInstance(ConfigurationFactory.KEY).getConfiguration(ctx, source);
                LOGGER.debug("Starting {} from configuration {}", ctx, source);
                ctx.start(config);
                ContextAnchor.THREAD_CONTEXT.remove();
            } else {
                ctx.start();
            }
        }
        return ctx;
    }

    /**
     * Loads the LoggerContext using the ContextSelector using the provided Configuration
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param configuration The Configuration.
     * @return The LoggerContext.
     */
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
            final boolean currentContext, final Configuration configuration) {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext, null);
        if (externalContext != null && ctx.getExternalContext() == null) {
            ctx.setExternalContext(externalContext);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED) {
            ContextAnchor.THREAD_CONTEXT.set(ctx);
            try {
                ctx.start(configuration);
            } finally {
                ContextAnchor.THREAD_CONTEXT.remove();
            }
        }
        return ctx;
    }

    /**
     * Loads the LoggerContext using the ContextSelector.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param externalContext An external context (such as a ServletContext) to be associated with the LoggerContext.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @param configLocation The location of the configuration for the LoggerContext (or null).
     * @return The LoggerContext.
     */
    @Override
    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
                                    final boolean currentContext, final URI configLocation, final String name) {
        final LoggerContext ctx = selector.getContext(fqcn, loader, currentContext, configLocation);
        if (externalContext != null && ctx.getExternalContext() == null) {
            ctx.setExternalContext(externalContext);
        }
        if (name != null) {
            ctx.setName(name);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED) {
            if (configLocation != null || name != null) {
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                final Configuration config =
                        injector.getInstance(ConfigurationFactory.KEY).getConfiguration(ctx, name, configLocation);
                LOGGER.debug("Starting {} from configuration at {}", ctx, configLocation);
                ctx.start(config);
                ContextAnchor.THREAD_CONTEXT.remove();
            } else {
                ctx.start();
            }
        }
        return ctx;
    }

    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Map.Entry<String, Object> entry,
            final boolean currentContext, final URI configLocation, final String name) {
        final LoggerContext ctx = selector.getContext(fqcn, loader, entry, currentContext, configLocation);
        if (name != null) {
            ctx.setName(name);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED) {
            if (configLocation != null || name != null) {
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                final Configuration config =
                        injector.getInstance(ConfigurationFactory.KEY).getConfiguration(ctx, name, configLocation);
                LOGGER.debug("Starting {} from configuration at {}", ctx, configLocation);
                ctx.start(config);
                ContextAnchor.THREAD_CONTEXT.remove();
            } else {
                ctx.start();
            }
        }
        return ctx;
    }

    public LoggerContext getContext(final String fqcn, final ClassLoader loader, final Object externalContext,
            final boolean currentContext, final List<URI> configLocations, final String name) {
        final LoggerContext ctx = selector
                .getContext(fqcn, loader, currentContext, null/*this probably needs to change*/);
        if (externalContext != null && ctx.getExternalContext() == null) {
            ctx.setExternalContext(externalContext);
        }
        if (name != null) {
            ctx.setName(name);
        }
        if (ctx.getState() == LifeCycle.State.INITIALIZED) {
            if ((configLocations != null && !configLocations.isEmpty())) {
                ContextAnchor.THREAD_CONTEXT.set(ctx);
                final List<AbstractConfiguration> configurations = new ArrayList<>(configLocations.size());
                for (final URI configLocation : configLocations) {
                    final Configuration currentReadConfiguration = injector.getInstance(ConfigurationFactory.KEY)
                            .getConfiguration(ctx, name, configLocation);
                    if (currentReadConfiguration != null) {
                        if (currentReadConfiguration instanceof DefaultConfiguration) {
                            LOGGER.warn("Unable to locate configuration {}, ignoring", configLocation.toString());
                        }
                        else if (currentReadConfiguration instanceof AbstractConfiguration) {
                            configurations.add((AbstractConfiguration) currentReadConfiguration);
                        } else {
                            LOGGER.error(
                                    "Found configuration {}, which is not an AbstractConfiguration and can't be handled by CompositeConfiguration",
                                    configLocation);
                        }
                    } else {
                        LOGGER.info("Unable to access configuration {}, ignoring", configLocation.toString());
                    }
                }
                if (configurations.size() == 0) {
                    LOGGER.error("No configurations could be created for {}", configLocations.toString());
                } else if (configurations.size() == 1) {
                    ctx.start(configurations.get(0));
                } else {
                    final CompositeConfiguration compositeConfiguration = new CompositeConfiguration(configurations);
                    ctx.start(compositeConfiguration);
                }
                ContextAnchor.THREAD_CONTEXT.remove();
            } else {
                ctx.start();
            }
        }
        return ctx;
    }

    @Override
    public void shutdown(final String fqcn, final ClassLoader loader, final boolean currentContext, final boolean allContexts) {
        if (selector.hasContext(fqcn, loader, currentContext)) {
            selector.shutdown(fqcn, loader, currentContext, allContexts);
        }
    }

    /**
     * Checks to see if a LoggerContext is installed.
     * @param fqcn The fully qualified class name of the caller.
     * @param loader The ClassLoader to use or null.
     * @param currentContext If true returns the current Context, if false returns the Context appropriate
     * for the caller if a more appropriate Context can be determined.
     * @return true if a LoggerContext has been installed, false otherwise.
     * @since 2.13.0
     */
    @Override
    public boolean hasContext(final String fqcn, final ClassLoader loader, final boolean currentContext) {
        return selector.hasContext(fqcn, loader, currentContext);
    }

    /**
     * Returns the ContextSelector.
     * @return The ContextSelector.
     */
    public ContextSelector getSelector() {
        return selector;
    }

    /**
     * Returns the ShutdownCallbackRegistry
     *
     * @return the ShutdownCallbackRegistry
     * @since 2.4
     */
    public ShutdownCallbackRegistry getShutdownCallbackRegistry() {
        return shutdownCallbackRegistry;
    }

    /**
     * Removes knowledge of a LoggerContext.
     *
     * @param context The context to remove.
     */
    @Override
    public void removeContext(final org.apache.logging.log4j.spi.LoggerContext context) {
        if (context instanceof LoggerContext) {
            selector.removeContext((LoggerContext) context);
        }
    }

    @Override
    public boolean isClassLoaderDependent() {
        return selector.isClassLoaderDependent();
    }

    @Override
    public Cancellable addShutdownCallback(final Runnable callback) {
        return isShutdownHookEnabled() ? shutdownCallbackRegistry.addShutdownCallback(callback) : null;
    }

    public boolean isShutdownHookEnabled() {
        return !isWebApp() && PropertiesUtil.getProperties()
                .getBooleanProperty(Log4jProperties.SHUTDOWN_HOOK_ENABLED, true);
    }
}
