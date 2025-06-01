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
package org.apache.logging.log4j.core.config.builder;

import java.net.URI;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;

/**
 * Normally this would be a plugin. However, we don't want it used for everything so it will be defined
 * via a system property.
 */
// @Plugin(name = "CustomConfigurationFactory", category = ConfigurationFactory.CATEGORY)
// @Order(50)
public class CustomConfigurationFactory extends ConfigurationFactory {

    static Configuration addTestFixtures(final String name, final ConfigurationBuilder<?> builder) {
        builder.setConfigurationName(name);
        builder.setStatusLevel(Level.ERROR);
        builder.add(builder.newScriptFile("target/test-classes/scripts/filter.groovy")
                .setIsWatchedAttribute(true));
        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                .setAttribute("level", Level.DEBUG));

        final AppenderComponentBuilder appenderBuilder =
                builder.newAppender("Stdout", "CONSOLE").setAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(
                builder.newLayout("PatternLayout").setAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .setAttribute("marker", "FLOW"));
        builder.add(appenderBuilder);

        final AppenderComponentBuilder appenderBuilder2 =
                builder.newAppender("Kafka", "Kafka").setAttribute("topic", "my-topic");
        appenderBuilder2.addComponent(builder.newProperty("bootstrap.servers", "localhost:9092"));
        appenderBuilder2.add(builder.newLayout("GelfLayout")
                .setAttribute("host", "my-host")
                .addComponent(builder.newKeyValuePair("extraField", "extraValue")));
        builder.add(appenderBuilder2);

        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG, true)
                .add(builder.newAppenderRef("Stdout"))
                .setAdditivityAttribute(false));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));

        builder.add(builder.newCustomLevel("Panic", 17));

        return builder.build();
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(
            final LoggerContext loggerContext, final String name, final URI configLocation) {
        final ConfigurationBuilder<?> builder = newConfigurationBuilder();
        return addTestFixtures(name, builder);
    }

    @Override
    protected String[] getSupportedTypes() {
        return new String[] {"*"};
    }
}
