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
package org.apache.logging.log4j.core.util;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.HttpWatcher;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.junit.jupiter.api.Test;

/**
 * Test the WatchManager
 */
@SetTestProperty(key = UrlConnectionFactory.ALLOWED_PROTOCOLS, value = "http,https")
@WireMockTest
class HttpWatcherTest {

    private static final DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final String XML = "application/xml";

    @Test
    void testModified(final WireMockRuntimeInfo info) throws Exception {
        final WireMock wireMock = info.getWireMock();

        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        List<ConfigurationListener> listeners = singletonList(new TestConfigurationListener(queue, "log4j-test1.xml"));
        // HTTP Last-Modified is in seconds
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant previous = now.minus(5, ChronoUnit.MINUTES);
        final URL url = new URL(info.getHttpBaseUrl() + "/log4j-test1.xml");
        final Configuration configuration = createConfiguration(url);

        final StubMapping stubMapping = wireMock.register(get("/log4j-test1.xml")
                .willReturn(aResponse()
                        .withBodyFile("log4j-test1.xml")
                        .withStatus(200)
                        .withHeader("Last-Modified", formatter.format(previous))
                        .withHeader("Content-Type", XML)));
        Watcher watcher = new HttpWatcher(configuration, null, listeners, previous.toEpochMilli());
        watcher.watching(new Source(url));
        try {
            assertThat(watcher.isModified()).as("File was modified").isTrue();
            assertThat(watcher.getLastModified()).as("File modification time").isEqualTo(previous.toEpochMilli());
            // Check if listeners are correctly called
            // Note: listeners are called asynchronously
            watcher.modified();
            String str = queue.poll(1, TimeUnit.SECONDS);
            assertThat(str).isEqualTo("log4j-test1.xml");
            ConfigurationSource configurationSource = configuration.getConfigurationSource();
            // Check that the last modified time of the ConfigurationSource was modified as well
            // See: https://github.com/apache/logging-log4j2/issues/2937
            assertThat(configurationSource.getLastModified())
                    .as("Last modification time of current ConfigurationSource")
                    .isEqualTo(0L);
            configurationSource = configurationSource.resetInputStream();
            assertThat(configurationSource.getLastModified())
                    .as("Last modification time of next ConfigurationSource")
                    .isEqualTo(previous.toEpochMilli());
        } finally {
            wireMock.removeStubMapping(stubMapping);
        }
    }

    @Test
    void testNotModified(final WireMockRuntimeInfo info) throws Exception {
        final WireMock wireMock = info.getWireMock();

        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        final List<ConfigurationListener> listeners =
                singletonList(new TestConfigurationListener(queue, "log4j-test2.xml"));
        // HTTP Last-Modified is in seconds
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant previous = now.minus(5, ChronoUnit.MINUTES);
        final URL url = new URL(info.getHttpBaseUrl() + "/log4j-test2.xml");
        final Configuration configuration = createConfiguration(url);

        final StubMapping stubMapping = wireMock.register(get("/log4j-test2.xml")
                .willReturn(aResponse()
                        .withStatus(304)
                        .withHeader("Last-Modified", formatter.format(now) + " GMT")
                        .withHeader("Content-Type", XML)));
        Watcher watcher = new HttpWatcher(configuration, null, listeners, previous.toEpochMilli());
        watcher.watching(new Source(url));
        try {
            assertThat(watcher.isModified()).as("File was modified").isFalse();
            // If the file was not modified, neither should be the last modification time
            assertThat(watcher.getLastModified()).isEqualTo(previous.toEpochMilli());
            // Check that the last modified time of the ConfigurationSource was not modified either
            ConfigurationSource configurationSource = configuration.getConfigurationSource();
            assertThat(configurationSource.getLastModified())
                    .as("Last modification time of current ConfigurationSource")
                    .isEqualTo(0L);
            configurationSource = configurationSource.resetInputStream();
            assertThat(configurationSource.getLastModified())
                    .as("Last modification time of next ConfigurationSource")
                    .isEqualTo(0L);
        } finally {
            wireMock.removeStubMapping(stubMapping);
        }
    }

    // Creates a configuration with a predefined configuration source
    private static Configuration createConfiguration(URL url) {
        ConfigurationSource configurationSource = new ConfigurationSource(new Source(url), new byte[0], 0L);
        return new AbstractConfiguration(null, configurationSource) {};
    }

    private static class TestConfigurationListener implements ConfigurationListener {
        private final Queue<String> queue;
        private final String name;

        public TestConfigurationListener(final Queue<String> queue, final String name) {
            this.queue = queue;
            this.name = name;
        }

        @Override
        public void onChange(final Reconfigurable reconfigurable) {
            // System.out.println("Reconfiguration detected for " + name);
            queue.add(name);
        }
    }
}
