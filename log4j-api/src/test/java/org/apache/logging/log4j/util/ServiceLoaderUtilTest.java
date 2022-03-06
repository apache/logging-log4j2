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

package org.apache.logging.log4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.test.BetterService;
import org.apache.logging.log4j.util.test.Service;
import org.apache.logging.log4j.util.test.Service1;
import org.apache.logging.log4j.util.test.Service2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.SetSystemProperty.SetSystemProperties;

public class ServiceLoaderUtilTest {

    private static final AtomicInteger counter = new AtomicInteger();
    private static final StatusListener listener = new StatusListener() {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void log(StatusData data) {
            counter.incrementAndGet();
        }

        @Override
        public Level getStatusLevel() {
            return Level.WARN;
        }
    };

    @BeforeAll
    public static void installStatusListener() {
        StatusLogger.getLogger().registerListener(listener);
    }

    @AfterAll
    public static void removeStatusListener() {
        StatusLogger.getLogger().removeListener(listener);
    }

    @Test
    public void testBrokenServiceFile() {
        List<Service> services = Collections.emptyList();
        final int warnings = counter.get();
        try {
            services = ServiceLoaderUtil.loadServices(Service.class, StatusLogger.getLogger()::warn);
        } catch (ServiceConfigurationError e) {
            fail(e);
        }
        assertEquals(2, services.size());
        // A warning for each broken service
        assertEquals(warnings + 2, counter.get());
    }

    @Test
    public void testMultipleServicesPresentWarning() {
        final int warnings = counter.get();
        final Service service = ServiceLoaderUtil.getService(Service.class);
        assertTrue(service instanceof Service1);
        assertEquals(warnings + 1, counter.get());
        // No warning
        final BetterService betterService = ServiceLoaderUtil.getService(BetterService.class);
        assertTrue(betterService instanceof Service2);
        assertEquals(warnings + 1, counter.get());
    }

    @Test
    @SetSystemProperties({
            @SetSystemProperty(key = "org.apache.logging.log4j.util.test.Service", value = "org.apache.logging.log4j.util.test.Service2"),
            @SetSystemProperty(key = "org.apache.logging.log4j.util.test.BetterService", value = "java.lang.String") })
    public void testOverrideService() {
        final int warnings = counter.get();
        // Valid override
        final Service service = ServiceLoaderUtil.getService(Service.class);
        assertTrue(service instanceof Service2);
        assertEquals(warnings, counter.get());
        // Invalid override
        final BetterService betterService = ServiceLoaderUtil.getService(BetterService.class);
        assertEquals(warnings + 1, counter.get());
        assertTrue(betterService instanceof Service2);
    }

    @Test
    @SetSystemProperty(key = "java.lang.String", value = "invalid.String")
    public void testNonExistingService() {
        final int warnings = counter.get();
        assertNull(ServiceLoaderUtil.getService(Double.class));
        assertEquals(warnings, counter.get());
        // With manual override
        assertNull(ServiceLoaderUtil.getService(String.class));
        assertEquals(warnings + 1, counter.get());
    }
}
