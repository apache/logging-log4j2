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
package example;

import java.io.Serializable;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAliases;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginLoggerContext;
import org.apache.logging.log4j.core.config.plugins.PluginNode;
import org.apache.logging.log4j.core.config.plugins.PluginValue;

/**
 * Test plugin class for unit tests.
 */
@Plugin(name = "Fake", category = "Test")
@PluginAliases({"AnotherFake", "StillFake"})
public class FakePlugin {

    @Plugin(name = "Nested", category = "Test")
    public static class Nested {}

    @PluginFactory
    public static FakePlugin newPlugin(
            @PluginAttribute("attribute") int attribute,
            @PluginElement("layout") Layout<? extends Serializable> layout,
            @PluginConfiguration Configuration config,
            @PluginNode Node node,
            @PluginLoggerContext LoggerContext loggerContext,
            @PluginValue("value") String value) {
        return null;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<FakePlugin> {

        @PluginBuilderAttribute
        @SuppressWarnings("log4j.public.setter")
        private int attribute;

        @PluginBuilderAttribute
        @SuppressWarnings("log4j.public.setter")
        private int attributeWithoutPublicSetterButWithSuppressAnnotation;

        @PluginElement("layout")
        private Layout<? extends Serializable> layout;

        @PluginConfiguration
        private Configuration config;

        @PluginNode
        private Node node;

        @PluginLoggerContext
        private LoggerContext loggerContext;

        @PluginValue("value")
        private String value;

        @Override
        public FakePlugin build() {
            return null;
        }
    }
}
