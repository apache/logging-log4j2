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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.logging.log4j.core.net.WireMockUtil.createMapping;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.test.TestProperties;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingTestProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit test for simple App.
 */
@SetTestProperty(key = "log4j2.configurationAllowedProtocols", value = "http,https")
@SetTestProperty(key = "log4j2.configurationPassword", value = "log4j")
@SetTestProperty(key = "log4j2.configurationUsername", value = "log4j")
@UsingTestProperties
@WireMockTest
class MutableThreadContextMapFilterTest implements MutableThreadContextMapFilter.FilterConfigUpdateListener {

    private static final BasicCredentials CREDENTIALS = new BasicCredentials("log4j", "log4j");
    private static final String FILE_NAME = "testConfig.json";
    private static final String URL_PATH = "/" + FILE_NAME;
    private static final String JSON = "application/json";

    private static final byte[] EMPTY_CONFIG = ("{" //
                    + "  \"configs\":{}" //
                    + "}")
            .getBytes(UTF_8);
    private static final byte[] FILTER_CONFIG = ("{" //
                    + "  \"configs\": {" //
                    + "    \"loginId\": [\"rgoers\", \"adam\"]," //
                    + "    \"corpAcctNumber\": [\"30510263\"]" //
                    + "  }" //
                    + "}")
            .getBytes(UTF_8);

    private static final String CONFIG = "filter/MutableThreadContextMapFilterTest.xml";
    private static LoggerContext loggerContext = null;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition filterUpdated = lock.newCondition();
    private final Condition resultVerified = lock.newCondition();
    private Exception exception;

    @AfterEach
    void cleanup() {
        exception = null;
        ThreadContext.clearMap();
        if (loggerContext != null) {
            loggerContext.stop();
            loggerContext = null;
        }
    }

    @Test
    void file_location_works(TestProperties properties, @TempDir Path dir) throws Exception {
        // Set up the test file.
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant before = now.minus(1, ChronoUnit.MINUTES);
        Instant after = now.plus(1, ChronoUnit.MINUTES);
        Path testConfig = dir.resolve("testConfig.json");
        properties.setProperty("configLocation", testConfig.toString());
        try (final InputStream inputStream = new ByteArrayInputStream(EMPTY_CONFIG)) {
            Files.copy(inputStream, testConfig);
            Files.setLastModifiedTime(testConfig, FileTime.from(before));
        }
        // Setup Log4j
        ConfigurationSource source =
                ConfigurationSource.fromResource(CONFIG, getClass().getClassLoader());
        Configuration configuration = ConfigurationFactory.getInstance().getConfiguration(null, source);
        configuration.initialize(); // To create the components
        final ListAppender app = configuration.getAppender("LIST");
        assertThat(app).isNotNull();
        final MutableThreadContextMapFilter filter = (MutableThreadContextMapFilter) configuration.getFilter();
        assertNotNull(filter);
        filter.registerListener(this);

        lock.lock();
        try {
            // Starts the configuration
            loggerContext = Configurator.initialize(getClass().getClassLoader(), configuration);
            assertNotNull(loggerContext);

            final Logger logger = loggerContext.getLogger(MutableThreadContextMapFilterTest.class);

            assertThat(filterUpdated.await(20, TimeUnit.SECONDS))
                    .as("Initial configuration was loaded")
                    .isTrue();
            ThreadContext.put("loginId", "rgoers");
            logger.debug("This is a test");
            assertThat(app.getEvents()).isEmpty();

            // Prepare the second test case: updated config
            try (final InputStream inputStream = new ByteArrayInputStream(FILTER_CONFIG)) {
                Files.copy(inputStream, testConfig, StandardCopyOption.REPLACE_EXISTING);
                Files.setLastModifiedTime(testConfig, FileTime.from(after));
            }
            resultVerified.signalAll();

            assertThat(filterUpdated.await(20, TimeUnit.SECONDS))
                    .as("Updated configuration was loaded")
                    .isTrue();
            logger.debug("This is a test");
            assertThat(app.getEvents()).hasSize(1);

            // Prepare the third test case: removed config
            Files.delete(testConfig);
            resultVerified.signalAll();

            assertThat(filterUpdated.await(20, TimeUnit.SECONDS))
                    .as("Configuration removal was detected")
                    .isTrue();
            logger.debug("This is a test");
            assertThat(app.getEvents()).hasSize(1);
            resultVerified.signalAll();
        } finally {
            lock.unlock();
        }
        assertThat(exception).as("Asynchronous exception").isNull();
    }

    @Test
    void http_location_works(TestProperties properties, WireMockRuntimeInfo info) throws Exception {
        WireMock wireMock = info.getWireMock();
        // Setup WireMock
        // The HTTP Last-Modified header has a precision of 1 second
        ZonedDateTime now = LocalDateTime.now().atZone(ZoneOffset.UTC);
        ZonedDateTime before = now.minusMinutes(1);
        ZonedDateTime after = now.plusMinutes(1);
        properties.setProperty("configLocation", info.getHttpBaseUrl() + URL_PATH);
        // Setup Log4j
        ConfigurationSource source =
                ConfigurationSource.fromResource(CONFIG, getClass().getClassLoader());
        Configuration configuration = ConfigurationFactory.getInstance().getConfiguration(null, source);
        configuration.initialize(); // To create the components
        final ListAppender app = configuration.getAppender("LIST");
        assertThat(app).isNotNull();
        final MutableThreadContextMapFilter filter = (MutableThreadContextMapFilter) configuration.getFilter();
        assertNotNull(filter);
        filter.registerListener(this);
        lock.lock();
        try {
            // Prepare the first test case: original empty config
            wireMock.importStubMappings(createMapping(URL_PATH, CREDENTIALS, EMPTY_CONFIG, JSON, before));
            // Starts the configuration
            loggerContext = Configurator.initialize(getClass().getClassLoader(), configuration);
            assertNotNull(loggerContext);

            final Logger logger = loggerContext.getLogger(MutableThreadContextMapFilterTest.class);

            assertThat(filterUpdated.await(2, TimeUnit.SECONDS))
                    .as("Initial configuration was loaded")
                    .isTrue();
            ThreadContext.put("loginId", "rgoers");
            logger.debug("This is a test");
            assertThat(app.getEvents()).isEmpty();

            // Prepare the second test case: updated config
            wireMock.removeMappings();
            wireMock.importStubMappings(createMapping(URL_PATH, CREDENTIALS, FILTER_CONFIG, JSON, after));
            resultVerified.signalAll();

            assertThat(filterUpdated.await(2, TimeUnit.SECONDS))
                    .as("Updated configuration was loaded")
                    .isTrue();
            logger.debug("This is a test");
            assertThat(app.getEvents()).hasSize(1);

            // Prepare the third test case: removed config
            wireMock.removeMappings();
            resultVerified.signalAll();

            assertThat(filterUpdated.await(2, TimeUnit.SECONDS))
                    .as("Configuration removal was detected")
                    .isTrue();
            logger.debug("This is a test");
            assertThat(app.getEvents()).hasSize(1);
            resultVerified.signalAll();
        } finally {
            lock.unlock();
        }
        assertThat(exception).as("Asynchronous exception").isNull();
    }

    @Override
    public void onEvent() {
        lock.lock();
        try {
            filterUpdated.signalAll();
            resultVerified.await();
        } catch (final InterruptedException e) {
            exception = e;
        } finally {
            lock.unlock();
        }
    }
}
