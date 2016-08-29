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

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.filter.ThreadContextMapFilter;
import org.apache.logging.log4j.junit.CleanFiles;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.util.Strings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.apache.logging.log4j.hamcrest.MapMatchers.hasSize;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;

/**
 * Tests XInclude.
 */
@RunWith(Parameterized.class)
public class XIncludeTest {

    private static final String LOGGER_NAME = "org.apache.logging.log4j.test1.Test";
    private static final String FILE_LOGGER_NAME = "org.apache.logging.log4j.test2.Test";
    private static final String APPENDER_NAME = "STDOUT";

    private final String logFileName;

    @Rule
    public TestRule rules;

    private final LoggerContextRule init;

    private LoggerContext ctx;

    private final SecureRandom random = new SecureRandom();

    public XIncludeTest(final String configFileName, final String logFileName) {
        this.logFileName = logFileName;
        this.init = new LoggerContextRule(configFileName);
        this.rules = RuleChain.outerRule(new CleanFiles(logFileName)).around(this.init);
    }

    @Parameters(name = "config={0}, log={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        {"classpath:log4j-xinclude.xml", "target/test-xinclude.log"},
                }
        );
    }

    @Before
    public void setUp() throws Exception {
        this.ctx = this.init.getLoggerContext();
    }

    @Test
    public void testConfiguredAppenders() throws Exception {
        final Configuration configuration = this.ctx.getConfiguration();
        final Map<String, Appender> appenders = configuration.getAppenders();
        assertThat(appenders, is(notNullValue()));
        assertThat(appenders.size(), is(equalTo(3)));
    }

    @Test
    public void testLogger() throws Exception {
        final Logger logger = this.ctx.getLogger(LOGGER_NAME);
        assertThat(logger, is(instanceOf(org.apache.logging.log4j.core.Logger.class)));
        final org.apache.logging.log4j.core.Logger l = (org.apache.logging.log4j.core.Logger) logger;
        assertThat(l.getLevel(), is(equalTo(Level.DEBUG)));
        assertThat(l.filterCount(), is(equalTo(1)));
        final Iterator<Filter> iterator = l.getFilters();
        assertThat(iterator.hasNext(), is(true));
        final Filter filter = iterator.next();
        assertThat(filter, is(instanceOf(ThreadContextMapFilter.class)));
        final Map<String, Appender> appenders = l.getAppenders();
        assertThat(appenders, is(notNullValue()));
        assertThat(appenders, hasSize(1));
        final Appender appender = appenders.get(APPENDER_NAME);
        assertThat(appender, is(notNullValue()));
        assertThat(appender.getName(), is(equalTo("STDOUT")));
    }

    @Test
    public void testLogToFile() throws Exception {
        final Logger logger = this.ctx.getLogger(FILE_LOGGER_NAME);
        final long random = this.random.nextLong();
        logger.debug("This is test message number {}", random);
        int count = 0;
        String line = Strings.EMPTY;
        try (BufferedReader in = new BufferedReader(new FileReader(this.logFileName))) {
            while (in.ready()) {
                ++count;
                line = in.readLine();
            }
        }
        assertThat(count, is(equalTo(1)));
        assertThat(line, endsWith(Long.toString(random)));
    }

}
