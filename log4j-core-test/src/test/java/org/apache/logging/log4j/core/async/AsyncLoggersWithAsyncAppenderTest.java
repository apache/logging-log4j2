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
package org.apache.logging.log4j.core.async;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("AsyncLoggers")
@SetTestProperty(
        key = Constants.LOG4J_CONTEXT_SELECTOR,
        value = "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector")
@UsingStatusListener
public class AsyncLoggersWithAsyncAppenderTest {

    @Test
    @LoggerContextSource
    public void testLoggingWorks(final LoggerContext ctx) throws Exception {
        final Logger logger = ctx.getLogger(AsyncLoggersWithAsyncAppenderTest.class);
        logger.error("This {} a test", "is");
        logger.warn("Hello {}!", "world");
        final Appender appender = ctx.getConfiguration().getAppender("List");
        assertThat(appender).isInstanceOf(ListAppender.class);
        final ListAppender listAppender = (ListAppender) appender;
        final List<String> list = ((ListAppender) appender).getMessages(2, 1, TimeUnit.SECONDS);
        assertThat(list).as("Log events").hasSize(2);
        String msg = list.get(0);
        String expected = getClass().getName() + " This {} a test - [is] - This is a test";
        assertThat(msg).isEqualTo(expected);
        msg = list.get(1);
        expected = getClass().getName() + " Hello {}! - [world] - Hello world!";
        assertThat(msg).isEqualTo(expected);
    }
}
