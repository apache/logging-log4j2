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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.from;

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
import org.apache.logging.log4j.plugins.test.validation.di.ConfigurableRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Inject
    @Configurable
    PluginType<ConfigurableRecord> configurableRecordPluginType;

    @BeforeEach
    void setUp() {
        instanceFactory.injectMembers(this);
        assertThat(this).hasNoNullFieldsOrProperties();
        assertThat(configurablePluginType).isNotNull();
    }

    @Test
    void validatingPlugin() {
        final Node alpha = Node.newBuilder()
                .setName("alpha")
                .setPluginType(validatingPluginType)
                .setAttribute("name", "Alpha")
                .get();
        final ValidatingPlugin object = configurationProcessor.processNodeTree(alpha);
        assertThat(object).returns("Alpha", from(ValidatingPlugin::getName));
    }

    @Test
    void validatingPluginWithGenericBuilder() {
        final Node beta = Node.newBuilder()
                .setName("beta")
                .setPluginType(validatingPluginWithGenericBuilderPluginType)
                .setAttribute("name", "Beta")
                .get();
        final ValidatingPluginWithGenericBuilder object = configurationProcessor.processNodeTree(beta);
        assertThat(object).returns("Beta", from(ValidatingPluginWithGenericBuilder::getName));
    }

    @Test
    void validatingPluginWithTypedBuilder() {
        final Node gamma = Node.newBuilder()
                .setName("gamma")
                .setPluginType(validatingPluginWithTypedBuilderPluginType)
                .setAttribute("name", "Gamma")
                .get();
        final ValidatingPluginWithTypedBuilder object = configurationProcessor.processNodeTree(gamma);
        assertThat(object).returns("Gamma", from(ValidatingPluginWithTypedBuilder::getName));
    }

    @Test
    void pluginWithGenericSubclassFoo1Builder() {
        final Node delta = Node.newBuilder()
                .setName("delta")
                .setPluginType(pluginWithGenericSubclassFoo1BuilderPluginType)
                .setAttribute("thing", "thought")
                .setAttribute("foo1", "bar2")
                .get();
        final PluginWithGenericSubclassFoo1Builder object = configurationProcessor.processNodeTree(delta);
        assertThat(object)
                .returns("bar2", from(PluginWithGenericSubclassFoo1Builder::getFoo1))
                .returns("thought", from(PluginWithGenericSubclassFoo1Builder::getThing));
    }

    @Test
    void configurablePlugin() {
        final Node root = Node.newBuilder()
                .setName("root")
                .setPluginType(configurablePluginType)
                .addChild(builder -> builder.setName("alpha")
                        .setPluginType(validatingPluginType)
                        .setAttribute("name", "Alpha"))
                .addChild(builder -> builder.setName("beta")
                        .setPluginType(validatingPluginWithGenericBuilderPluginType)
                        .setAttribute("name", "Beta"))
                .addChild(builder -> builder.setName("gamma")
                        .setPluginType(validatingPluginWithTypedBuilderPluginType)
                        .setAttribute("name", "Gamma"))
                .addChild(builder -> builder.setName("delta")
                        .setPluginType(pluginWithGenericSubclassFoo1BuilderPluginType)
                        .setAttribute("thing", "thought")
                        .setAttribute("foo1", "bar2"))
                .get();
        final ConfigurablePlugin result = configurationProcessor.processNodeTree(root);
        assertThat(result)
                .hasNoNullFieldsOrProperties()
                .returns("Alpha", from(ConfigurablePlugin::getAlphaName))
                .returns("Beta", from(ConfigurablePlugin::getBetaName))
                .returns("Gamma", from(ConfigurablePlugin::getGammaName))
                .returns("thought", from(ConfigurablePlugin::getDeltaThing))
                .returns("bar2", from(ConfigurablePlugin::getDeltaName));
    }

    @Test
    void configurableRecord() {
        final Node root = Node.newBuilder()
                .setName("root")
                .setPluginType(configurableRecordPluginType)
                .addChild(builder -> builder.setName("alpha")
                        .setPluginType(validatingPluginType)
                        .setAttribute("name", "Alpha"))
                .addChild(builder -> builder.setName("beta")
                        .setPluginType(validatingPluginWithGenericBuilderPluginType)
                        .setAttribute("name", "Beta"))
                .addChild(builder -> builder.setName("gamma")
                        .setPluginType(validatingPluginWithTypedBuilderPluginType)
                        .setAttribute("name", "Gamma"))
                .addChild(builder -> builder.setName("delta")
                        .setPluginType(pluginWithGenericSubclassFoo1BuilderPluginType)
                        .setAttribute("thing", "thought")
                        .setAttribute("foo1", "bar2"))
                .get();
        final ConfigurableRecord result = configurationProcessor.processNodeTree(root);
        assertThat(result)
                .hasNoNullFieldsOrProperties()
                .returns("Alpha", from(ConfigurableRecord::alpha).andThen(ValidatingPlugin::getName))
                .returns("Beta", from(ConfigurableRecord::beta).andThen(ValidatingPluginWithGenericBuilder::getName))
                .returns("Gamma", from(ConfigurableRecord::gamma).andThen(ValidatingPluginWithTypedBuilder::getName))
                .returns(
                        "thought",
                        from(ConfigurableRecord::delta).andThen(PluginWithGenericSubclassFoo1Builder::getThing))
                .returns(
                        "bar2", from(ConfigurableRecord::delta).andThen(PluginWithGenericSubclassFoo1Builder::getFoo1));
    }
}
