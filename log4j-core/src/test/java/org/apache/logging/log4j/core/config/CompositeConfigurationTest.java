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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.composite.CompositeConfiguration;
import org.apache.logging.log4j.junit.LoggerContextRule;
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
                assertEquals("Expected 2 Appender references for cat1 but got " + appendersMap.size(), 2,
                        appendersMap.size());
                assertTrue(appendersMap.get("STDOUT") instanceof ConsoleAppender);

                appendersMap = config.getLogger("cat2").getAppenders();
                assertEquals("Expected 1 Appender reference for cat2 but got " + appendersMap.size(), 1,
                        appendersMap.size());
                assertTrue(appendersMap.get("File") instanceof FileAppender);

                appendersMap = config.getLogger("cat3").getAppenders();
                assertEquals("Expected 1 Appender reference for cat3 but got " + appendersMap.size(), 1,
                        appendersMap.size());
                assertTrue(appendersMap.get("File") instanceof FileAppender);

                appendersMap = config.getRootLogger().getAppenders();
                assertEquals("Expected 2 Appender references for the root logger but got " + appendersMap.size(), 2,
                        appendersMap.size());
                assertTrue(appendersMap.get("File") instanceof FileAppender);
                assertTrue(appendersMap.get("STDOUT") instanceof ConsoleAppender);
            }
        };
        runTest(lcr, test);
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

    private void runTest(final LoggerContextRule rule, final Statement statement) {
        try {
            rule.apply(statement, Description
                    .createTestDescription(getClass(), Thread.currentThread().getStackTrace()[1].getMethodName()))
                    .evaluate();
        } catch (final Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
