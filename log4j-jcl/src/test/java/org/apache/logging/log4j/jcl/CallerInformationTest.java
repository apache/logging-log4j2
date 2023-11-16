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
package org.apache.logging.log4j.jcl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.test.junit.SetTestProperty;
import org.apache.logging.log4j.test.junit.UsingStatusListener;
import org.junit.jupiter.api.Test;

@UsingStatusListener
@SetTestProperty(key = "log4j2.configurationFile", value = "org/apache/logging/log4j/jcl/CallerInformationTest.xml")
public class CallerInformationTest {

    @Test
    public void testClassLogger() throws Exception {
        final LoggerContext ctx = LoggerContext.getContext(false);
        final ListAppender app = ctx.getConfiguration().getAppender("Class");
        app.clear();
        final Log logger = LogFactory.getLog("ClassLogger");
        logger.info("Ignored message contents.");
        logger.warn("Verifying the caller class is still correct.");
        logger.error("Hopefully nobody breaks me!");
        final List<String> messages = app.getMessages();
        assertThat(messages).hasSize(3).allMatch(c -> getClass().getName().equals(c));
    }

    @Test
    public void testMethodLogger() throws Exception {
        final LoggerContext ctx = LoggerContext.getContext(false);
        final ListAppender app = ctx.getConfiguration().getAppender("Method");
        app.clear();
        final Log logger = LogFactory.getLog("MethodLogger");
        logger.info("More messages.");
        logger.warn("CATASTROPHE INCOMING!");
        logger.error("ZOMBIES!!!");
        logger.warn("brains~~~");
        logger.info("Itchy. Tasty.");
        final List<String> messages = app.getMessages();
        assertThat(messages).hasSize(5).allMatch("testMethodLogger"::equals);
    }
}
