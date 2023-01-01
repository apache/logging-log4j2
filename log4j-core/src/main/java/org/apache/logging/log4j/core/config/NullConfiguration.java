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

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.async.AsyncLoggerConfigDelegate;
import org.apache.logging.log4j.core.async.AsyncWaitStrategyFactory;
import org.apache.logging.log4j.core.filter.DenyAllFilter;
import org.apache.logging.log4j.core.lookup.Interpolator;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.script.ScriptManager;
import org.apache.logging.log4j.core.time.NanoClock;
import org.apache.logging.log4j.core.time.internal.DummyNanoClock;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.plugins.ConfigurationScoped;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.di.SimpleScope;
import org.apache.logging.log4j.util.PropertyResolver;

/**
 * This configuration defaults to no logging.
 */
public class NullConfiguration extends AbstractLifeCycle implements Configuration {

    /**
     * Name of this configuration.
     */
    public static final String NULL_NAME = "Null";

    private final WeakReference<LoggerContext> loggerContextRef;
    private final Injector injector;
    private final PropertyResolver propertyResolver;
    private final LoggerConfig rootLoggerConfig;
    private final Filter filter = DenyAllFilter.newBuilder().get();
    private final Advertiser advertiser = new DefaultAdvertiser();
    private final StrSubstitutor strSubstitutor;
    private final NanoClock nanoClock = new DummyNanoClock();

    public NullConfiguration() {
        this(NullLoggerContext.getInstance());
    }

    public NullConfiguration(final LoggerContext loggerContext) {
        loggerContextRef = new WeakReference<>(loggerContext);
        injector = loggerContext.getInjector().copy();
        injector.registerScope(ConfigurationScoped.class, new SimpleScope(() -> "ConfigurationScoped; name=Null"));
        propertyResolver = loggerContext.getPropertyResolver();
        rootLoggerConfig = LoggerConfig.RootLogger.newRootBuilder()
                .setLevel(Level.OFF)
                .setConfig(this)
                .setPropertyResolver(propertyResolver)
                .get();
        final Interpolator interpolator = new Interpolator();
        interpolator.setLoggerContext(loggerContext);
        strSubstitutor = new StrSubstitutor(interpolator);
        strSubstitutor.setConfiguration(this);
    }

    @Override
    public String getName() {
        return NULL_NAME;
    }

    @Override
    public LoggerConfig getLoggerConfig(final String name) {
        return rootLoggerConfig;
    }

    @Override
    public <T extends Appender> T getAppender(final String name) {
        return null;
    }

    @Override
    public Map<String, Appender> getAppenders() {
        return Map.of();
    }

    @Override
    public void addAppender(final Appender appender) {
        // no-op
    }

    @Override
    public Map<String, LoggerConfig> getLoggers() {
        return Map.of(rootLoggerConfig.getName(), rootLoggerConfig);
    }

    @Override
    public void addLoggerAppender(final Logger logger, final Appender appender) {
        // no-op
    }

    @Override
    public void addLoggerFilter(final Logger logger, final Filter filter) {
        // no-op
    }

    @Override
    public void setLoggerAdditive(final Logger logger, final boolean additive) {
        // no-op
    }

    @Override
    public void addLogger(final String name, final LoggerConfig loggerConfig) {
        // no-op
    }

    @Override
    public void removeLogger(final String name) {
        // no-op
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of();
    }

    @Override
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }

    @Override
    public LoggerConfig getRootLogger() {
        return rootLoggerConfig;
    }

    @Override
    public void addListener(final ConfigurationListener listener) {
        // no-op
    }

    @Override
    public void removeListener(final ConfigurationListener listener) {
        // no-op
    }

    @Override
    public StrSubstitutor getStrSubstitutor() {
        return strSubstitutor;
    }

    @Override
    public void createConfiguration(final Node node, final LogEvent event) {
        // no-op
    }

    @Override
    public <T> T getInstance(final Class<T> type) {
        return injector.getInstance(type);
    }

    @Override
    public <T> Optional<T> tryGetInstance(final Class<T> type) {
        return injector.tryGetInstance(type);
    }

    @Override
    public <T> T getInstance(final Key<T> key) {
        return injector.getInstance(key);
    }

    @Override
    public <T> T getComponent(final String name) {
        return null;
    }

    @Override
    public void addComponent(final String name, final Object object) {
        // no-op
    }

    @Override
    public void setAdvertiser(final Advertiser advertiser) {
        // no-op
    }

    @Override
    public Advertiser getAdvertiser() {
        return advertiser;
    }

    @Override
    public boolean isShutdownHookEnabled() {
        return false;
    }

    @Override
    public long getShutdownTimeoutMillis() {
        return 0;
    }

    @Override
    public ConfigurationScheduler getScheduler() {
        return getInstance(ConfigurationScheduler.class);
    }

    @Override
    public ConfigurationSource getConfigurationSource() {
        return ConfigurationSource.NULL_SOURCE;
    }

    @Override
    public List<CustomLevelConfig> getCustomLevels() {
        return List.of();
    }

    @Override
    public ScriptManager getScriptManager() {
        return getInstance(ScriptManager.KEY);
    }

    @Override
    public AsyncLoggerConfigDelegate getAsyncLoggerConfigDelegate() {
        return null;
    }

    @Override
    public AsyncWaitStrategyFactory getAsyncWaitStrategyFactory() {
        return null;
    }

    @Override
    public WatchManager getWatchManager() {
        return getInstance(WatchManager.class);
    }

    @Override
    public NanoClock getNanoClock() {
        return nanoClock;
    }

    @Override
    public void setNanoClock(final NanoClock nanoClock) {
        // no-op
    }

    @Override
    public LoggerContext getLoggerContext() {
        return loggerContextRef.get();
    }

    @Override
    public void addFilter(final Filter filter) {
        // no-op
    }

    @Override
    public void removeFilter(final Filter filter) {
        // no-op
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public boolean hasFilter() {
        return true;
    }

    @Override
    public boolean isFiltered(final LogEvent event) {
        // ignore all log events even if they somehow make it through
        return true;
    }
}
