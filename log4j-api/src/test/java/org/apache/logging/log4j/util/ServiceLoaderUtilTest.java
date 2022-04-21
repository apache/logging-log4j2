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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.status.StatusData;
import org.apache.logging.log4j.status.StatusListener;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.test.Service;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.SAME_THREAD)
public class ServiceLoaderUtilTest {

    private static final AtomicInteger counter = new AtomicInteger();
    private static final StatusListener listener = new StatusListener() {

        @Override
        public void close() throws IOException {
        }

        @Override
        public void log(StatusData data) {
            final StackTraceElement stackTraceElement = data.getStackTraceElement();
            if (stackTraceElement.getClassName().startsWith(ServiceLoaderUtil.class.getName())) {
                counter.incrementAndGet();
            }
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
            services = ServiceLoaderUtil.loadServices(Service.class, MethodHandles.lookup(), false)
                    .collect(Collectors.toList());
        } catch (ServiceConfigurationError e) {
            fail(e);
        }
        assertEquals(2, services.size());
        // A warning for each broken service
        assertEquals(warnings + 2, counter.get());
    }

}
