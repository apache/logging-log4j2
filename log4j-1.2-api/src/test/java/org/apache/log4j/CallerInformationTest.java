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
package org.apache.log4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;

public class CallerInformationTest {

    // config from log4j-core test-jar
    private static final String CONFIG = "log4j2-calling-class.xml";

    @ClassRule
    public static final LoggerContextRule ctx = new LoggerContextRule(CONFIG);

    @Test
    public void testClassLogger() throws Exception {
        final ListAppender app = ctx.getListAppender("Class").clear();
        final Logger logger = Logger.getLogger("ClassLogger");
        logger.info("Ignored message contents.");
        logger.warn("Verifying the caller class is still correct.");
        logger.error("Hopefully nobody breaks me!");
        final List<String> messages = app.getMessages();
        assertThat(messages).describedAs("Incorrect number of messages.").hasSize(3);
        for (final String message : messages) {
            assertThat(message).describedAs("Incorrect caller class name.").isEqualTo(this.getClass().getName());
        }
    }

    @Test
    public void testMethodLogger() throws Exception {
        final ListAppender app = ctx.getListAppender("Method").clear();
        final Logger logger = Logger.getLogger("MethodLogger");
        logger.info("More messages.");
        logger.warn("CATASTROPHE INCOMING!");
        logger.error("ZOMBIES!!!");
        logger.warn("brains~~~");
        logger.info("Itchy. Tasty.");
        final List<String> messages = app.getMessages();
        assertThat(messages).describedAs("Incorrect number of messages.").hasSize(5);
        for (final String message : messages) {
            assertThat(message).describedAs("Incorrect caller method name.").isEqualTo("testMethodLogger");
        }
    }
}
