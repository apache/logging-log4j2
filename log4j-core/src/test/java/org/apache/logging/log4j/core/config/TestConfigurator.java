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
package org.apache.logging.log4j.core.config;

import static org.apache.logging.log4j.hamcrest.MapMatchers.hasSize;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.theInstance;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.filter.CompositeFilter;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("functional")
public class TestConfigurator {

    private static final String CONFIG_NAME = "ConfigTest";

    private static final String FILESEP = System.getProperty("file.separator");


    private LoggerContext ctx = null;

    private static final String[] CHARS = new String[] {
        "aaaaaaaaaa",
        "bbbbbbbbbb",
        "cccccccccc",
        "dddddddddd",
        "eeeeeeeeee",
        "ffffffffff",
        "gggggggggg",
        "hhhhhhhhhh",
        "iiiiiiiiii",
        "jjjjjjjjjj",
        "kkkkkkkkkk",
        "llllllllll",
        "mmmmmmmmmm",
    };

    @AfterEach
    public void cleanup() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (ctx != null) {
            Configurator.shutdown(ctx);
            ctx = null;
        }
    }

    @Test
    public void testInitialize_Name_PathName() throws Exception {
        ctx = Configurator.initialize("Test1", "target/test-classes/log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testInitialize_Name_ClassLoader_URI() throws Exception {
        ctx = Configurator.initialize("Test1", null, new File("target/test-classes/log4j2-config.xml").toURI());
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testInitialize_InputStream_File() throws Exception {
        final File file = new File("target/test-classes/log4j2-config.xml");
        final InputStream is = new FileInputStream(file);
        final ConfigurationSource source = new ConfigurationSource(is, file);
        ctx = Configurator.initialize(null, source);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testInitialize_NullClassLoader_ConfigurationSourceWithInputStream_NoId() throws Exception {
        final InputStream is = new FileInputStream("target/test-classes/log4j2-config.xml");
        final ConfigurationSource source =
            new ConfigurationSource(is);
        ctx = Configurator.initialize(null, source);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testInitialize_Name_LocationName() throws Exception {
        ctx = Configurator.initialize("Test1", "log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testFromClassPathProperty() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY, "classpath:log4j2-config.xml");
        ctx = Configurator.initialize("Test1", null);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testFromClassPathWithClassPathPrefix() throws Exception {
        ctx = Configurator.initialize("Test1", "classpath:log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testFromClassPathWithClassLoaderPrefix() throws Exception {
        ctx = Configurator.initialize("Test1", "classloader:log4j2-config.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testByName() throws Exception {
        ctx = Configurator.initialize("-config", null);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }

    @Test
    public void testReconfiguration() throws Exception {
        final File file = new File("target/test-classes/log4j2-config.xml");
        assertTrue(file.setLastModified(System.currentTimeMillis() - 120000), "setLastModified should have succeeded.");
        ctx = Configurator.initialize("Test1", "target/test-classes/log4j2-config.xml");
        final Logger logger = LogManager.getLogger("org.apache.test.TestConfigurator");
        Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("Wrong configuration").is(new HamcrestCondition<>(hasKey("List")));

        // Sleep and check
        Thread.sleep(50);
        if (!file.setLastModified(System.currentTimeMillis())) {
            Thread.sleep(500);
        }
        assertTrue(file.setLastModified(System.currentTimeMillis()), "setLastModified should have succeeded.");
        TimeUnit.SECONDS.sleep(config.getWatchManager().getIntervalSeconds()+1);
        for (int i = 0; i < 17; ++i) {
            logger.debug("Test message " + i);
        }

        // Sleep and check
        Thread.sleep(50);
        if (is(theInstance(config)).matches(ctx.getConfiguration())) {
            Thread.sleep(500);
        }
        final Configuration newConfig = ctx.getConfiguration();
        assertThat(newConfig).describedAs("Configuration not reset").isNotSameAs(config);
        Configurator.shutdown(ctx);
        config = ctx.getConfiguration();
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(NullConfiguration.NULL_NAME);
    }


    @Test
    public void testEnvironment() throws Exception {
        ctx = Configurator.initialize("-config", null);
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Incorrect Configuration.").isEqualTo(CONFIG_NAME);
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders map should not be null.").isNotNull();
        assertThat(map).is(new HamcrestCondition<>(hasSize(greaterThan(0))));
        assertThat(map).describedAs("No ListAppender named List2").is(new HamcrestCondition<>(hasKey("List2")));
        final Appender app = map.get("List2");
        final Layout<? extends Serializable> layout = app.getLayout();
        assertThat(layout).describedAs("Appender List2 does not have a Layout").isNotNull();
        assertThat(layout).describedAs("Appender List2 is not configured with a PatternLayout").isInstanceOf(PatternLayout.class);
        final String pattern = ((PatternLayout) layout).getConversionPattern();
        assertThat(pattern).describedAs("No conversion pattern for List2 PatternLayout").isNotNull();
        assertFalse(pattern.startsWith("${env:PATH}"), "Environment variable was not substituted");
    }

    @Test
    public void testNoLoggers() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-loggers.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        final String name = DefaultConfiguration.DEFAULT_NAME + "@" + Integer.toHexString(config.hashCode());
        assertThat(config.getName()).describedAs("Unexpected Configuration.").isEqualTo(name);
    }

    @Test
    public void testBadStatus() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-status.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("XMLConfigTest");
        final LoggerConfig root = config.getLoggerConfig("");
        assertThat(root).describedAs("No Root Logger").isNotNull();
        assertThat(root.getLevel()).describedAs("Expected error level, was " + root.getLevel()).isSameAs(Level.ERROR);
    }

    @Test
    public void testBadFilterParam() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-badfilterparam.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("XMLConfigTest");
        final LoggerConfig lcfg = config.getLoggerConfig("org.apache.logging.log4j.test1");
        assertThat(lcfg).describedAs("No Logger").isNotNull();
        final Filter filter = lcfg.getFilter();
        assertThat(filter).describedAs("Unexpected Filter").isNull();
    }

    @Test
    public void testNoFilters() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-nofilter.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("XMLConfigTest");
        final LoggerConfig lcfg = config.getLoggerConfig("org.apache.logging.log4j.test1");
        assertThat(lcfg).describedAs("No Logger").isNotNull();
        final Filter filter = lcfg.getFilter();
        assertThat(filter).describedAs("No Filter").isNotNull();
        assertThat(filter).isInstanceOf(CompositeFilter.class);
        assertTrue(((CompositeFilter) filter).isEmpty(), "Unexpected filters");
    }

    @Test
    public void testBadLayout() throws Exception {
        ctx = Configurator.initialize("Test1", "bad/log4j-badlayout.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("XMLConfigTest");
    }

    @Test
    public void testBadFileName() throws Exception {
        final StringBuilder dir = new StringBuilder("/VeryLongDirectoryName");

        for (final String element : CHARS) {
            dir.append(element);
            dir.append(element.toUpperCase());
        }
        final String value = FILESEP.equals("/") ? dir.toString() + "/test.log" : "1:/target/bad:file.log";
        System.setProperty("testfile", value);
        ctx = Configurator.initialize("Test1", "bad/log4j-badfilename.xml");
        LogManager.getLogger("org.apache.test.TestConfigurator");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("XMLConfigTest");
        assertThat(config.getAppenders()).is(new HamcrestCondition<>(hasSize(equalTo(2))));
    }

    @Test
    public void testBuilder() throws Exception {
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("BuilderTest");
        builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
                .addAttribute("level", Level.DEBUG));
        final AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout").
                addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY,
                Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
        builder.add(appenderBuilder);
        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG).
                add(builder.newAppenderRef("Stdout")).
                addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));
        ctx = Configurator.initialize(builder.build());
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("BuilderTest");
        assertThat(config.getAppenders()).is(new HamcrestCondition<>(hasSize(equalTo(1))));
    }

    @Test
    public void testRolling() throws Exception {
        final ConfigurationBuilder< BuiltConfiguration > builder =
                ConfigurationBuilderFactory.newConfigurationBuilder();

        builder.setStatusLevel( Level.ERROR);
        builder.setConfigurationName("RollingBuilder");
        // create the console appender
        AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout").
                addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable"));
        builder.add( appenderBuilder );

        final LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
                .addAttribute("pattern", "%d [%t] %-5level: %msg%n");
        final ComponentBuilder triggeringPolicy = builder.newComponent("Policies")
                .addComponent(builder.newComponent("CronTriggeringPolicy").addAttribute("schedule", "0 0 0 * * ?"))
                .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
        appenderBuilder = builder.newAppender("rolling", "RollingFile")
                .addAttribute("fileName", "target/rolling.log")
                .addAttribute("filePattern", "target/archive/rolling-%d{MM-dd-yy}.log.gz")
                .add(layoutBuilder)
                .addComponent(triggeringPolicy);
        builder.add(appenderBuilder);

        // create the new logger
        builder.add( builder.newLogger( "TestLogger", Level.DEBUG )
                .add( builder.newAppenderRef( "rolling" ) )
                .addAttribute( "additivity", false ) );

        builder.add( builder.newRootLogger( Level.DEBUG )
                .add( builder.newAppenderRef( "rolling" ) ) );
        final Configuration config = builder.build();
        config.initialize();
        assertThat(config.<Appender>getAppender("rolling")).describedAs("No rolling file appender").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("RollingBuilder");
        // Initialize the new configuration
        final LoggerContext ctx = Configurator.initialize( config );
        Configurator.shutdown(ctx);

    }

    @Test
    public void testBuilderWithScripts() throws Exception {
        final String script = "if (logEvent.getLoggerName().equals(\"NoLocation\")) {\n" +
                "                return \"NoLocation\";\n" +
                "            } else if (logEvent.getMarker() != null && logEvent.getMarker().isInstanceOf(\"FLOW\")) {\n" +
                "                return \"Flow\";\n" +
                "            } else {\n" +
                "                return null;\n" +
                "            }";
        final ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName("BuilderTest");
        builder.add(builder.newScriptFile("filter.groovy", "target/test-classes/scripts/filter.groovy").addIsWatched(true));
        final AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
                ConsoleAppender.Target.SYSTEM_OUT);
        appenderBuilder.add(builder.newLayout("PatternLayout").
                addComponent(builder.newComponent("ScriptPatternSelector")
                        .addAttribute("defaultPattern", "[%-5level] %c{1.} %C{1.}.%M.%L %msg%n")
                        .addComponent(builder.newComponent("PatternMatch").addAttribute("key", "NoLocation")
                                .addAttribute("pattern", "[%-5level] %c{1.} %msg%n"))
                        .addComponent(builder.newComponent("PatternMatch").addAttribute("key", "FLOW")
                                .addAttribute("pattern", "[%-5level] %c{1.} ====== %C{1.}.%M:%L %msg ======%n"))
                        .addComponent(builder.newComponent("selectorScript", "Script", script).addAttribute("language", "beanshell"))));
        appenderBuilder.add(builder.newFilter("ScriptFilter", Filter.Result.DENY,
                Filter.Result.NEUTRAL).addComponent(builder.newComponent("ScriptRef").addAttribute("ref", "filter.groovy")));
        builder.add(appenderBuilder);
        builder.add(builder.newLogger("org.apache.logging.log4j", Level.DEBUG).
                add(builder.newAppenderRef("Stdout")).
                addAttribute("additivity", false));
        builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));
        ctx = Configurator.initialize(builder.build());
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("No configuration").isNotNull();
        assertThat(config.getName()).describedAs("Unexpected Configuration").isEqualTo("BuilderTest");
        assertThat(config.getAppenders()).is(new HamcrestCondition<>(hasSize(equalTo(1))));
        assertThat(config.getScriptManager().getScript("filter.groovy")).describedAs("Filter script not found").isNotNull();
        assertThat(config.getScriptManager().getScript("selectorScript")).describedAs("pattern selector script not found").isNotNull();
    }

}
