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
package org.apache.logging.log4j.spring.cloud.config.client;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.Watcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Class Description goes here.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SpringConfiguration.class})
public class Log4j2EventListenerTest {

    private static final String CONFIG = "log4j-console.xml";
    private static final String DIR = "target/logs";

    public static LoggerContextRule loggerContextRule =
            LoggerContextRule.createShutdownTimeoutLoggerContextRule(CONFIG);

    @Rule
    public RuleChain chain = loggerContextRule.withCleanFilesRule(DIR);

    @Autowired
    private ApplicationEventPublisher publisher;

    @Test
    public void test() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final Source source = new Source(new File("test.java"));
        loggerContextRule
                .getLoggerContext()
                .getConfiguration()
                .getWatchManager()
                .watch(source, new TestWatcher(count));
        publisher.publishEvent(new EnvironmentChangeEvent(new HashSet<>()));
        assertTrue(count.get() > 0);
    }

    private static class TestWatcher implements Watcher {

        private final AtomicInteger count;

        public TestWatcher(final AtomicInteger count) {
            this.count = count;
        }

        @Override
        public List<ConfigurationListener> getListeners() {
            return null;
        }

        @Override
        public void modified() {}

        @Override
        public boolean isModified() {
            count.incrementAndGet();
            return false;
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        @Override
        public void watching(final Source source) {}

        @Override
        public Source getSource() {
            return null;
        }

        @Override
        public Watcher newWatcher(
                final Reconfigurable reconfigurable,
                final List<ConfigurationListener> listeners,
                final long lastModifiedMillis) {
            return this;
        }
    }
}
