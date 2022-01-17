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

package org.apache.logging.log4j.plugin.processor;

import org.apache.logging.log4j.plugins.di.model.DisposesMethod;
import org.apache.logging.log4j.plugins.di.model.InjectionTarget;
import org.apache.logging.log4j.plugins.di.model.PluginModule;
import org.apache.logging.log4j.plugins.di.model.ProducerField;
import org.apache.logging.log4j.plugins.test.validation.ExampleBean;
import org.apache.logging.log4j.plugins.test.validation.FakePlugin;
import org.apache.logging.log4j.plugins.test.validation.ImplicitBean;
import org.apache.logging.log4j.plugins.test.validation.ImplicitMethodBean;
import org.apache.logging.log4j.plugins.test.validation.ProductionBean;
import org.apache.logging.log4j.plugins.test.validation.plugins.Log4jModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BeanProcessorTest {
    @Test
    void smokeTests() {
        final PluginModule module = new Log4jModule();
        final var plugins = module.getPluginSources();
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClass().equals(ExampleBean.class)));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClass().equals(ImplicitBean.class)));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClass().equals(ImplicitMethodBean.class)));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClass().equals(ProductionBean.Builder.class)));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof ProducerField &&
                plugin.getDeclaringClass().equals(ProductionBean.class)));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof DisposesMethod &&
                plugin.getDeclaringClass().equals(ProductionBean.class)));
        assertTrue(plugins.stream().anyMatch(plugin -> plugin instanceof InjectionTarget &&
                plugin.getDeclaringClass().equals(FakePlugin.class)));
    }
}
