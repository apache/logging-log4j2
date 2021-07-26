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

import org.apache.logging.log4j.plugins.di.model.DisposesMethod;
import org.apache.logging.log4j.plugins.di.model.GenericPlugin;
import org.apache.logging.log4j.plugins.di.model.InjectionTarget;
import org.apache.logging.log4j.plugins.di.model.PluginModule;
import org.apache.logging.log4j.plugins.di.model.ProducerField;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanProcessorTest {
    @Test
    void smokeTests() {
        final PluginModule service = ServiceLoader.load(PluginModule.class, getClass().getClassLoader())
                .findFirst()
                .orElseThrow();
        final var plugins = service.getPluginSources();
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClassName().equals("org.apache.logging.log4j.plugins.test.validation.ExampleBean")));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClassName().equals("org.apache.logging.log4j.plugins.test.validation.ImplicitBean")));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClassName().equals("org.apache.logging.log4j.plugins.test.validation.ImplicitMethodBean")));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClassName().equals("org.apache.logging.log4j.plugins.test.validation.ProductionBean$Builder")));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof ProducerField &&
                plugin.getDeclaringClassName().equals("org.apache.logging.log4j.plugins.test.validation.ProductionBean")));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof DisposesMethod &&
                plugin.getDeclaringClassName().equals("org.apache.logging.log4j.plugins.test.validation.ProductionBean")));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof GenericPlugin &&
                plugin.getDeclaringClassName().equals(FakePlugin.class.getName())));
    }
}