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
package org.apache.logging.log4j.core.filter;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.filter.mutable.KeyValuePairConfig;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.internal.HttpResponse;
import org.apache.logging.log4j.core.util.internal.HttpSourceLoader;
import org.apache.logging.log4j.core.util.internal.LastModifiedSource;
import org.apache.logging.log4j.core.util.internal.Status;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PerformanceSensitive;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Filter based on a value in the Thread Context Map (MDC).
 */
@Configurable(elementType = Filter.ELEMENT_TYPE, printObject = true)
@Plugin
@PluginAliases("MutableContextMapFilter")
@PerformanceSensitive("allocation")
public class MutableThreadContextMapFilter extends AbstractFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final KeyValuePair[] EMPTY_ARRAY = {};

    private volatile Filter filter;
    private final long pollInterval;
    private final ConfigurationScheduler scheduler;
    private final LastModifiedSource source;
    private final List<FilterConfigUpdateListener> listeners = new ArrayList<>();
    private ScheduledFuture<?> future = null;
    private final ContextDataInjector contextDataInjector;
    private final ContextDataFactory contextDataFactory;
    private final HttpSourceLoader httpSourceLoader;

    private MutableThreadContextMapFilter(final Filter filter,
                                          final LastModifiedSource source,
                                          final long pollInterval,
                                          final Result onMatch,
                                          final Result onMismatch,
                                          final Configuration configuration,
                                          final ContextDataInjector contextDataInjector,
                                          final ContextDataFactory contextDataFactory,
                                          final HttpSourceLoader httpSourceLoader) {
        super(onMatch, onMismatch);
        this.filter = filter;
        this.pollInterval = pollInterval;
        this.source = source;
        this.scheduler = configuration.getScheduler();
        this.contextDataInjector = contextDataInjector;
        this.contextDataFactory = contextDataFactory;
        this.httpSourceLoader = httpSourceLoader;
    }

    @Override
    public void start() {

        if (pollInterval > 0) {
            future = scheduler.scheduleWithFixedDelay(new FileMonitor(), 0, pollInterval, TimeUnit.SECONDS);
            LOGGER.debug("Watching {} with poll interval {}", source.toString(), pollInterval);
        }
        super.start();
    }

    @Override
    public boolean stop(long timeout, TimeUnit timeUnit) {
        future.cancel(true);
        return super.stop(timeout, timeUnit);
    }

    public void registerListener(FilterConfigUpdateListener listener) {
        listeners.add(listener);
    }

    @PluginFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Result filter(LogEvent event) {
        return filter.filter(event);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        return filter.filter(logger, level, marker, msg, t);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return filter.filter(logger, level, marker, msg, t);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return filter.filter(logger, level, marker, msg, params);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0) {
        return filter.filter(logger, level, marker, msg, p0);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1) {
        return filter.filter(logger, level, marker, msg, p0, p1);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2, Object p3) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2, Object p3, Object p4) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2, Object p3, Object p4, Object p5) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2, Object p3, Object p4, Object p5, Object p6) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2, Object p3, Object p4, Object p5, Object p6, Object p7) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object p0,
        Object p1,
        Object p2, Object p3, Object p4, Object p5, Object p6, Object p7, Object p8, Object p9) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    public static class Builder extends AbstractFilterBuilder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<MutableThreadContextMapFilter> {
        @PluginAttribute
        private String configLocation;

        @PluginAttribute
        private long pollInterval;

        @PluginConfiguration
        private Configuration configuration;

        private ContextDataInjector contextDataInjector;

        private ContextDataFactory contextDataFactory;

        // HttpSourceLoader is not exported, so let's keep it hidden
        @Inject
        private HttpSourceLoader httpSourceLoader;

        /**
         * Sets the Configuration.
         * @param configuration The Configuration.
         * @return this.
         */
        public Builder setConfiguration(final Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * Set the frequency in seconds that changes to the list a ThreadContext valudes should be
         * checked.
         * @param pollInterval interval in seconds to check the file for changes.
         * @return this.
         */
        public Builder setPollInterval(final long pollInterval) {
            this.pollInterval = pollInterval;
            return this;
        }

        /**
         * Sets the configuration to use.
         * @param configLocation the location of the configuration.
         * @return this
         */
        public Builder setConfigLocation(final String configLocation) {
            this.configLocation = configLocation;
            return this;
        }

        @Inject
        public Builder setContextDataInjector(final ContextDataInjector contextDataInjector) {
            this.contextDataInjector = contextDataInjector;
            return this;
        }

        @Inject
        public Builder setContextDataFactory(final ContextDataFactory contextDataFactory) {
            this.contextDataFactory = contextDataFactory;
            return this;
        }

        @Override
        public MutableThreadContextMapFilter build() {
            final LastModifiedSource source = getSource(configLocation);
            if (source == null) {
                return new MutableThreadContextMapFilter(new NoOpFilter(), null, 0,
                        getOnMatch(), getOnMismatch(), configuration, contextDataInjector, contextDataFactory, httpSourceLoader);
            }
            Filter filter;
            if (pollInterval <= 0) {
                ConfigResult result = getConfig(source, httpSourceLoader);
                if (result.getStatus() == Status.SUCCESS) {
                    if (result.pairs.length > 0) {
                        filter = ThreadContextMapFilter.newBuilder()
                                .setPairs(result.pairs)
                                .setOperator("or")
                                .setOnMatch(getOnMatch())
                                .setOnMismatch(getOnMismatch())
                                .setContextDataInjector(contextDataInjector)
                                .setContextDataFactory(contextDataFactory)
                                .get();
                    } else {
                        filter = new NoOpFilter();
                    }
                } else if (result.getStatus() == Status.NOT_FOUND || result.getStatus() == Status.EMPTY) {
                    filter = new NoOpFilter();
                } else {
                    LOGGER.warn("Unexpected response returned on initial call: {}", result.getStatus());
                    filter = new NoOpFilter();
                }
            } else {
                filter = new NoOpFilter();
            }

            if (pollInterval > 0) {
                configuration.getScheduler().incrementScheduledItems();
            }
            return new MutableThreadContextMapFilter(filter, source, pollInterval, getOnMatch(), getOnMismatch(),
                    configuration, contextDataInjector, contextDataFactory, httpSourceLoader);
        }
    }

    private class FileMonitor implements Runnable {

        @Override
        public void run() {
            final ConfigResult result = getConfig(source, httpSourceLoader);
            if (result.getStatus() == Status.SUCCESS) {
                filter = ThreadContextMapFilter.newBuilder()
                        .setPairs(result.pairs)
                        .setOperator("or")
                        .setOnMatch(getOnMatch())
                        .setOnMismatch(getOnMismatch())
                        .setContextDataInjector(contextDataInjector)
                        .setContextDataFactory(contextDataFactory)
                        .get();
                LOGGER.info("Filter configuration was updated: {}", filter.toString());
                for (FilterConfigUpdateListener listener : listeners) {
                    listener.onEvent();
                }
            } else if (result.getStatus() == Status.NOT_FOUND) {
                if (!(filter instanceof NoOpFilter)) {
                    LOGGER.info("Filter configuration was removed");
                    filter = new NoOpFilter();
                    for (FilterConfigUpdateListener listener : listeners) {
                        listener.onEvent();
                    }
                }
            } else if (result.getStatus() == Status.EMPTY) {
                LOGGER.debug("Filter configuration is empty");
                filter = new NoOpFilter();
            }
        }
    }

    private static LastModifiedSource getSource(final String configLocation) {
        LastModifiedSource source = null;
        try {
            final URI uri = new URI(configLocation);
            if (uri.getScheme() != null) {
                source = new LastModifiedSource(new URI(configLocation));
            } else {
                source = new LastModifiedSource(new File(configLocation));
            }

        } catch (Exception ex) {
            source = new LastModifiedSource(new File(configLocation));
        }
        return source;
    }

    private static ConfigResult getConfig(final LastModifiedSource source,
                                          final HttpSourceLoader httpSourceLoader) {
        final File inputFile = source.getFile();
        InputStream inputStream = null;
        HttpResponse response;
        final long lastModified = source.getLastModified();
        if (inputFile != null && inputFile.exists()) {
            try {
                final long modified = inputFile.lastModified();
                if (modified > lastModified) {
                    source.setLastModified(modified);
                    inputStream = new FileInputStream(inputFile);
                    response = new HttpResponse(Status.SUCCESS);
                } else {
                    response = new HttpResponse(Status.NOT_MODIFIED);
                }
            } catch (Exception ex) {
                response = new HttpResponse(Status.ERROR);
            }
        } else if (source.getURI() != null) {
            try {
                response = httpSourceLoader.load(source);
                inputStream = response.getInputStream();
            } catch (ConfigurationException ex) {
                response = new HttpResponse(Status.ERROR);
            }
        } else {
            response = new HttpResponse(Status.NOT_FOUND);
        }
        if (response.getStatus() == Status.SUCCESS) {
            LOGGER.debug("Processing Debug key/value pairs from: {}", source.toString());
            try {
                final KeyValuePairConfig keyValuePairConfig = MAPPER.readValue(inputStream, KeyValuePairConfig.class);
                if (keyValuePairConfig != null) {
                    final Map<String, String[]> configs = keyValuePairConfig.getConfigs();
                    if (configs != null && configs.size() > 0) {
                        final List<KeyValuePair> pairs = new ArrayList<>();
                        for (Map.Entry<String, String[]> entry : configs.entrySet()) {
                            final String key = entry.getKey();
                            for (final String value : entry.getValue()) {
                                if (value != null) {
                                    pairs.add(new KeyValuePair(key, value));
                                } else {
                                    LOGGER.warn("Ignoring null value for {}", key);
                                }
                            }
                        }
                        if (pairs.size() > 0) {
                            return new ConfigResult(Status.SUCCESS, pairs.toArray(EMPTY_ARRAY));
                        }
                        return new ConfigResult(Status.EMPTY);
                    }
                    LOGGER.debug("No configuration data in {}", source.toString());
                    return new ConfigResult(Status.EMPTY);
                }
                LOGGER.warn("No configs element in MutableThreadContextMapFilter configuration");
                return new ConfigResult(Status.ERROR);
            } catch (Exception ex) {
                LOGGER.warn("Invalid key/value pair configuration, input ignored: {}", ex.getMessage());
                return new ConfigResult(Status.ERROR);
            }
        }
        return new ConfigResult(Status.ERROR);
    }

    private static class NoOpFilter extends AbstractFilter {

        public NoOpFilter() {
            super(Result.NEUTRAL, Result.NEUTRAL);
        }
    }

    public interface FilterConfigUpdateListener {
        void onEvent();
    }

    private static class ConfigResult extends HttpResponse {
        private final KeyValuePair[] pairs;

        private ConfigResult(final Status status) {
            super(status);
            this.pairs = EMPTY_ARRAY;
        }

        private ConfigResult(final Status status, final KeyValuePair... pairs) {
            super(status);
            this.pairs = pairs;
        }
    }
}
