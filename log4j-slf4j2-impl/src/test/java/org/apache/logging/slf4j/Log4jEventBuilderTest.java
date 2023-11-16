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
package org.apache.logging.slf4j;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Config from `log4j-core` test jar.
@LoggerContextSource("log4j2-config.xml")
public class Log4jEventBuilderTest {

    private final Logger logger;
    private final ListAppender appender;

    public Log4jEventBuilderTest(@Named("List") final Appender appender) {
        logger = LoggerFactory.getLogger("org.apache.test.Log4jEventBuilderTest");
        this.appender = (ListAppender) appender;
    }

    @BeforeEach
    public void setUp() {
        appender.clear();
    }

    @Test
    public void testKeyValuePairs() {
        logger.atDebug().addKeyValue("testKeyValuePairs", "ok").log();
        final List<LogEvent> events = appender.getEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getContextData().toMap()).containsEntry("testKeyValuePairs", "ok");
    }

    @Test
    public void testArguments() {
        logger.atDebug().setMessage("{}-{}").addArgument("a").addArgument("b").log();
        logger.atDebug().log("{}-{}", "a", "b");
        logger.atDebug().addArgument("a").log("{}-{}", "b");
        logger.atDebug().log("{}-{}", new Object[] {"a", "b"});
        assertThat(appender.getEvents()).hasSize(4).allMatch(event -> "a-b"
                .equals(event.getMessage().getFormattedMessage()));
    }
}
