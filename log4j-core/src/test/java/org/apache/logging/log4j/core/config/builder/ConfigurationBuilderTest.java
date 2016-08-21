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
package org.apache.logging.log4j.core.config.builder;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConfigurationBuilderTest {

    private void addTestFixtures(final String name, final ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setConfigurationName(name);
        builder.setStatusLevel(Level.ERROR);
        builder.add(builder.newScriptFile("target/test-classes/scripts/filter.groovy").addIsWatched(true));
        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                .addAttribute("level", Level.DEBUG));
        final AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout").
                addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY,
                Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
        builder.add(appenderBuilder);
        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG).
                    add(builder.newAppenderRef("Stdout")).
                    addAttribute("additivity", false).
                    addAttribute("includeLocation", true));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));
        builder.addProperty("MyKey", "MyValue");
        builder.add(builder.newCustomLevel("Panic", 17));
        builder.setPackages("foo,bar");
    }

    private final static String expectedXml1 =
            "<?xml version='1.0' encoding='UTF-8'?>" + System.lineSeparator() +
            "<Configuration name=\"config name\" status=\"ERROR\" packages=\"foo,bar\">" + System.lineSeparator() +
            "\t<Properties>" + System.lineSeparator() +
            "\t\t<Property name=\"MyKey\">MyValue</Property>" + System.lineSeparator() +
            "\t</Properties>" + System.lineSeparator() +
            "\t<Scripts>" + System.lineSeparator() +
            "\t\t<ScriptFile path=\"target/test-classes/scripts/filter.groovy\" name=\"target/test-classes/scripts/filter.groovy\" isWatched=\"true\"/>" + System.lineSeparator() +
            "\t</Scripts>" + System.lineSeparator() +
            "\t<CustomLevels>" + System.lineSeparator() +
            "\t\t<CustomLevel intLevel=\"17\" name=\"Panic\"/>" + System.lineSeparator() +
            "\t</CustomLevels>" + System.lineSeparator() +
            "\t<ThresholdFilter onMatch=\"ACCEPT\" level=\"DEBUG\" onMisMatch=\"NEUTRAL\"/>" + System.lineSeparator() +
            "\t<Appenders>" + System.lineSeparator() +
            "\t\t<CONSOLE name=\"Stdout\" target=\"SYSTEM_OUT\">" + System.lineSeparator() +
            "\t\t\t<PatternLayout pattern=\"%d [%t] %-5level: %msg%n%throwable\"/>" + System.lineSeparator() +
            "\t\t\t<MarkerFilter onMatch=\"DENY\" onMisMatch=\"NEUTRAL\" marker=\"FLOW\"/>" + System.lineSeparator() +
            "\t\t</CONSOLE>" + System.lineSeparator() +
            "\t</Appenders>" + System.lineSeparator() +
            "\t<Loggers>" + System.lineSeparator() +
            "\t\t<Logger additivity=\"false\" level=\"DEBUG\" includeLocation=\"true\" name=\"org.apache.logging.log4j\">" + System.lineSeparator() +
            "\t\t\t<AppenderRef ref=\"Stdout\"/>" + System.lineSeparator() +
            "\t\t</Logger>" + System.lineSeparator() +
            "\t\t<Root level=\"ERROR\">" + System.lineSeparator() +
            "\t\t\t<AppenderRef ref=\"Stdout\"/>" + System.lineSeparator() +
            "\t\t</Root>" + System.lineSeparator() +
            "\t</Loggers>" + System.lineSeparator() +
            "</Configuration>" + System.lineSeparator();

    @Test
    public void testXmlConstructing() throws Exception {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        addTestFixtures("config name", builder);
        final String xmlConfiguration = builder.toXmlConfiguration();
        assertEquals(expectedXml1, xmlConfiguration);
    }

    private final static String expectedXml2 =
            "<?xml version='1.0' encoding='UTF-8'?>" + System.lineSeparator() +
            "<Configuration name=\"config name\" status=\"ERROR\" packages=\"foo,bar\">" + System.lineSeparator() +
            "\t<Properties>" + System.lineSeparator() +
            "\t\t<Property name=\"MyKey\">MyValue</Property>" + System.lineSeparator() +
            "\t</Properties>" + System.lineSeparator() +
            "\t<Scripts>" + System.lineSeparator() +
            "\t\t<ScriptFile name=\"target/test-classes/scripts/filter.groovy\" path=\"target/test-classes/scripts/filter.groovy\" isWatched=\"true\"/>" + System.lineSeparator() +
            "\t</Scripts>" + System.lineSeparator() +
            "\t<CustomLevels>" + System.lineSeparator() +
            "\t\t<CustomLevel name=\"Panic\" intLevel=\"17\"/>" + System.lineSeparator() +
            "\t</CustomLevels>" + System.lineSeparator() +
            "\t<ThresholdFilter onMatch=\"ACCEPT\" level=\"DEBUG\" onMisMatch=\"NEUTRAL\"/>" + System.lineSeparator() +
            "\t<Appenders>" + System.lineSeparator() +
            "\t\t<CONSOLE name=\"Stdout\" target=\"SYSTEM_OUT\">" + System.lineSeparator() +
            "\t\t\t<PatternLayout pattern=\"%d [%t] %-5level: %msg%n%throwable\"/>" + System.lineSeparator() +
            "\t\t\t<MarkerFilter onMatch=\"DENY\" onMisMatch=\"NEUTRAL\" marker=\"FLOW\"/>" + System.lineSeparator() +
            "\t\t</CONSOLE>" + System.lineSeparator() +
            "\t</Appenders>" + System.lineSeparator() +
            "\t<Loggers>" + System.lineSeparator() +
            "\t\t<Logger name=\"org.apache.logging.log4j\" additivity=\"false\" level=\"DEBUG\" includeLocation=\"true\">" + System.lineSeparator() +
            "\t\t\t<AppenderRef ref=\"Stdout\"/>" + System.lineSeparator() +
            "\t\t</Logger>" + System.lineSeparator() +
            "\t\t<Root level=\"ERROR\">" + System.lineSeparator() +
            "\t\t\t<AppenderRef ref=\"Stdout\"/>" + System.lineSeparator() +
            "\t\t</Root>" + System.lineSeparator() +
            "\t</Loggers>" + System.lineSeparator() +
            "</Configuration>" + System.lineSeparator();

    @Test
    public void testXmlConstructingWithAbstractConfiguration() throws Exception {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        addTestFixtures("config name", builder);
        AbstractConfiguration configuration = builder.build(false);
        configuration.setup();
        final String xmlConfiguration = configuration.toXmlConfiguration();
        assertEquals(expectedXml2, xmlConfiguration);
    }
}
