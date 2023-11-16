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
package org.apache.logging.log4j.core.appender.routing;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.script.Bindings;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.AppenderControl;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.script.AbstractScript;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.script.ScriptRef;
import org.apache.logging.log4j.core.util.Booleans;

/**
 * This Appender "routes" between various Appenders, some of which can be references to
 * Appenders defined earlier in the configuration while others can be dynamically created
 * within this Appender as required. Routing is achieved by specifying a pattern on
 * the Routing appender declaration. The pattern should contain one or more substitution patterns of
 * the form "$${[key:]token}". The pattern will be resolved each time the Appender is called using
 * the built in StrSubstitutor and the StrLookup plugin that matches the specified key.
 */
@Plugin(name = "Routing", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public final class RoutingAppender extends AbstractAppender {

    public static final String STATIC_VARIABLES_KEY = "staticVariables";

    public static class Builder<B extends Builder<B>> extends AbstractAppender.Builder<B>
            implements org.apache.logging.log4j.core.util.Builder<RoutingAppender> {

        // Does not work unless the element is called "Script", I wanted "DefaultRounteScript"...
        @PluginElement("Script")
        private AbstractScript defaultRouteScript;

        @PluginElement("Routes")
        private Routes routes;

        @PluginElement("RewritePolicy")
        private RewritePolicy rewritePolicy;

        @PluginElement("PurgePolicy")
        private PurgePolicy purgePolicy;

        @Override
        public RoutingAppender build() {
            final String name = getName();
            if (name == null) {
                LOGGER.error("No name defined for this RoutingAppender");
                return null;
            }
            if (routes == null) {
                LOGGER.error("No routes defined for RoutingAppender {}", name);
                return null;
            }
            if (defaultRouteScript != null) {
                if (getConfiguration().getScriptManager() == null) {
                    LOGGER.error("Script support is not enabled");
                    return null;
                }
                if (!(defaultRouteScript instanceof ScriptRef)) {
                    if (!getConfiguration().getScriptManager().addScript(defaultRouteScript)) {
                        return null;
                    }
                }
            }
            return new RoutingAppender(
                    name,
                    getFilter(),
                    isIgnoreExceptions(),
                    routes,
                    rewritePolicy,
                    getConfiguration(),
                    purgePolicy,
                    defaultRouteScript,
                    getPropertyArray());
        }

        public Routes getRoutes() {
            return routes;
        }

        public AbstractScript getDefaultRouteScript() {
            return defaultRouteScript;
        }

        public RewritePolicy getRewritePolicy() {
            return rewritePolicy;
        }

        public PurgePolicy getPurgePolicy() {
            return purgePolicy;
        }

        public B withRoutes(@SuppressWarnings("hiding") final Routes routes) {
            this.routes = routes;
            return asBuilder();
        }

        public B withDefaultRouteScript(@SuppressWarnings("hiding") final AbstractScript defaultRouteScript) {
            this.defaultRouteScript = defaultRouteScript;
            return asBuilder();
        }

        public B withRewritePolicy(@SuppressWarnings("hiding") final RewritePolicy rewritePolicy) {
            this.rewritePolicy = rewritePolicy;
            return asBuilder();
        }

        public void withPurgePolicy(@SuppressWarnings("hiding") final PurgePolicy purgePolicy) {
            this.purgePolicy = purgePolicy;
        }
    }

    @PluginBuilderFactory
    public static <B extends Builder<B>> B newBuilder() {
        return new Builder<B>().asBuilder();
    }

    private static final String DEFAULT_KEY = "ROUTING_APPENDER_DEFAULT";

    private final Routes routes;
    private Route defaultRoute;
    private final Configuration configuration;
    private final ConcurrentMap<String, CreatedRouteAppenderControl> createdAppenders = new ConcurrentHashMap<>();
    private final Map<String, AppenderControl> createdAppendersUnmodifiableView =
            Collections.unmodifiableMap((Map<String, AppenderControl>) (Map<String, ?>) createdAppenders);
    private final ConcurrentMap<String, RouteAppenderControl> referencedAppenders = new ConcurrentHashMap<>();
    private final RewritePolicy rewritePolicy;
    private final PurgePolicy purgePolicy;
    private final AbstractScript defaultRouteScript;
    private final ConcurrentMap<Object, Object> scriptStaticVariables = new ConcurrentHashMap<>();

    private RoutingAppender(
            final String name,
            final Filter filter,
            final boolean ignoreExceptions,
            final Routes routes,
            final RewritePolicy rewritePolicy,
            final Configuration configuration,
            final PurgePolicy purgePolicy,
            final AbstractScript defaultRouteScript,
            final Property[] properties) {
        super(name, filter, null, ignoreExceptions, properties);
        this.routes = routes;
        this.configuration = configuration;
        this.rewritePolicy = rewritePolicy;
        this.purgePolicy = purgePolicy;
        if (this.purgePolicy != null) {
            this.purgePolicy.initialize(this);
        }
        this.defaultRouteScript = defaultRouteScript;
        Route defRoute = null;
        for (final Route route : routes.getRoutes()) {
            if (route.getKey() == null) {
                if (defRoute == null) {
                    defRoute = route;
                } else {
                    error("Multiple default routes. Route " + route.toString() + " will be ignored");
                }
            }
        }
        defaultRoute = defRoute;
    }

    @Override
    public void start() {
        if (defaultRouteScript != null) {
            if (configuration == null) {
                error("No Configuration defined for RoutingAppender; required for Script element.");
            } else {
                final ScriptManager scriptManager = configuration.getScriptManager();
                final Bindings bindings = scriptManager.createBindings(defaultRouteScript);
                bindings.put(STATIC_VARIABLES_KEY, scriptStaticVariables);
                final Object object = scriptManager.execute(defaultRouteScript.getName(), bindings);
                final Route route = routes.getRoute(Objects.toString(object, null));
                if (route != null) {
                    defaultRoute = route;
                }
            }
        }
        // Register all the static routes.
        for (final Route route : routes.getRoutes()) {
            if (route.getAppenderRef() != null) {
                final Appender appender = configuration.getAppender(route.getAppenderRef());
                if (appender != null) {
                    final String key = route == defaultRoute ? DEFAULT_KEY : route.getKey();
                    referencedAppenders.put(key, new ReferencedRouteAppenderControl(appender));
                } else {
                    error("Appender " + route.getAppenderRef() + " cannot be located. Route ignored");
                }
            }
        }
        super.start();
    }

    @Override
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        setStopping();
        super.stop(timeout, timeUnit, false);
        // Only stop appenders that were created by this RoutingAppender
        for (final Map.Entry<String, CreatedRouteAppenderControl> entry : createdAppenders.entrySet()) {
            final Appender appender = entry.getValue().getAppender();
            if (appender instanceof LifeCycle2) {
                ((LifeCycle2) appender).stop(timeout, timeUnit);
            } else {
                appender.stop();
            }
        }
        setStopped();
        return true;
    }

    @Override
    public void append(LogEvent event) {
        if (rewritePolicy != null) {
            event = rewritePolicy.rewrite(event);
        }
        final String pattern = routes.getPattern(event, scriptStaticVariables);
        final String key = pattern != null
                ? configuration.getStrSubstitutor().replace(event, pattern)
                : defaultRoute.getKey() != null ? defaultRoute.getKey() : DEFAULT_KEY;
        final RouteAppenderControl control = getControl(key, event);
        if (control != null) {
            try {
                control.callAppender(event);
            } finally {
                control.release();
            }
        }
        updatePurgePolicy(key, event);
    }

    private void updatePurgePolicy(final String key, final LogEvent event) {
        if (purgePolicy != null
                // LOG4J2-2631: PurgePolicy implementations do not need to be aware of appenders that
                // were not created by this RoutingAppender.
                && !referencedAppenders.containsKey(key)) {
            purgePolicy.update(key, event);
        }
    }

    private synchronized RouteAppenderControl getControl(final String key, final LogEvent event) {
        RouteAppenderControl control = getAppender(key);
        if (control != null) {
            control.checkout();
            return control;
        }
        Route route = null;
        for (final Route r : routes.getRoutes()) {
            if (r.getAppenderRef() == null && key.equals(r.getKey())) {
                route = r;
                break;
            }
        }
        if (route == null) {
            route = defaultRoute;
            control = getAppender(DEFAULT_KEY);
            if (control != null) {
                control.checkout();
                return control;
            }
        }
        if (route != null) {
            final Appender app = createAppender(route, event);
            if (app == null) {
                return null;
            }
            final CreatedRouteAppenderControl created = new CreatedRouteAppenderControl(app);
            control = created;
            createdAppenders.put(key, created);
        }

        if (control != null) {
            control.checkout();
        }
        return control;
    }

    private RouteAppenderControl getAppender(final String key) {
        final RouteAppenderControl result = referencedAppenders.get(key);
        if (result == null) {
            return createdAppenders.get(key);
        }
        return result;
    }

    private Appender createAppender(final Route route, final LogEvent event) {
        final Node routeNode = route.getNode();
        for (final Node node : routeNode.getChildren()) {
            if (node.getType().getElementName().equals(Appender.ELEMENT_TYPE)) {
                final Node appNode = new Node(node);
                configuration.createConfiguration(appNode, event);
                if (appNode.getObject() instanceof Appender) {
                    final Appender app = appNode.getObject();
                    app.start();
                    return app;
                }
                error("Unable to create Appender of type " + node.getName());
                return null;
            }
        }
        error("No Appender was configured for route " + route.getKey());
        return null;
    }

    /**
     * Returns an unmodifiable view of the appenders created by this {@link RoutingAppender}.
     * Note that this map does not contain appenders that are routed by reference.
     */
    public Map<String, AppenderControl> getAppenders() {
        return createdAppendersUnmodifiableView;
    }

    /**
     * Deletes the specified appender.
     *
     * @param key The appender's key
     */
    public void deleteAppender(final String key) {
        LOGGER.debug("Deleting route with {} key ", key);
        // LOG4J2-2631: Only appenders created by this RoutingAppender are eligible for deletion.
        final CreatedRouteAppenderControl control = createdAppenders.remove(key);
        if (null != control) {
            LOGGER.debug("Stopping route with {} key", key);
            // Synchronize with getControl to avoid triggering stopAppender before RouteAppenderControl.checkout
            // can be invoked.
            synchronized (this) {
                control.pendingDeletion = true;
            }
            // Don't attempt to stop the appender in a synchronized block, since it may block flushing events
            // to disk.
            control.tryStopAppender();
        } else if (referencedAppenders.containsKey(key)) {
            LOGGER.debug(
                    "Route {} using an appender reference may not be removed because "
                            + "the appender may be used outside of the RoutingAppender",
                    key);
        } else {
            LOGGER.debug("Route with {} key already deleted", key);
        }
    }

    /**
     * Creates a RoutingAppender.
     * @param name The name of the Appender.
     * @param ignore If {@code "true"} (default) exceptions encountered when appending events are logged; otherwise
     *               they are propagated to the caller.
     * @param routes The routing definitions.
     * @param config The Configuration (automatically added by the Configuration).
     * @param rewritePolicy A RewritePolicy, if any.
     * @param filter A Filter to restrict events processed by the Appender or null.
     * @return The RoutingAppender
     * @deprecated Since 2.7; use {@link #newBuilder()}
     */
    @Deprecated
    public static RoutingAppender createAppender(
            final String name,
            final String ignore,
            final Routes routes,
            final Configuration config,
            final RewritePolicy rewritePolicy,
            final PurgePolicy purgePolicy,
            final Filter filter) {

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        if (name == null) {
            LOGGER.error("No name provided for RoutingAppender");
            return null;
        }
        if (routes == null) {
            LOGGER.error("No routes defined for RoutingAppender");
            return null;
        }
        return new RoutingAppender(
                name, filter, ignoreExceptions, routes, rewritePolicy, config, purgePolicy, null, null);
    }

    public Route getDefaultRoute() {
        return defaultRoute;
    }

    public AbstractScript getDefaultRouteScript() {
        return defaultRouteScript;
    }

    public PurgePolicy getPurgePolicy() {
        return purgePolicy;
    }

    public RewritePolicy getRewritePolicy() {
        return rewritePolicy;
    }

    public Routes getRoutes() {
        return routes;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ConcurrentMap<Object, Object> getScriptStaticVariables() {
        return scriptStaticVariables;
    }

    /**
     * LOG4J2-2629: PurgePolicy implementations can invoke {@link #deleteAppender(String)} after we have looked up
     * an instance of a target appender but before events are appended, which could result in events not being
     * recorded to any appender.
     * This extension of {@link AppenderControl} allows to mark usage of an appender, allowing deferral of
     * {@link Appender#stop()} until events have successfully been recorded.
     * Alternative approaches considered:
     * - More aggressive synchronization: Appenders may do expensive I/O that shouldn't block routing.
     * - Move the 'updatePurgePolicy' invocation before appenders are called: Unfortunately this approach doesn't work
     *   if we consider an ImmediatePurgePolicy (or IdlePurgePolicy with a very small timeout) because it may attempt
     *   to remove an appender that doesn't exist yet. It's counterintuitive to get an event that a route has been
     *   used at a point when we expect the route doesn't exist in {@link #getAppenders()}.
     */
    private abstract static class RouteAppenderControl extends AppenderControl {

        RouteAppenderControl(final Appender appender) {
            super(appender, null, null);
        }

        abstract void checkout();

        abstract void release();
    }

    private static final class CreatedRouteAppenderControl extends RouteAppenderControl {

        private volatile boolean pendingDeletion;
        private final AtomicInteger depth = new AtomicInteger();

        CreatedRouteAppenderControl(final Appender appender) {
            super(appender);
        }

        @Override
        void checkout() {
            if (pendingDeletion) {
                LOGGER.warn("CreatedRouteAppenderControl.checkout invoked on a "
                        + "RouteAppenderControl that is pending deletion");
            }
            depth.incrementAndGet();
        }

        @Override
        void release() {
            depth.decrementAndGet();
            tryStopAppender();
        }

        void tryStopAppender() {
            if (pendingDeletion
                    // Only attempt to stop the appender if we can CaS the depth away from zero, otherwise either
                    // 1. Another invocation of tryStopAppender has succeeded, or
                    // 2. Events are being appended, and will trigger stop when they complete
                    && depth.compareAndSet(0, -100_000)) {
                final Appender appender = getAppender();
                LOGGER.debug("Stopping appender {}", appender);
                appender.stop();
            }
        }
    }

    private static final class ReferencedRouteAppenderControl extends RouteAppenderControl {

        ReferencedRouteAppenderControl(final Appender appender) {
            super(appender);
        }

        @Override
        void checkout() {
            // nop
        }

        @Override
        void release() {
            // nop
        }
    }
}
