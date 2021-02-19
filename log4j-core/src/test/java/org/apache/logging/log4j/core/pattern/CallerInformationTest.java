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
package org.apache.logging.log4j.core.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.junit.LoggerContextSource;
import org.apache.logging.log4j.junit.Named;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.jupiter.api.Test;

@LoggerContextSource("log4j2-calling-class.xml")
public class CallerInformationTest {

    @Test
    public void testClassLogger(final LoggerContext context, @Named("Class") final ListAppender app) {
        app.clear();
        final Logger logger = context.getLogger("ClassLogger");
        logger.info("Ignored message contents.");
        logger.warn("Verifying the caller class is still correct.");
        logger.error("Hopefully nobody breaks me!");
        final List<String> messages = app.getMessages();
        assertThat(messages.size()).describedAs("Incorrect number of messages.").isEqualTo(3);
        for (final String message : messages) {
            assertThat(message).describedAs("Incorrect caller class name.").isEqualTo(this.getClass().getName());
        }
    }

    @Test
    public void testMethodLogger(final LoggerContext context, @Named("Method") final ListAppender app) {
        app.clear();
        final Logger logger = context.getLogger("MethodLogger");
        logger.info("More messages.");
        logger.warn("CATASTROPHE INCOMING!");
        logger.error("ZOMBIES!!!");
        logger.fatal("brains~~~");
        logger.info("Itchy. Tasty.");
        final List<String> messages = app.getMessages();
        assertThat(messages.size()).describedAs("Incorrect number of messages.").isEqualTo(5);
        for (final String message : messages) {
            assertThat(message).describedAs("Incorrect caller method name.").isEqualTo("testMethodLogger");
        }
    }
}
