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

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import com.code_intelligence.jazzer.api.FuzzerSecurityIssueCritical;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.util.Strings;

final class FuzzingUtil {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private static final int MAX_STRING_LENGTH = 30;

    static void assertValidJson(final String json) {
        final byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        assertValidJson(jsonBytes);
    }

    static void assertValidJson(final byte[] jsonBytes) {
        // We deliberately use an external library instead of using `JsonReader` from Log4j.
        // This is to fuzz Log4j components in isolation.
        try {
            OBJECT_MAPPER.readTree(jsonBytes);
        } catch (final Exception error) {
            throw new FuzzerSecurityIssueCritical("malformed JSON output", error);
        }
    }

    static LoggerContext createLayoutFuzzingLoggerContext(
            final String appenderPluginName,
            final Function<ConfigurationBuilder<?>, LayoutComponentBuilder> layoutSupplier) {

        // Create the configuration builder
        final ConfigurationBuilder<BuiltConfiguration> configBuilder =
                ConfigurationBuilderFactory.newConfigurationBuilder();

        // Create the appender configuration
        final String appenderName = "fuzzingAppender";
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

        // Initialize the configuration
        return Configurator.initialize(config);
    }

    static void logWithoutParams(final Logger logger, final FuzzedDataProvider dataProvider) {
        final String message = dataProvider.consumeString(MAX_STRING_LENGTH);
        final Throwable throwable = createThrowableLogParam(dataProvider);
        if (throwable != null) {
            logger.error(message, throwable);
        } else {
            logger.error(message);
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

    static void logWithParams(final Logger logger, final FuzzedDataProvider dataProvider) {
        final int paramCount = dataProvider.consumeInt(1, 3);
        final String message = Strings.repeat("{}", paramCount);
        final Object[] params = IntStream.range(0, paramCount)
                .mapToObj(ignored -> createLogParam(dataProvider, true))
                .toArray(Object[]::new);
        final Throwable throwable = createThrowableLogParam(dataProvider);
        if (throwable != null) {
            logger.error(message, params, throwable);
        } else {
            logger.error(message, params);
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
