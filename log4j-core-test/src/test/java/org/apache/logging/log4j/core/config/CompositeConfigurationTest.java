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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.core.filter.RegexFilter;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

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
    @LoggerContextSource("classpath:log4j-comp-logger.xml,log4j-comp-logger.json")
    public void compositeLogger(final LoggerContext lcr) {
        final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
        Map<String, Appender> appendersMap = config.getLogger("cat1").getAppenders();
        assertEquals(2, appendersMap.size(), "Expected 2 Appender references for cat1 but got " + appendersMap.size());
        assertTrue(appendersMap.get("STDOUT") instanceof ConsoleAppender);

        final Filter loggerFilter = config.getLogger("cat1").getFilter();
        assertTrue(loggerFilter instanceof RegexFilter);
        assertEquals(Filter.Result.DENY, loggerFilter.getOnMatch());

        appendersMap = config.getLogger("cat2").getAppenders();
        assertEquals(1, appendersMap.size(), "Expected 1 Appender reference for cat2 but got " + appendersMap.size());
        assertTrue(appendersMap.get("File") instanceof FileAppender);

        appendersMap = config.getLogger("cat3").getAppenders();
        assertEquals(1, appendersMap.size(), "Expected 1 Appender reference for cat3 but got " + appendersMap.size());
        assertTrue(appendersMap.get("File") instanceof FileAppender);

        appendersMap = config.getRootLogger().getAppenders();
        assertEquals(
                2,
                appendersMap.size(),
                "Expected 2 Appender references for the root logger but got " + appendersMap.size());
        assertTrue(appendersMap.get("File") instanceof FileAppender);
        assertTrue(appendersMap.get("STDOUT") instanceof ConsoleAppender);

        assertEquals(
                ConfigurationSource.COMPOSITE_SOURCE,
                config.getConfigurationSource(),
                "Expected COMPOSITE_SOURCE for composite configuration but got " + config.getConfigurationSource());
    }

    @Test
    @LoggerContextSource("classpath:log4j-comp-root-loggers.xml,log4j-comp-logger.json")
    public void testAttributeCheckWhenMergingConfigurations(final LoggerContext lcr) {
        try {
            final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
            assertNotNull(config);
        } catch (final NullPointerException e) {
            fail("Should not throw NullPointerException when there are different nodes.");
        }
    }

    @Test
    @LoggerContextSource("classpath:log4j-comp-logger-root.xml,log4j-comp-logger-attr-override.json")
    public void testAttributeMergeForLoggers(final LoggerContext lcr) {
        final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
        // Test for Root log level override
        assertEquals(Level.WARN, config.getRootLogger().getLevel(), "Expected Root logger log level to be WARN");

        // Test for cat2 level override
        final LoggerConfig cat2 = config.getLogger("cat2");
        assertEquals(Level.INFO, cat2.getLevel(), "Expected cat2 log level to be INFO");

        // Test for cat2 additivity override
        assertTrue(cat2.isAdditive(), "Expected cat2 additivity to be true");

        // Regression
        // Check level on cat3 (not present in root config)
        assertEquals(Level.ERROR, config.getLogger("cat3").getLevel(), "Expected cat3 log level to be ERROR");
        // Check level on cat1 (not present in overridden config)
        assertEquals(Level.DEBUG, config.getLogger("cat1").getLevel(), "Expected cat1 log level to be DEBUG");
    }

    @Test
    @LoggerContextSource("classpath:log4j-comp-logger-root.xml,log4j-does-not-exist.json")
    public void testMissingConfig(final LoggerContext lcr) {
        final AbstractConfiguration config = (AbstractConfiguration) lcr.getConfiguration();
        assertNotNull(config, "No configuration returned");
        // Test for Root log level override
        assertEquals(Level.ERROR, config.getRootLogger().getLevel(), "Expected Root logger log level to be ERROR");

        // Test for no cat2 level override
        final LoggerConfig cat2 = config.getLogger("cat2");
        assertEquals(Level.DEBUG, cat2.getLevel(), "Expected cat2 log level to be INFO");
    }

    @Test
    @LoggerContextSource("classpath:log4j-comp-logger-ref.xml,log4j-comp-logger-ref.json")
    public void testAppenderRefFilterMerge(final LoggerContext lcr) {
        final CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();

        final List<AppenderRef> appenderRefList = config.getLogger("cat1").getAppenderRefs();
        final AppenderRef appenderRef = getAppenderRef(appenderRefList, "STDOUT");
        assertTrue(
                appenderRef.getFilter() != null && appenderRef.getFilter() instanceof RegexFilter,
                "Expected cat1 STDOUT appenderRef to have a regex filter");
    }

    private AppenderRef getAppenderRef(final List<AppenderRef> appenderRefList, final String refName) {
        for (final AppenderRef ref : appenderRefList) {
            if (ref.getRef().equalsIgnoreCase(refName)) {
                return ref;
            }
        }
        return null;
    }
    /*
    @Test
    public void overrideFilter() {
        final LoggerContextRule lcr = new LoggerContextRule("classpath:log4j-comp-filter.xml,log4j-comp-filter.json");
        Statement test = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                CompositeConfiguration config = (CompositeConfiguration) lcr.getConfiguration();
                assertTrue(config.getFilter() instanceof CompositeFilter);
                CompositeFilter filter = (CompositeFilter) config.getFilter();
                assertTrue(filter.getFiltersArray().length == 2);
            }
        };
        runTest(lcr, test);
    }

    @Test
    public void testReconfiguration() throws Exception {
        final LoggerContextRule rule =
                new LoggerContextRule("classpath:log4j-comp-reconfig.xml,log4j-comp-reconfig.properties");
        Statement test = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final Configuration oldConfig = rule.getConfiguration();
                final org.apache.logging.log4j.Logger logger = rule.getLogger("LoggerTest");
                final int MONITOR_INTERVAL_SECONDS = 5;
                final File file = new File("target/test-classes/log4j-comp-reconfig.properties");
                final long orig = file.lastModified();
                final long newTime = orig + 10000;
                assertTrue("setLastModified should have succeeded.", file.setLastModified(newTime));
                TimeUnit.SECONDS.sleep(MONITOR_INTERVAL_SECONDS + 1);
                for (int i = 0; i < 17; ++i) {
                    logger.debug("Reconfigure");
                }
                int loopCount = 0;
                Configuration newConfig;
                do {
                    Thread.sleep(100);
                    newConfig = rule.getConfiguration();
                    ++loopCount;
                } while (newConfig == oldConfig && loopCount <= 5);
                assertNotSame("Reconfiguration failed", newConfig, oldConfig);
            }
        };
        runTest(rule, test);

    } */
}
