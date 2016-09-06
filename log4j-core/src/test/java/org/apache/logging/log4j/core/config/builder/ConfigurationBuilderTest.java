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
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigurationBuilderTest {

    private static final String EOL = System.lineSeparator();

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
        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG, true).
                    add(builder.newAppenderRef("Stdout")).
                    addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));
        builder.addProperty("MyKey", "MyValue");
        builder.add(builder.newCustomLevel("Panic", 17));
        builder.setPackages("foo,bar");
    }

    private final static String expectedXml =
            "<?xml version='1.0' encoding='UTF-8'?>" + EOL +
            "<Configuration name=\"config name\" status=\"ERROR\" packages=\"foo,bar\">" + EOL +
            "\t<Properties>" + EOL +
            "\t\t<Property name=\"MyKey\">MyValue</Property>" + EOL +
            "\t</Properties>" + EOL +
            "\t<Scripts>" + EOL +
            "\t\t<ScriptFile name=\"target/test-classes/scripts/filter.groovy\" path=\"target/test-classes/scripts/filter.groovy\" isWatched=\"true\"/>" + EOL +
            "\t</Scripts>" + EOL +
            "\t<CustomLevels>" + EOL +
            "\t\t<CustomLevel name=\"Panic\" intLevel=\"17\"/>" + EOL +
            "\t</CustomLevels>" + EOL +
            "\t<ThresholdFilter onMatch=\"ACCEPT\" onMisMatch=\"NEUTRAL\" level=\"DEBUG\"/>" + EOL +
            "\t<Appenders>" + EOL +
            "\t\t<CONSOLE name=\"Stdout\" target=\"SYSTEM_OUT\">" + EOL +
            "\t\t\t<PatternLayout pattern=\"%d [%t] %-5level: %msg%n%throwable\"/>" + EOL +
            "\t\t\t<MarkerFilter onMatch=\"DENY\" onMisMatch=\"NEUTRAL\" marker=\"FLOW\"/>" + EOL +
            "\t\t</CONSOLE>" + EOL +
            "\t</Appenders>" + EOL +
            "\t<Loggers>" + EOL +
            "\t\t<Logger name=\"org.apache.logging.log4j\" level=\"DEBUG\" includeLocation=\"true\" additivity=\"false\">" + EOL +
            "\t\t\t<AppenderRef ref=\"Stdout\"/>" + EOL +
            "\t\t</Logger>" + EOL +
            "\t\t<Root level=\"ERROR\">" + EOL +
            "\t\t\t<AppenderRef ref=\"Stdout\"/>" + EOL +
            "\t\t</Root>" + EOL +
            "\t</Loggers>" + EOL +
            "</Configuration>" + EOL;

    // TODO make test run properly on Windows
    @Test
    public void testXmlConstructing() throws Exception {
        if (System.lineSeparator().length() == 1) { // Only run test on platforms with single character line endings (such as Linux), not on Windows
            final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
            addTestFixtures("config name", builder);
            final String xmlConfiguration = builder.toXmlConfiguration();
            assertEquals(expectedXml, xmlConfiguration);
        }
    }

}
