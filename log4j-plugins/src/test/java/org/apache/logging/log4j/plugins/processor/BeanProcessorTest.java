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

import org.apache.logging.log4j.plugins.di.spi.PluginModule;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanProcessorTest {
    @Test
    void smokeTests() {
        final PluginModule service = ServiceLoader.load(PluginModule.class, getClass().getClassLoader())
                .findFirst()
                .orElseThrow();
        assertTrue(service.getInjectionBeans().containsKey("org.apache.logging.log4j.plugins.test.validation.ExampleBean"));
        assertTrue(service.getInjectionBeans().containsKey("org.apache.logging.log4j.plugins.test.validation.ImplicitBean"));
        assertTrue(service.getInjectionBeans().containsKey("org.apache.logging.log4j.plugins.test.validation.ImplicitMethodBean"));
        assertTrue(service.getInjectionBeans().containsKey("org.apache.logging.log4j.plugins.test.validation.ProductionBean$Builder"));
        assertTrue(service.getProducerBeans().containsKey("org.apache.logging.log4j.plugins.test.validation.ProductionBean"));
        assertTrue(service.getDestructorBeans().contains("org.apache.logging.log4j.plugins.test.validation.ProductionBean"));
        assertTrue(service.getPluginBeans().containsKey(FakePlugin.class.getName()));
    }
}