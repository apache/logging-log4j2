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
package org.apache.logging.log4j.fuzz;

import static java.util.Objects.requireNonNull;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.spi.ExtendedLogger;
import org.apache.logging.log4j.util.Strings;
import org.jspecify.annotations.Nullable;

public final class FuzzingUtil {

    private static final int MAX_STRING_LENGTH = 30;

    public static LoggerContext createLoggerContext(
            final String loggerContextName,
            final String appenderPluginName,
            final Function<ConfigurationBuilder<?>, LayoutComponentBuilder> layoutSupplier) {

        // Create the configuration builder
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder();

        // Create the appender configuration
        final String appenderName = "FUZZING_APPENDER";
        final LayoutComponentBuilder layoutComponentBuilder = layoutSupplier.apply(configBuilder);
        final AppenderComponentBuilder appenderComponentBuilder =
                configBuilder.newAppender(appenderName, appenderPluginName).add(layoutComponentBuilder);

        // Create the root logger configuration
        final RootLoggerComponentBuilder loggerComponentBuilder =
                configBuilder.newRootLogger(Level.ALL).add(configBuilder.newAppenderRef(appenderName));

        // Create the conclusive configuration (uninitialized!)
        final Configuration config = configBuilder
                .setStatusLevel(Level.OFF)
                .add(appenderComponentBuilder)
                .add(loggerComponentBuilder)
                .build(false);

        // Create the logger context
        final LoggerContext loggerContext = new LoggerContext(loggerContextName);
        loggerContext.start(config);
        return loggerContext;
    }

    /**
     * Contract modelling an API-agnostic logger.
     * This allows {@link #fuzzLogger(LoggerFacade, FuzzedDataProvider)} to accept all logger implementations; of Log4j API, of SLF4J, etc.
     */
    public interface LoggerFacade {

        void log(String message);

        void log(String message, Throwable throwable);

        void log(String message, Object[] parameters);
    }

    public static final class Log4jLoggerFacade implements LoggerFacade {

        private static final String FQCN = Log4jLoggerFacade.class.getName();

        private final ExtendedLogger logger;

        public Log4jLoggerFacade(final ExtendedLogger logger) {
            this.logger = requireNonNull(logger, "logger");
        }

        @Override
        public void log(final String message) {
            logger.logIfEnabled(FQCN, Level.ERROR, null, message);
        }

        @Override
        public void log(final String message, final Throwable throwable) {
            logger.logIfEnabled(FQCN, Level.ERROR, null, message, throwable);
        }

        @Override
        public void log(final String message, final Object[] parameters) {
            logger.logIfEnabled(FQCN, Level.ERROR, null, message, parameters);
        }
    }

    public static void fuzzLogger(final LoggerFacade logger, final FuzzedDataProvider dataProvider) {
        requireNonNull(logger, "logger");
        requireNonNull(dataProvider, "dataProvider");
        final boolean parameterized = dataProvider.consumeBoolean();
        if (parameterized) {
            fuzzLoggerWithParams(logger, dataProvider);
        } else {
            fuzzLoggerWithoutParams(logger, dataProvider);
        }
    }

    private static void fuzzLoggerWithoutParams(final LoggerFacade logger, final FuzzedDataProvider dataProvider) {
        final String message = dataProvider.consumeString(MAX_STRING_LENGTH);
        final Throwable throwable = createThrowableLogParam(dataProvider);
        if (throwable != null) {
            logger.log(message, throwable);
        } else {
            logger.log(message);
        }
    }

    @Nullable
    private static Throwable createThrowableLogParam(final FuzzedDataProvider dataProvider) {
        final boolean enabled = dataProvider.consumeBoolean();
        if (!enabled) {
            return null;
        }
        final String errorMessage = dataProvider.consumeString(MAX_STRING_LENGTH);
        return new Exception(errorMessage);
    }

    private static void fuzzLoggerWithParams(final LoggerFacade logger, final FuzzedDataProvider dataProvider) {
        final int paramCount = dataProvider.consumeInt(1, 3);
        final String message = Strings.repeat("{}", paramCount);
        final Throwable throwable = createThrowableLogParam(dataProvider);
        final Object[] params = IntStream.range(0, paramCount + (throwable != null ? 1 : 0))
                .mapToObj(ignored -> createLogParam(dataProvider, true))
                .toArray(Object[]::new);
        if (throwable != null) {
            params[paramCount] = throwable;
            logger.log(message, params);
        } else {
            logger.log(message, params);
        }
    }

    private static Object createLogParam(final FuzzedDataProvider dataProvider, final boolean nestingAllowed) {
        final boolean nested = nestingAllowed && dataProvider.consumeBoolean();
        final int paramTypeCode = dataProvider.consumeInt(0, nested ? 9 : 8);
        switch (paramTypeCode) {
            case 0:
                return dataProvider.consumeByte();
            case 1:
                return dataProvider.consumeBoolean();
            case 2:
                return dataProvider.consumeChar();
            case 3:
                return dataProvider.consumeShort();
            case 4:
                return dataProvider.consumeInt();
            case 5:
                return dataProvider.consumeLong();
            case 6:
                return dataProvider.consumeFloat();
            case 7:
                return dataProvider.consumeDouble();
            case 8:
                return dataProvider.consumeString(MAX_STRING_LENGTH);
            default:
                return createNestedLogParam(dataProvider);
        }
    }

    private static Object createNestedLogParam(final FuzzedDataProvider dataProvider) {
        final int nestLength = dataProvider.consumeInt(0, 3);
        final Stream<Object> nestItems =
                IntStream.range(0, nestLength).mapToObj(ignored -> createLogParam(dataProvider, false));
        final boolean arrayTyped = dataProvider.consumeBoolean();
        return arrayTyped ? nestItems.toArray(Object[]::new) : nestItems.collect(Collectors.toList());
    }
}
