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

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.stream.Collectors;

import org.apache.logging.log4j.test.BetterService;
import org.apache.logging.log4j.test.Service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class ServiceLoaderUtilTest {

    @Test
    public void testServiceResolution() {
        // Run only if we are a module
        if (ServiceLoaderUtil.class.getModule().isNamed()) {
            List<Object> services = Collections.emptyList();
            // Service from test module
            try {
                services = ServiceLoaderUtil.loadServices(Service.class, MethodHandles.lookup())
                        .collect(Collectors.toList());
            } catch (ServiceConfigurationError e) {
                fail(e);
            }
            assertEquals(2, services.size(), "Service services");
            // BetterService from test module
            services.clear();
            try {
                services = ServiceLoaderUtil.loadServices(BetterService.class, MethodHandles.lookup())
                        .collect(Collectors.toList());
            } catch (ServiceConfigurationError e) {
                fail(e);
            }
            assertEquals(1, services.size(), "BetterService services");
            // PropertySource from org.apache.logging.log4j module from this module
            services.clear();
            try {
                services = ServiceLoaderUtil.loadServices(PropertySource.class, MethodHandles.lookup())
                        .collect(Collectors.toList());
            } catch (ServiceConfigurationError e) {
                fail(e);
            }
            assertEquals(0, services.size(), "PropertySource services");
        }
    }
}
