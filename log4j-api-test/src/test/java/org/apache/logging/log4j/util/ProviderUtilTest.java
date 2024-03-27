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
package org.apache.logging.log4j.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.logging.log4j.TestProvider;
import org.apache.logging.log4j.spi.Provider;
import org.apache.logging.log4j.test.TestLogger;
import org.apache.logging.log4j.test.TestLoggerContextFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@Execution(ExecutionMode.CONCURRENT)
class ProviderUtilTest {

    private static final Pattern ERROR_OR_WARNING = Pattern.compile(" (ERROR|WARN) .*", Pattern.DOTALL);

    private static final Provider LOCAL_PROVIDER = new LocalProvider();
    private static final Provider TEST_PROVIDER = new TestProvider();

    private static final Collection<Provider> NO_PROVIDERS = Collections.emptyList();
    private static final Collection<Provider> ONE_PROVIDER = Collections.singleton(TEST_PROVIDER);
    private static final Collection<Provider> TWO_PROVIDERS = Arrays.asList(LOCAL_PROVIDER, TEST_PROVIDER);

    private TestLogger statusLogger;

    @BeforeEach
    void setup() {
        statusLogger = new TestLogger();
    }

    @Test
    void should_have_a_fallback_provider() {
        final PropertiesUtil properties = new PropertiesUtil(new Properties());
        assertThat(ProviderUtil.selectProvider(properties, NO_PROVIDERS, statusLogger))
                .as("check selected provider")
                .isNotNull();
        // An error for the absence of providers
        assertHasErrorOrWarning(statusLogger);
    }

    @Test
    void should_be_silent_with_a_single_provider() {
        final PropertiesUtil properties = new PropertiesUtil(new Properties());
        assertThat(ProviderUtil.selectProvider(properties, ONE_PROVIDER, statusLogger))
                .as("check selected provider")
                .isSameAs(TEST_PROVIDER);
        assertNoErrorsOrWarnings(statusLogger);
    }

    @Test
    void should_select_provider_with_highest_priority() {
        final PropertiesUtil properties = new PropertiesUtil(new Properties());
        assertThat(ProviderUtil.selectProvider(properties, TWO_PROVIDERS, statusLogger))
                .as("check selected provider")
                .isSameAs(TEST_PROVIDER);
        // A warning for the presence of multiple providers
        assertHasErrorOrWarning(statusLogger);
    }

    @Test
    void should_recognize_log4j_provider_property() {
        final Properties map = new Properties();
        map.setProperty("log4j.provider", LocalProvider.class.getName());
        final PropertiesUtil properties = new PropertiesUtil(map);
        assertThat(ProviderUtil.selectProvider(properties, TWO_PROVIDERS, statusLogger))
                .as("check selected provider")
                .isInstanceOf(LocalProvider.class);
        assertNoErrorsOrWarnings(statusLogger);
    }

    /**
     * Can be removed in the future.
     */
    @Test
    void should_recognize_log4j_factory_property() {
        final Properties map = new Properties();
        map.setProperty("log4j2.loggerContextFactory", LocalLoggerContextFactory.class.getName());
        final PropertiesUtil properties = new PropertiesUtil(map);
        assertThat(ProviderUtil.selectProvider(properties, TWO_PROVIDERS, statusLogger)
                        .getLoggerContextFactory())
                .as("check selected logger context factory")
                .isInstanceOf(LocalLoggerContextFactory.class);
        // Deprecation warning
        assertHasErrorOrWarning(statusLogger);
    }

    /**
     * Can be removed in the future.
     */
    @Test
    void log4j_provider_property_has_priority() {
        final Properties map = new Properties();
        map.setProperty("log4j.provider", LocalProvider.class.getName());
        map.setProperty("log4j2.loggerContextFactory", TestLoggerContextFactory.class.getName());
        final PropertiesUtil properties = new PropertiesUtil(map);
        assertThat(ProviderUtil.selectProvider(properties, TWO_PROVIDERS, statusLogger))
                .as("check selected provider")
                .isInstanceOf(LocalProvider.class);
        // Warning
        assertHasErrorOrWarning(statusLogger);
    }

    static Stream<Arguments> incorrect_configuration_do_not_throw() {
        return Stream.of(
                Arguments.of("java.lang.String", null),
                Arguments.of("non.existent.Provider", null),
                // logger context factory without a matching provider
                Arguments.of(null, "org.apache.logging.log4j.util.ProviderUtilTest$LocalLoggerContextFactory"),
                Arguments.of(null, "java.lang.String"),
                Arguments.of(null, "non.existent.LoggerContextFactory"));
    }

    @ParameterizedTest
    @MethodSource
    void incorrect_configuration_do_not_throw(final String provider, final String contextFactory) {
        final Properties map = new Properties();
        if (provider != null) {
            map.setProperty("log4j.provider", provider);
        }
        if (contextFactory != null) {
            map.setProperty("log4j2.loggerContextFactory", contextFactory);
        }
        final PropertiesUtil properties = new PropertiesUtil(map);
        assertThat(ProviderUtil.selectProvider(properties, ONE_PROVIDER, statusLogger))
                .as("check selected provider")
                .isNotNull();
        // Warnings will be present
        assertHasErrorOrWarning(statusLogger);
    }

    public static class LocalLoggerContextFactory extends TestLoggerContextFactory {}

    /**
     * A provider with a smaller priority than {@link org.apache.logging.log4j.TestProvider}.
     */
    public static class LocalProvider extends org.apache.logging.log4j.spi.Provider {
        public LocalProvider() {
            super(0, CURRENT_VERSION, LocalLoggerContextFactory.class);
        }
    }

    private void assertHasErrorOrWarning(final TestLogger statusLogger) {
        assertThat(statusLogger.getEntries()).as("check StatusLogger entries").anySatisfy(entry -> assertThat(entry)
                .matches(ERROR_OR_WARNING));
    }

    private void assertNoErrorsOrWarnings(final TestLogger statusLogger) {
        assertThat(statusLogger.getEntries()).as("check StatusLogger entries").allSatisfy(entry -> assertThat(entry)
                .doesNotMatch(ERROR_OR_WARNING));
    }
}
