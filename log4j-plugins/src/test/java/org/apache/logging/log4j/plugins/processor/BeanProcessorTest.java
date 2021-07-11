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

package org.apache.logging.log4j.plugins.processor;

import org.apache.logging.log4j.plugins.di.spi.BeanInfoService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanProcessorTest {
    @Test
    void canLoadTestCategory() {
        final BeanInfoService service = ServiceLoader.load(BeanInfoService.class, getClass().getClassLoader())
                .findFirst()
                .orElseThrow();
        final List<String> testPlugins = service.getPluginCategories().get("Test");
        assertNotNull(testPlugins);
        assertNotEquals(0, testPlugins.size());
        assertTrue(testPlugins.stream().anyMatch(name -> name.equals(FakePlugin.class.getName())));
    }

    @Test
    void smokeTests() {
        final BeanInfoService service = ServiceLoader.load(BeanInfoService.class, getClass().getClassLoader())
                .findFirst()
                .orElseThrow();
        assertTrue(service.getInjectableClassNames().stream().anyMatch(name -> name.equals("org.apache.logging.log4j.plugins.test.validation.ExampleBean")));
        assertTrue(service.getInjectableClassNames().stream().anyMatch(name -> name.equals("org.apache.logging.log4j.plugins.test.validation.ImplicitBean")));
        assertTrue(service.getInjectableClassNames().stream().anyMatch(name -> name.equals("org.apache.logging.log4j.plugins.test.validation.ImplicitMethodBean")));
        assertTrue(service.getInjectableClassNames().stream().anyMatch(name -> name.equals("org.apache.logging.log4j.plugins.test.validation.ProductionBean$Builder")));
        assertTrue(service.getProducibleClassNames().stream().anyMatch(name -> name.equals("org.apache.logging.log4j.plugins.test.validation.ProductionBean")));
        assertTrue(service.getDestructibleClassNames().stream().anyMatch(name -> name.equals("org.apache.logging.log4j.plugins.test.validation.ProductionBean")));
    }
}