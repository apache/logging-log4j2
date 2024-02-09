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

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.junit.jupiter.api.Test;

public class ConfigurationPropertiesOrderTest {

    @Test
    void propertiesCanComeLast() {
        final Configuration config = new AbstractConfiguration(null, ConfigurationSource.NULL_SOURCE) {
            @Override
            public void setup() {
                // Nodes
                final Node appenders = newNode(rootNode, "Appenders");
                rootNode.getChildren().add(appenders);

                final Node console = newNode(appenders, "Console");
                console.getAttributes().put("name", "${console.name}");
                appenders.getChildren().add(console);

                final Node loggers = newNode(rootNode, "Loggers");
                rootNode.getChildren().add(loggers);

                final Node rootLogger = newNode(loggers, "Root");
                rootLogger.getAttributes().put("level", "INFO");
                loggers.getChildren().add(rootLogger);

                final Node properties = newNode(rootNode, "Properties");
                rootNode.getChildren().add(properties);

                final Node property = newNode(properties, "Property");
                property.getAttributes().put("name", "console.name");
                property.getAttributes().put("value", "CONSOLE");
                properties.getChildren().add(property);
            }

            private Node newNode(final Node parent, final String name) {
                return new Node(rootNode, name, pluginManager.getPluginType(name));
            }
        };
        config.initialize();

        final Appender noInterpolation = config.getAppender("${console.name}");
        assertThat(noInterpolation).as("No interpolation for '${console.name}'").isNull();
        final Appender console = config.getAppender("CONSOLE");
        assertThat(console).isInstanceOf(ConsoleAppender.class);
    }
}
