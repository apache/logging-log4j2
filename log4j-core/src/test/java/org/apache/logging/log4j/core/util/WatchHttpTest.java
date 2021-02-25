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
package org.apache.logging.log4j.core.util;

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
import org.apache.logging.log4j.core.net.ssl.TestConstants;
import org.apache.logging.log4j.core.util.datetime.FastDateFormat;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.removeStub;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test the WatchManager
 */
public class WatchHttpTest {

    private static final String FORCE_RUN_KEY = WatchHttpTest.class.getSimpleName() + ".forceRun";
    private final String file = "log4j-test1.xml";
    private static FastDateFormat formatter;
    private static final String XML = "application/xml";

    private static final boolean IS_WINDOWS = PropertiesUtil.getProperties().isOsWindows();

    @BeforeClass
    public static void beforeClass() {
        try {
            formatter = FastDateFormat.getInstance("EEE, dd MMM yyyy HH:mm:ss", TimeZone.getTimeZone("UTC"));
        } catch (Exception ex) {
            System.err.println("Unable to create date format.");
            ex.printStackTrace();
            throw ex;
        }
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort()
        .keystorePath(TestConstants.KEYSTORE_FILE)
        .keystorePassword(String.valueOf(TestConstants.KEYSTORE_PWD()))
        .keystoreType(TestConstants.KEYSTORE_TYPE));

    @Test
    public void testWatchManager() throws Exception {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        List<ConfigurationListener> listeners = new ArrayList<>();
        listeners.add(new TestConfigurationListener(queue, "log4j-test1.xml"));
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar now = Calendar.getInstance(timeZone);
        Calendar previous = now;
        previous.add(Calendar.MINUTE, -5);
        Configuration configuration = new DefaultConfiguration();
        Assume.assumeTrue(!IS_WINDOWS || Boolean.getBoolean(FORCE_RUN_KEY));
        URL url = new URL("http://localhost:" + wireMockRule.port() + "/log4j-test1.xml");
        StubMapping stubMapping = stubFor(get(urlPathEqualTo("/log4j-test1.xml"))
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
            watchManager.watch(new Source(url.toURI(), previous.getTimeInMillis()), new HttpWatcher(configuration, null,
                listeners, previous.getTimeInMillis()));
            final String str = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull("File change not detected", str);
        } finally {
            removeStub(stubMapping);
            watchManager.stop();
            scheduler.stop();
        }
    }

    @Test
    public void testNotModified() throws Exception {
        BlockingQueue<String> queue = new LinkedBlockingQueue<>();
        List<ConfigurationListener> listeners = new ArrayList<>();
        listeners.add(new TestConfigurationListener(queue, "log4j-test2.xml"));
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar now = Calendar.getInstance(timeZone);
        Calendar previous = now;
        previous.add(Calendar.MINUTE, -5);
        Configuration configuration = new DefaultConfiguration();
        Assume.assumeTrue(!IS_WINDOWS || Boolean.getBoolean(FORCE_RUN_KEY));
        URL url = new URL("http://localhost:" + wireMockRule.port() + "/log4j-test2.xml");
        StubMapping stubMapping = stubFor(get(urlPathEqualTo("/log4j-test2.xml"))
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
            watchManager.watch(new Source(url.toURI(), previous.getTimeInMillis()), new HttpWatcher(configuration, null,
                listeners, previous.getTimeInMillis()));
            final String str = queue.poll(2, TimeUnit.SECONDS);
            assertNull("File changed.", str);
        } finally {
            removeStub(stubMapping);
            watchManager.stop();
            scheduler.stop();
        }
    }

    private static class TestConfigurationListener implements ConfigurationListener {
        private final Queue<String> queue;
        private final String name;

        public TestConfigurationListener(final Queue<String> queue, String name) {
            this.queue = queue;
            this.name = name;
        }

        @Override
        public void onChange(Reconfigurable reconfigurable) {
            //System.out.println("Reconfiguration detected for " + name);
            queue.add(name);
        }
    }
}
