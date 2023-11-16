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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.test.BetterService;
import org.apache.logging.log4j.test.ListStatusListener;
import org.apache.logging.log4j.test.Service;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

public class ServiceLoaderUtilTest {

    @Test
    public void testServiceResolution() {
        final List<Object> services = new ArrayList<>();
        assertDoesNotThrow(() -> ServiceLoaderUtil.loadServices(BetterService.class, MethodHandles.lookup(), false)
                .forEach(services::add));
        assertThat(services).hasSize(1);
        services.clear();
        assertDoesNotThrow(() -> ServiceLoaderUtil.loadServices(PropertySource.class, MethodHandles.lookup(), false)
                .forEach(services::add));
        assertThat(services).hasSize(3);
    }

    @Test
    @UsingStatusListener
    public void testBrokenServiceFile(final ListStatusListener listener) {
        final List<Service> services = new ArrayList<>();
        assertDoesNotThrow(() -> ServiceLoaderUtil.loadServices(Service.class, MethodHandles.lookup(), false)
                .forEach(services::add));
        assertEquals(2, services.size());
        // A warning for each broken service
        final List<Throwable> errors = listener.findStatusData(Level.WARN)
                .map(StatusData::getThrowable)
                .collect(Collectors.toList());
        assertThat(errors.stream().map(Throwable::getMessage))
                .allMatch(message -> message.endsWith("invalid.Service not found")
                        || message.endsWith("org.apache.logging.log4j.Logger not a subtype")
                        || message.endsWith("Truncated class file"));
        assertThat(errors.stream().<Class<?>>map(Throwable::getClass))
                .containsExactlyInAnyOrder(
                        ServiceConfigurationError.class, ServiceConfigurationError.class, ClassFormatError.class);
    }

    @Test
    public void testOsgiUnavailable() {
        // OSGI classes are present...
        assertDoesNotThrow(() -> Class.forName("org.osgi.framework.FrameworkUtil"));
        // ...but we don't run in an OSGI container
        assertThat(OsgiServiceLocator.isAvailable())
                .as("Running in OSGI container")
                .isFalse();
    }
}
