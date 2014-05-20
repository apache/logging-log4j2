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
import org.apache.logging.log4j.junit.InitialLoggerContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class MissingRootLoggerTest {

    @Rule
    public InitialLoggerContext context = new InitialLoggerContext("missingRootLogger.xml");

    @Test
    public void testMissingRootLogger() throws Exception {
        final LoggerContext ctx = context.getContext();
        final Logger logger = ctx.getLogger("sample.Logger1");
        assertTrue("Logger should have the INFO level enabled", logger.isInfoEnabled());
        assertFalse("Logger should have the DEBUG level disabled", logger.isDebugEnabled());
        final Configuration config = ctx.getConfiguration();
        assertNotNull("Config not null", config);
//        final String MISSINGROOT = "MissingRootTest";
//        assertTrue("Incorrect Configuration. Expected " + MISSINGROOT + " but found " + config.getName(),
//                MISSINGROOT.equals(config.getName()));
        final Map<String, Appender> map = config.getAppenders();
        assertNotNull("Appenders not null", map);
        assertEquals("There should only be two appenders", 2, map.size());
        assertTrue("Contains List", map.containsKey("List"));
        assertTrue("Contains Console", map.containsKey("Console"));

        final Map<String, LoggerConfig> loggerMap = config.getLoggers();
        assertNotNull("loggerMap not null", loggerMap);
        assertEquals("There should only be one configured logger", 1, loggerMap.size());
        // only the sample logger, no root logger in loggerMap!
        assertTrue("contains key=sample", loggerMap.containsKey("sample"));

        final LoggerConfig sample = loggerMap.get("sample");
        final Map<String, Appender> sampleAppenders = sample.getAppenders();
        assertEquals("The sample logger should only have one appender", 1, sampleAppenders.size());
        // sample only has List appender, not Console!
        assertTrue("The sample appender should be a ListAppender", sampleAppenders.containsKey("List"));

        final AbstractConfiguration baseConfig = (AbstractConfiguration) config;
        final LoggerConfig root = baseConfig.getRootLogger();
        final Map<String, Appender> rootAppenders = root.getAppenders();
        assertEquals("The root logger should only have one appender", 1, rootAppenders.size());
        // root only has Console appender!
        assertTrue("The root appender should be a ConsoleAppender", rootAppenders.containsKey("Console"));
        assertEquals(Level.ERROR, root.getLevel());
    }

}
