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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.core.util.WatchManager;
import org.apache.logging.log4j.core.util.Watcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {SpringConfiguration.class})
@TestPropertySource(properties = "spring.cloud.config.watch.enabled=true")
class Log4j2EventListenerTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Test
    void environmentChangeEventTriggersConfigWatch() {
        final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        final WatchManager watchManager = loggerContext.getConfiguration().getWatchManager();
        if (!watchManager.isStarted()) {
            watchManager.start();
        }
        final AtomicInteger count = new AtomicInteger(0);
        final Source source = new Source(new File("test.java"));
        watchManager.watch(source, new TestWatcher(count));
        publisher.publishEvent(new EnvironmentChangeEvent(new HashSet<>()));
        assertTrue(count.get() > 0);
    }

    private static class TestWatcher implements Watcher {

        private final AtomicInteger count;

        TestWatcher(final AtomicInteger count) {
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
