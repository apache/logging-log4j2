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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.ConfigurationScheduler;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.HttpWatcher;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.jupiter.api.Test;

/**
 * Test the WatchManager
 */
@SetTestProperty(key = UrlConnectionFactory.ALLOWED_PROTOCOLS, value = "http,https")
@WireMockTest
public class WatchHttpTest {

    private static final String FORCE_RUN_KEY = WatchHttpTest.class.getSimpleName() + ".forceRun";
    private final String file = "log4j-test1.xml";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static FastDateFormat formatter = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss", UTC);
    private static final String XML = "application/xml";

    private static final boolean IS_WINDOWS = PropertiesUtil.getProperties().isOsWindows();

    @Test
    public void testWatchManager(final WireMockRuntimeInfo info) throws Exception {
        assumeTrue(!IS_WINDOWS || Boolean.getBoolean(FORCE_RUN_KEY));
        final WireMock wireMock = info.getWireMock();

        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        final List<ConfigurationListener> listeners = new ArrayList<>();
        listeners.add(new TestConfigurationListener(queue, "log4j-test1.xml"));
        final Calendar now = Calendar.getInstance(UTC);
        final Calendar previous = now;
        previous.add(Calendar.MINUTE, -5);
        final Configuration configuration = new DefaultConfiguration();
        final URL url = new URL(info.getHttpBaseUrl() + "/log4j-test1.xml");
        final StubMapping stubMapping = wireMock.register(get(urlPathEqualTo("/log4j-test1.xml"))
                .willReturn(aResponse()
                        .withBodyFile(file)
                        .withStatus(200)
                        .withHeader("Last-Modified", formatter.format(previous) + " GMT")
                        .withHeader("Content-Type", XML)));
        final ConfigurationScheduler scheduler = new ConfigurationScheduler();
        scheduler.incrementScheduledItems();
        final WatchManager watchManager = new WatchManager(scheduler);
        watchManager.setIntervalSeconds(1);
        scheduler.start();
        watchManager.start();
        try {
            watchManager.watch(
                    new Source(url), new HttpWatcher(configuration, null, listeners, previous.getTimeInMillis()));
            final String str = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull("File change not detected", str);
        } finally {
            watchManager.stop();
            scheduler.stop();
            wireMock.removeStubMapping(stubMapping);
        }
    }

    @Test
    public void testNotModified(final WireMockRuntimeInfo info) throws Exception {
        assumeTrue(!IS_WINDOWS || Boolean.getBoolean(FORCE_RUN_KEY));
        final WireMock wireMock = info.getWireMock();

        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        final List<ConfigurationListener> listeners = new ArrayList<>();
        listeners.add(new TestConfigurationListener(queue, "log4j-test2.xml"));
        final TimeZone timeZone = TimeZone.getTimeZone("UTC");
        final Calendar now = Calendar.getInstance(timeZone);
        final Calendar previous = now;
        previous.add(Calendar.MINUTE, -5);
        final Configuration configuration = new DefaultConfiguration();
        final URL url = new URL(info.getHttpBaseUrl() + "/log4j-test2.xml");
        final StubMapping stubMapping = wireMock.register(get(urlPathEqualTo("/log4j-test2.xml"))
                .willReturn(aResponse()
                        .withBodyFile(file)
                        .withStatus(304)
                        .withHeader("Last-Modified", formatter.format(now) + " GMT")
                        .withHeader("Content-Type", XML)));
        final ConfigurationScheduler scheduler = new ConfigurationScheduler();
        scheduler.incrementScheduledItems();
        final WatchManager watchManager = new WatchManager(scheduler);
        watchManager.setIntervalSeconds(1);
        scheduler.start();
        watchManager.start();
        try {
            watchManager.watch(
                    new Source(url), new HttpWatcher(configuration, null, listeners, previous.getTimeInMillis()));
            final String str = queue.poll(2, TimeUnit.SECONDS);
            assertNull("File changed.", str);
        } finally {
            watchManager.stop();
            scheduler.stop();
            wireMock.removeStubMapping(stubMapping);
        }
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
