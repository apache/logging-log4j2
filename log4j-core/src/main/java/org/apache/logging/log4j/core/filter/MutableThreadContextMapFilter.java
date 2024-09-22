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
package org.apache.logging.log4j.core.filter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationException;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.filter.mutable.KeyValuePairConfig;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.core.util.internal.HttpInputStreamUtil;
import org.apache.logging.log4j.core.util.internal.LastModifiedSource;
import org.apache.logging.log4j.core.util.internal.Status;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.PropertiesUtil;

/**
 * Filter based on a value in the Thread Context Map (MDC).
 */
@Plugin(
        name = "MutableThreadContextMapFilter",
        category = Node.CATEGORY,
        elementType = Filter.ELEMENT_TYPE,
        printObject = true)
@PluginAliases("MutableContextMapFilter")
@PerformanceSensitive("allocation")
public class MutableThreadContextMapFilter extends AbstractFilter {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final KeyValuePair[] EMPTY_ARRAY = {};

    private volatile Filter filter;
    private final long pollInterval;
    private final ConfigurationScheduler scheduler;
    private final LastModifiedSource source;
    private final AuthorizationProvider authorizationProvider;
    private final List<FilterConfigUpdateListener> listeners = new ArrayList<>();
    private ScheduledFuture<?> future = null;

    private MutableThreadContextMapFilter(
            final Filter filter,
            final LastModifiedSource source,
            final long pollInterval,
            final AuthorizationProvider authorizationProvider,
            final Result onMatch,
            final Result onMismatch,
            final Configuration configuration) {
        super(onMatch, onMismatch);
        this.filter = filter;
        this.pollInterval = pollInterval;
        this.source = source;
        this.scheduler = configuration.getScheduler();
        this.authorizationProvider = authorizationProvider;
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
    public boolean stop(final long timeout, final TimeUnit timeUnit) {
        future.cancel(true);
        return super.stop(timeout, timeUnit);
    }

    public void registerListener(final FilterConfigUpdateListener listener) {
        listeners.add(listener);
    }

    @PluginBuilderFactory
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public Result filter(final LogEvent event) {
        return filter.filter(event);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
        return filter.filter(logger, level, marker, msg, t);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
        return filter.filter(logger, level, marker, msg, t);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
        return filter.filter(logger, level, marker, msg, params);
    }

    @Override
    public Result filter(
            final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
        return filter.filter(logger, level, marker, msg, p0);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1) {
        return filter.filter(logger, level, marker, msg, p0, p1);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8);
    }

    @Override
    public Result filter(
            final Logger logger,
            final Level level,
            final Marker marker,
            final String msg,
            final Object p0,
            final Object p1,
            final Object p2,
            final Object p3,
            final Object p4,
            final Object p5,
            final Object p6,
            final Object p7,
            final Object p8,
            final Object p9) {
        return filter.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
    }

    public static class Builder extends AbstractFilterBuilder<Builder>
            implements org.apache.logging.log4j.core.util.Builder<MutableThreadContextMapFilter> {
        @PluginBuilderAttribute
        private String configLocation;

        @PluginBuilderAttribute
        private long pollInterval;

        @PluginConfiguration
        private Configuration configuration;

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

        @Override
        public MutableThreadContextMapFilter build() {
            final LastModifiedSource source = getSource(configLocation);
            if (source == null) {
                return new MutableThreadContextMapFilter(
                        new NoOpFilter(), null, 0, null, getOnMatch(), getOnMismatch(), configuration);
            }
            final AuthorizationProvider authorizationProvider =
                    ConfigurationFactory.authorizationProvider(PropertiesUtil.getProperties());
            Filter filter;
            if (pollInterval <= 0) {
                final ConfigResult result = getConfig(source, authorizationProvider);
                if (result.status == Status.SUCCESS) {
                    if (result.pairs.length > 0) {
                        filter = ThreadContextMapFilter.createFilter(result.pairs, "or", getOnMatch(), getOnMismatch());
                    } else {
                        filter = new NoOpFilter();
                    }
                } else if (result.status == Status.NOT_FOUND || result.status == Status.EMPTY) {
                    filter = new NoOpFilter();
                } else {
                    LOGGER.warn("Unexpected response returned on initial call: {}", result.status);
                    filter = new NoOpFilter();
                }
            } else {
                filter = new NoOpFilter();
            }

            if (pollInterval > 0) {
                configuration.getScheduler().incrementScheduledItems();
            }
            return new MutableThreadContextMapFilter(
                    filter, source, pollInterval, authorizationProvider, getOnMatch(), getOnMismatch(), configuration);
        }
    }

    private class FileMonitor implements Runnable {

        @Override
        public void run() {
            final ConfigResult result = getConfig(source, authorizationProvider);
            if (result.status == Status.SUCCESS) {
                filter = ThreadContextMapFilter.createFilter(result.pairs, "or", getOnMatch(), getOnMismatch());
                LOGGER.info("Filter configuration was updated: {}", filter.toString());
                for (FilterConfigUpdateListener listener : listeners) {
                    listener.onEvent();
                }
            } else if (result.status == Status.NOT_FOUND) {
                if (!(filter instanceof NoOpFilter)) {
                    LOGGER.info("Filter configuration was removed");
                    filter = new NoOpFilter();
                    for (FilterConfigUpdateListener listener : listeners) {
                        listener.onEvent();
                    }
                }
            } else if (result.status == Status.EMPTY) {
                LOGGER.debug("Filter configuration is empty");
                filter = new NoOpFilter();
            }
        }
    }

    @SuppressFBWarnings(
            value = "PATH_TRAVERSAL_IN",
            justification = "The location of the file comes from a configuration value.")
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

    private static ConfigResult getConfig(
            final LastModifiedSource source, final AuthorizationProvider authorizationProvider) {
        final File inputFile = source.getFile();
        InputStream inputStream = null;
        HttpInputStreamUtil.Result result = null;
        final long lastModified = source.getLastModified();
        if (inputFile != null && inputFile.exists()) {
            try {
                final long modified = inputFile.lastModified();
                if (modified > lastModified) {
                    source.setLastModified(modified);
                    inputStream = new FileInputStream(inputFile);
                    result = new HttpInputStreamUtil.Result(Status.SUCCESS);
                } else {
                    result = new HttpInputStreamUtil.Result(Status.NOT_MODIFIED);
                }
            } catch (Exception ex) {
                result = new HttpInputStreamUtil.Result(Status.ERROR);
            }
        } else if (source.getURI() != null) {
            try {
                result = HttpInputStreamUtil.getInputStream(source, authorizationProvider);
                inputStream = result.getInputStream();
            } catch (ConfigurationException ex) {
                result = new HttpInputStreamUtil.Result(Status.ERROR);
            }
        } else {
            result = new HttpInputStreamUtil.Result(Status.NOT_FOUND);
        }
        final ConfigResult configResult = new ConfigResult();
        if (result.getStatus() == Status.SUCCESS) {
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
                            configResult.pairs = pairs.toArray(EMPTY_ARRAY);
                            configResult.status = Status.SUCCESS;
                        } else {
                            configResult.status = Status.EMPTY;
                        }
                    } else {
                        LOGGER.debug("No configuration data in {}", source.toString());
                        configResult.status = Status.EMPTY;
                    }
                } else {
                    LOGGER.warn("No configs element in MutableThreadContextMapFilter configuration");
                    configResult.status = Status.ERROR;
                }
            } catch (Exception ex) {
                LOGGER.warn("Invalid key/value pair configuration, input ignored: {}", ex.getMessage());
                configResult.status = Status.ERROR;
            }
        } else {
            configResult.status = result.getStatus();
        }
        return configResult;
    }

    private static class NoOpFilter extends AbstractFilter {

        public NoOpFilter() {
            super(Result.NEUTRAL, Result.NEUTRAL);
        }
    }

    public interface FilterConfigUpdateListener {
        void onEvent();
    }

    private static class ConfigResult extends HttpInputStreamUtil.Result {
        public KeyValuePair[] pairs;
        public Status status;
    }
}
