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
package org.apache.logging.log4j;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.apache.logging.log4j.test.TestLogger;
import org.apache.logging.log4j.test.TestLoggerContextFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Class Description goes here.
 */
public class ScopedResourceLoggerTest {
    @BeforeAll
    public static void beforeAll() {
        System.setProperty("log4j2.loggerContextFactory", TestLoggerContextFactory.class.getName());
    }

    @BeforeAll
    public static void afterAll() {
        System.clearProperty("log4j2.loggerContextFactory");
    }

    @Test
    public void testFactory() throws Exception {
        Connection connection = new Connection("Test", "dummy");
        connection.useConnection();
        MapSupplier mapSupplier = new MapSupplier(connection);
        Logger logger = ScopedResourceLogger.newBuilder()
                .withClass(this.getClass())
                .withSupplier(mapSupplier)
                .build();
        logger.debug("Hello, {}", "World");
        Logger log = LogManager.getLogger(this.getClass().getName());
        assertTrue(log instanceof TestLogger);
        TestLogger testLogger = (TestLogger) log;
        List<String> events = testLogger.getEntries();
        assertThat(events, hasSize(1));
        assertThat(events.get(0), containsString("Name=Test"));
        assertThat(events.get(0), containsString("Type=dummy"));
        assertThat(events.get(0), containsString("Count=1"));
        assertThat(events.get(0), containsString("Hello, World"));
        events.clear();
        connection.useConnection();
        logger.debug("Used the connection");
        assertThat(events.get(0), containsString("Count=2"));
        assertThat(events.get(0), containsString("Used the connection"));
        events.clear();
        connection = new Connection("NewConnection", "fiber");
        connection.useConnection();
        mapSupplier = new MapSupplier(connection);
        logger = ScopedResourceLogger.newBuilder().withSupplier(mapSupplier).build();
        logger.debug("Connection: {}", "NewConnection");
        assertThat(events, hasSize(1));
        assertThat(events.get(0), containsString("Name=NewConnection"));
        assertThat(events.get(0), containsString("Type=fiber"));
        assertThat(events.get(0), containsString("Count=1"));
        assertThat(events.get(0), containsString("Connection: NewConnection"));
        events.clear();
    }

    private static class MapSupplier implements Supplier<Map<String, ?>> {

        private final Connection connection;

        public MapSupplier(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public Map<String, ?> get() {
            Map<String, String> map = new HashMap<>();
            map.put("Name", connection.name);
            map.put("Type", connection.type);
            map.put("Count", Long.toString(connection.getCounter()));
            return map;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MapSupplier;
        }

        @Override
        public int hashCode() {
            return 77;
        }
    }

    private static class Connection {

        private final String name;
        private final String type;
        private final AtomicLong counter = new AtomicLong(0);

        public Connection(final String name, final String type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public long getCounter() {
            return counter.get();
        }

        public void useConnection() {
            counter.incrementAndGet();
        }
    }
}
