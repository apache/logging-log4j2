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

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.ClassRule;

/**
 *
 */
public class SequenceNumberPatternConverterZeroPaddedTest {

    @ClassRule
    public static LoggerContextRule ctx = new LoggerContextRule("SequenceNumberPatternConverterZeroPaddedTest.yml");

    @Before
    public void before() {
      ctx.getListAppender("Padded").clear();
    }

    @Test
    public void testPaddedSequence() throws Exception {
        final Logger logger = ctx.getLogger();
        logger.info("Message 1");
        logger.info("Message 2");
        logger.info("Message 3");
        logger.info("Message 4");
        logger.info("Message 5");

        final ListAppender app = ctx.getListAppender("Padded");
        final List<String> messages = app.getMessages();
        System.out.println("Written messages "+messages);
        assertThat(messages, contains("001", "002", "003", "004", "005"));
    }
}
