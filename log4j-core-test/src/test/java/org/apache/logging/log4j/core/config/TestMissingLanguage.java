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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.util.Constants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junitpioneer.jupiter.SetSystemProperty;

@SetSystemProperty(key = Constants.SCRIPT_LANGUAGES, value = "beanshell, Javascript")
@Tag("functional")
public class TestMissingLanguage {

    private LoggerContext ctx = null;

    @AfterEach
    public void cleanup() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (ctx != null) {
            Configurator.shutdown(ctx);
            ctx = null;
        }
    }

    @org.junit.jupiter.api.Test
    public void testBuilderWithScripts() throws Exception {
        final String script =
                "if (logEvent.getLoggerName().equals(\"NoLocation\")) {\n" + "                return \"NoLocation\";\n"
                        + "            } else if (logEvent.getMarker() != null && logEvent.getMarker().isInstanceOf(\"FLOW\")) {\n"
                        + "                return \"Flow\";\n"
                        + "            } else {\n"
                        + "                return null;\n"
                        + "            }";
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("BuilderTest");
        builder.add(builder.newScriptFile("filter.groovy", "target/test-classes/scripts/filter.groovy")
                .addIsWatched(true));
        final AppenderComponentBuilder appenderBuilder =
                builder.newAppender("Stdout", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout")
                .addComponent(builder.newComponent("ScriptPatternSelector")
                        .addAttribute("defaultPattern", "[%-5level] %c{1.} %C{1.}.%M.%L %msg%n")
                        .addComponent(builder.newComponent("PatternMatch")
                                .addAttribute("key", "NoLocation")
                                .addAttribute("pattern", "[%-5level] %c{1.} %msg%n"))
                        .addComponent(builder.newComponent("PatternMatch")
                                .addAttribute("key", "FLOW")
                                .addAttribute("pattern", "[%-5level] %c{1.} ====== %C{1.}.%M:%L %msg ======%n"))
                        .addComponent(builder.newComponent("selectorScript", "Script", script)
                                .addAttribute("language", "beanshell"))));
        appenderBuilder.add(builder.newFilter("ScriptFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
                .addComponent(builder.newComponent("ScriptRef").addAttribute("ref", "filter.groovy")));
        builder.add(appenderBuilder);
        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG)
                .add(builder.newAppenderRef("Stdout"))
                .addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));
        ctx = Configurator.initialize(builder.build());
        final Configuration config = ctx.getConfiguration();
        assertNotNull(config.getScriptManager(), "No ScriptManager");
        assertNull(config.getScriptManager().getScript("filter.groovy"), "Script should not be present");
    }
}
