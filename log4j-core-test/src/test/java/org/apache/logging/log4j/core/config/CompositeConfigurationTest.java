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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.filter.RegexFilter;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.apache.logging.log4j.core.util.Throwables;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CompositeConfigurationTest {
    /*
        @Test
        public void compositeConfigurationUsed() {
            final LoggerContextRule lcr = new LoggerContextRule(
                    "classpath:log4j-comp-appender.xml,log4j-comp-appender.json");
            Statement test = new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    assertTrue(lcr.getConfiguration() instanceof CompositeConfiguration);
                }
            };
            runTest(lcr, test);
        }

        @Test
        public void compositeProperties() {
            final LoggerContextRule lcr = new LoggerContextRule(
                    "classpath:log4j-comp-properties.xml,log4j-comp-properties.json");
            Statement test = new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                    assertEquals("json", config.getStrSubstitutor().replace("${propertyShared}"));
                    assertEquals("xml", config.getStrSubstitutor().replace("${propertyXml}"));
                    assertEquals("json", config.getStrSubstitutor().replace("${propertyJson}"));
                }
            };
            runTest(lcr, test);
        }

        @Test
        public void compositeAppenders() {
            final LoggerContextRule lcr = new LoggerContextRule(
                    "classpath:log4j-comp-appender.xml,log4j-comp-appender.json");
            Statement test = new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                    Map<String, Appender> appender = config.getAppenders();
                    assertEquals(3, appender.size());
                    assertTrue(appender.get("STDOUT") instanceof ConsoleAppender);
                    assertTrue(appender.get("File") instanceof FileAppender);
                    assertTrue(appender.get("Override") instanceof RollingFileAppender);
                }
            };
            runTest(lcr, test);
        }
    */
    @Test
    public void compositeLogger() {
        final LoggerContextRule lcr = new LoggerContextRule("classpath:log4j-comp-logger.xml,log4j-comp-logger.json");
        final Statement test = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                Map<String, Appender> appendersMap = config.getLogger("cat1").getAppenders();
                assertEquals(
                        "Expected 2 Appender references for cat1 but got " + appendersMap.size(),
                        2,
                        appendersMap.size());
                assertTrue(appendersMap.get("STDOUT") instanceof ConsoleAppender);

                final Filter loggerFilter = config.getLogger("cat1").getFilter();
                assertTrue(loggerFilter instanceof RegexFilter);
                assertEquals(loggerFilter.getOnMatch(), Filter.Result.DENY);

                appendersMap = config.getLogger("cat2").getAppenders();
                assertEquals(
                        "Expected 1 Appender reference for cat2 but got " + appendersMap.size(),
                        1,
                        appendersMap.size());
                assertTrue(appendersMap.get("File") instanceof FileAppender);

                appendersMap = config.getLogger("cat3").getAppenders();
                assertEquals(
                        "Expected 1 Appender reference for cat3 but got " + appendersMap.size(),
                        1,
                        appendersMap.size());
                assertTrue(appendersMap.get("File") instanceof FileAppender);

                appendersMap = config.getRootLogger().getAppenders();
                assertEquals(
                        "Expected 2 Appender references for the root logger but got " + appendersMap.size(),
                        2,
                        appendersMap.size());
                assertTrue(appendersMap.get("File") instanceof FileAppender);
                assertTrue(appendersMap.get("STDOUT") instanceof ConsoleAppender);

                assertEquals(
                        "Expected COMPOSITE_SOURCE for composite configuration but got "
                                + config.getConfigurationSource(),
                        config.getConfigurationSource(),
                        ConfigurationSource.COMPOSITE_SOURCE);
            }
        };
        runTest(lcr, test);
    }

    @Test
    public void testAttributeCheckWhenMergingConfigurations() {
        final LoggerContextRule lcr =
                new LoggerContextRule("classpath:log4j-comp-root-loggers.xml,log4j-comp-logger.json");
        final Statement test = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                    Assert.assertNotNull(config);
                } catch (final NullPointerException e) {
                    fail("Should not throw NullPointerException when there are different nodes.");
                }
            }
        };
        runTest(lcr, test);
    }

    @Test
    public void testAttributeMergeForLoggers() {
        final LoggerContextRule lcr =
                new LoggerContextRule("classpath:log4j-comp-logger-root.xml,log4j-comp-logger-attr-override.json");
        final Statement test = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                // Test for Root log level override
                assertEquals(
                        "Expected Root logger log level to be WARN",
                        Level.WARN,
                        config.getRootLogger().getLevel());

                // Test for cat2 level override
                final LoggerConfig cat2 = config.getLogger("cat2");
                assertEquals("Expected cat2 log level to be INFO", Level.INFO, cat2.getLevel());

                // Test for cat2 additivity override
                assertTrue("Expected cat2 additivity to be true", cat2.isAdditive());

                // Regression
                // Check level on cat3 (not present in root config)
                assertEquals(
                        "Expected cat3 log level to be ERROR",
                        Level.ERROR,
                        config.getLogger("cat3").getLevel());
                // Check level on cat1 (not present in overridden config)
                assertEquals(
                        "Expected cat1 log level to be DEBUG",
                        Level.DEBUG,
                        config.getLogger("cat1").getLevel());
            }
        };
        runTest(lcr, test);
    }

    @Test
    public void testMissingConfig() {
        final LoggerContextRule lcr =
                new LoggerContextRule("classpath:log4j-comp-logger-root.xml,log4j-does-not-exist.json");
        final Statement test = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final AbstractConfiguration config = (AbstractConfiguration) lcr.getConfiguration();
                assertNotNull("No configuration returned", config);
                // Test for Root log level override
                assertEquals(
                        "Expected Root logger log level to be ERROR",
                        Level.ERROR,
                        config.getRootLogger().getLevel());

                // Test for no cat2 level override
                final LoggerConfig cat2 = config.getLogger("cat2");
                assertEquals("Expected cat2 log level to be INFO", Level.DEBUG, cat2.getLevel());
            }
        };
        runTest(lcr, test);
    }

    @Test
    public void testAppenderRefFilterMerge() {
        final LoggerContextRule lcr =
                new LoggerContextRule("classpath:log4j-comp-logger-ref.xml,log4j-comp-logger-ref.json");
        final Statement test = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();

                final List<AppenderRef> appenderRefList =
                        config.getLogger("cat1").getAppenderRefs();
                final AppenderRef appenderRef = getAppenderRef(appenderRefList, "STDOUT");
                assertTrue(
                        "Expected cat1 STDOUT appenderRef to have a regex filter",
                        appenderRef.getFilter() != null && appenderRef.getFilter() instanceof RegexFilter);
            }
        };
        runTest(lcr, test);
    }

    private AppenderRef getAppenderRef(final List<AppenderRef> appenderRefList, final String refName) {
        for (final AppenderRef ref : appenderRefList) {
            if (ref.getRef().equalsIgnoreCase(refName)) {
                return ref;
            }
        }
        return null;
    }

    private void runTest(final LoggerContextRule rule, final Statement statement) {
        try {
            rule.apply(
                            statement,
                            Description.createTestDescription(
                                    getClass(),
                                    Thread.currentThread().getStackTrace()[1].getMethodName()))
                    .evaluate();
        } catch (final Throwable e) {
            Throwables.rethrow(e);
        }
    }
}
