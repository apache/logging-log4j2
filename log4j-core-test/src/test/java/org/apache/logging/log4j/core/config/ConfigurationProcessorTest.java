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
package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.di.ConfigurableInstanceFactory;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.plugins.test.validation.PluginWithGenericSubclassFoo1Builder;
import org.apache.logging.log4j.plugins.test.validation.ValidatingPlugin;
import org.apache.logging.log4j.plugins.test.validation.ValidatingPluginWithGenericBuilder;
import org.apache.logging.log4j.plugins.test.validation.ValidatingPluginWithTypedBuilder;
import org.apache.logging.log4j.plugins.test.validation.di.ConfigurablePlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

class ConfigurationProcessorTest {
    final ConfigurableInstanceFactory instanceFactory = DI.createInitializedFactory();
    final ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(instanceFactory);

    @Inject
    @Configurable
    PluginType<ConfigurablePlugin> configurablePluginType;
    @Inject
    @Configurable
    PluginType<ValidatingPlugin> validatingPluginType;
    @Inject
    @Configurable
    PluginType<ValidatingPluginWithGenericBuilder> validatingPluginWithGenericBuilderPluginType;
    @Inject
    @Configurable
    PluginType<ValidatingPluginWithTypedBuilder> validatingPluginWithTypedBuilderPluginType;
    @Inject
    @Configurable
    PluginType<PluginWithGenericSubclassFoo1Builder> pluginWithGenericSubclassFoo1BuilderPluginType;

    @BeforeEach
    void setUp() {
        instanceFactory.injectMembers(this);
        assertThat(configurablePluginType).isNotNull();
    }

    @Test
    void validatingPlugin() {
        final Node alpha = new Node(null, "alpha", validatingPluginType);
        alpha.setAttribute("name", "Alpha");
        final Object object = configurationProcessor.processNodeTree(alpha);
        assertThat(object)
                .asInstanceOf(type(ValidatingPlugin.class))
                .returns("Alpha", from(ValidatingPlugin::getName));
    }

    @Test
    void validatingPluginWithGenericBuilder() {
        final Node beta = new Node(null, "beta", validatingPluginWithGenericBuilderPluginType);
        beta.setAttribute("name", "Beta");
        final Object object = configurationProcessor.processNodeTree(beta);
        assertThat(object)
                .asInstanceOf(type(ValidatingPluginWithGenericBuilder.class))
                .returns("Beta", from(ValidatingPluginWithGenericBuilder::getName));
    }

    @Test
    void validatingPluginWithTypedBuilder() {
        final Node gamma = new Node(null, "gamma", validatingPluginWithTypedBuilderPluginType);
        gamma.setAttribute("name", "Gamma");
        final Object object = configurationProcessor.processNodeTree(gamma);
        assertThat(object)
                .asInstanceOf(type(ValidatingPluginWithTypedBuilder.class))
                .returns("Gamma", from(ValidatingPluginWithTypedBuilder::getName));
    }

    @Test
    void pluginWithGenericSubclassFoo1Builder() {
        final Node delta = new Node(null, "delta", pluginWithGenericSubclassFoo1BuilderPluginType);
        delta.setAttribute("thing", "thought");
        delta.setAttribute("foo1", "bar2");
        final Object object = configurationProcessor.processNodeTree(delta);
        assertThat(object)
                .asInstanceOf(type(PluginWithGenericSubclassFoo1Builder.class))
                .returns("bar2", from(PluginWithGenericSubclassFoo1Builder::getFoo1))
                .returns("thought", from(PluginWithGenericSubclassFoo1Builder::getThing));
    }

    @Test
    void configurablePlugin() {
        final Node root = new Node(null, "root", configurablePluginType);
        final Node alpha = new Node(root, "alpha", validatingPluginType);
        alpha.setAttribute("name", "Alpha");
        root.addChild(alpha);
        final Node beta = new Node(root, "beta", validatingPluginWithGenericBuilderPluginType);
        beta.setAttribute("name", "Beta");
        root.addChild(beta);
        final Node gamma = new Node(root, "gamma", validatingPluginWithTypedBuilderPluginType);
        gamma.setAttribute("name", "Gamma");
        root.addChild(gamma);
        final Node delta = new Node(root, "delta", pluginWithGenericSubclassFoo1BuilderPluginType);
        delta.setAttribute("thing", "thought");
        delta.setAttribute("foo1", "bar2");
        root.addChild(delta);
        final ConfigurablePlugin result = configurationProcessor.processNodeTree(root);
        assertThat(result)
                .hasNoNullFieldsOrProperties()
                .returns("Alpha", from(ConfigurablePlugin::getAlphaName))
                .returns("Beta", from(ConfigurablePlugin::getBetaName))
                .returns("Gamma", from(ConfigurablePlugin::getGammaName))
                .returns("thought", from(ConfigurablePlugin::getDeltaThing))
                .returns("bar2", from(ConfigurablePlugin::getDeltaName));
    }
}
