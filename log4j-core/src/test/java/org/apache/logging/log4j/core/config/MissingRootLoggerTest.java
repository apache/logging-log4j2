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
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.assertj.core.api.HamcrestCondition;
import org.junit.jupiter.api.Test;

@LoggerContextSource("missingRootLogger.xml")
public class MissingRootLoggerTest {

    @Test
    public void testMissingRootLogger(final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger("sample.Logger1");
        assertTrue(logger.isInfoEnabled(), "Logger should have the INFO level enabled");
        assertFalse(logger.isDebugEnabled(), "Logger should have the DEBUG level disabled");
        final Configuration config = ctx.getConfiguration();
        assertThat(config).describedAs("Config not null").isNotNull();
//        final String MISSINGROOT = "MissingRootTest";
//        assertTrue("Incorrect Configuration. Expected " + MISSINGROOT + " but found " + config.getName(),
//                MISSINGROOT.equals(config.getName()));
        final Map<String, Appender> map = config.getAppenders();
        assertThat(map).describedAs("Appenders not null").isNotNull();
        assertThat(map).describedAs("There should only be two appenders").is(new HamcrestCondition<>(hasSize(2)));
        assertThat(map).is(new HamcrestCondition<>(hasKey("List")));
        assertThat(map).is(new HamcrestCondition<>(hasKey("DefaultConsole-2")));

        final Map<String, LoggerConfig> loggerMap = config.getLoggers();
        assertThat(loggerMap).describedAs("loggerMap not null").isNotNull();
        assertThat(loggerMap).describedAs("There should only be one configured logger").is(new HamcrestCondition<>(hasSize(1)));
        // only the sample logger, no root logger in loggerMap!
        assertThat(loggerMap).describedAs("contains key=sample").is(new HamcrestCondition<>(hasKey("sample")));

        final LoggerConfig sample = loggerMap.get("sample");
        final Map<String, Appender> sampleAppenders = sample.getAppenders();
        assertThat(sampleAppenders).describedAs("The sample logger should only have one appender").is(new HamcrestCondition<>(hasSize(1)));
        // sample only has List appender, not Console!
        assertThat(sampleAppenders).describedAs("The sample appender should be a ListAppender").is(new HamcrestCondition<>(hasKey("List")));
        assertThat(config).isInstanceOf(AbstractConfiguration.class);
        final AbstractConfiguration baseConfig = (AbstractConfiguration) config;
        final LoggerConfig root = baseConfig.getRootLogger();
        final Map<String, Appender> rootAppenders = root.getAppenders();
        assertThat(rootAppenders).describedAs("The root logger should only have one appender").is(new HamcrestCondition<>(hasSize(1)));
        // root only has Console appender!
        assertThat(rootAppenders).describedAs("The root appender should be a ConsoleAppender").is(new HamcrestCondition<>(hasKey("DefaultConsole-2")));
        assertThat(root.getLevel()).isEqualTo(Level.ERROR);
    }

}
