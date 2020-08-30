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

import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.junit.jupiter.api.Test;

import static org.apache.logging.log4j.hamcrest.MapMatchers.hasSize;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@LoggerContextSource("missingRootLogger.xml")
public class MissingRootLoggerTest {

    @Test
    public void testMissingRootLogger(final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger("sample.Logger1");
        assertTrue(logger.isInfoEnabled(), "Logger should have the INFO level enabled");
        assertFalse(logger.isDebugEnabled(), "Logger should have the DEBUG level disabled");
        final Configuration config = ctx.getConfiguration();
        assertNotNull(config, "Config not null");
//        final String MISSINGROOT = "MissingRootTest";
//        assertTrue("Incorrect Configuration. Expected " + MISSINGROOT + " but found " + config.getName(),
//                MISSINGROOT.equals(config.getName()));
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull(map, "Appenders not null");
        assertThat("There should only be two appenders", map, hasSize(2));
        assertThat(map, hasKey("List"));
        assertThat(map, hasKey("DefaultConsole-2"));

        final Map<String, LoggerConfig> loggerMap = config.getLoggers();
        assertNotNull(loggerMap, "loggerMap not null");
        assertThat("There should only be one configured logger", loggerMap, hasSize(1));
        // only the sample logger, no root logger in loggerMap!
        assertThat("contains key=sample", loggerMap, hasKey("sample"));

        final LoggerConfig sample = loggerMap.get("sample");
        final Map<String, Appender> sampleAppenders = sample.getAppenders();
        assertThat("The sample logger should only have one appender", sampleAppenders, hasSize(1));
        // sample only has List appender, not Console!
        assertThat("The sample appender should be a ListAppender", sampleAppenders, hasKey("List"));
        assertThat(config, is(instanceOf(AbstractConfiguration.class)));
        final AbstractConfiguration baseConfig = (AbstractConfiguration) config;
        final LoggerConfig root = baseConfig.getRootLogger();
        final Map<String, Appender> rootAppenders = root.getAppenders();
        assertThat("The root logger should only have one appender", rootAppenders, hasSize(1));
        // root only has Console appender!
        assertThat("The root appender should be a ConsoleAppender", rootAppenders, hasKey("DefaultConsole-2"));
        assertEquals(Level.ERROR, root.getLevel());
    }

}
