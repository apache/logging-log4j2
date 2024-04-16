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
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.junit.jupiter.api.Test;

/**
 * Tests the ParameterizedMapMessageFactory class.
 */
@LoggerContextSource("log4j-map.xml")
public class ScopedResourceLoggerTest {

    private final ListAppender app;

    public ScopedResourceLoggerTest(@Named("List") final ListAppender list) {
        app = list.clear();
    }

    @Test
    public void testFactory(final LoggerContext context) throws Exception {
        Connection connection = new Connection("Test", "dummy");
        connection.useConnection();
        MapSupplier mapSupplier = new MapSupplier(connection);
        Logger logger = ScopedResourceLogger.newBuilder()
                .withClass(this.getClass())
                .withSupplier(mapSupplier)
                .build();
        logger.debug("Hello, {}", "World");
        List<LogEvent> events = app.getEvents();
        assertThat(events, hasSize(1));
        ReadOnlyStringMap map = events.get(0).getContextData();
        assertNotNull(map);
        Map<String, String> data = map.toMap();
        assertThat(data.size(), equalTo(3));
        assertEquals("Test", data.get("Name"));
        assertEquals("dummy", data.get("Type"));
        assertEquals("1", data.get("Count"));
        assertEquals("Hello, World", events.get(0).getMessage().getFormattedMessage());
        assertEquals(this.getClass().getName(), events.get(0).getLoggerName());
        assertEquals(this.getClass().getName(), events.get(0).getSource().getClassName());
        app.clear();
        connection.useConnection();
        logger.debug("Used the connection");
        events = app.getEvents();
        assertThat(events, hasSize(1));
        map = events.get(0).getContextData();
        assertNotNull(map);
        data = map.toMap();
        assertThat(data, aMapWithSize(3));
        assertEquals("2", data.get("Count"));
        app.clear();
        connection = new Connection("NewConnection", "fiber");
        connection.useConnection();
        mapSupplier = new MapSupplier(connection);
        logger = ScopedResourceLogger.newBuilder().withSupplier(mapSupplier).build();
        logger.debug("Connection: {}", "NewConnection");
        events = app.getEvents();
        assertThat(events, hasSize(1));
        map = events.get(0).getContextData();
        assertNotNull(map);
        data = map.toMap();
        assertThat(data, aMapWithSize(3));
        assertEquals("NewConnection", data.get("Name"));
        assertEquals("fiber", data.get("Type"));
        assertEquals("1", data.get("Count"));
        assertEquals("Connection: NewConnection", events.get(0).getMessage().getFormattedMessage());
        assertEquals(this.getClass().getName(), events.get(0).getLoggerName());
        assertEquals(this.getClass().getName(), events.get(0).getSource().getClassName());
        app.clear();
    }

    private static class MapSupplier implements Supplier<Map<String, ?>> {

        private final Connection connection;

        public MapSupplier(final Connection connection) {
            this.connection = connection;
        }

        @Override
        public Map<String, String> get() {
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
