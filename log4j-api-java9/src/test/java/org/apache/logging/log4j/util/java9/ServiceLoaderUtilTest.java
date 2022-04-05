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
package org.apache.logging.log4j.util.java9;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.PropertySource;
import org.apache.logging.log4j.util.ServiceLoaderUtil;
import org.apache.logging.log4j.util.java9.test.BetterService;
import org.apache.logging.log4j.util.java9.test.Service;
import org.junit.jupiter.api.Test;

public class ServiceLoaderUtilTest {

    @Test
    public void testServiceResolution() {
        List<Object> services;
        // Service from test module
        services = assertDoesNotThrow(() -> ServiceLoaderUtil.loadServices(Service.class, MethodHandles.lookup())
                .collect(Collectors.toList()));
        assertThat(services).hasSize(2);
        // BetterService from test module
        services = assertDoesNotThrow(() -> ServiceLoaderUtil.loadServices(BetterService.class, MethodHandles.lookup())
                .collect(Collectors.toList()));
        assertThat(services).hasSize(1);
        // PropertySource from org.apache.logging.log4j module from this module
        services = assertDoesNotThrow(() -> ServiceLoaderUtil.loadServices(PropertySource.class, MethodHandles.lookup())
                .collect(Collectors.toList()));
        assertThat(services).hasSize(0);
        // PropertySource from within org.apache.logging.log4j module
        services = assertDoesNotThrow(() -> PropertySource.loadPropertySources().collect(Collectors.toList()));
        assertThat(services).hasSize(2);
    }
}
