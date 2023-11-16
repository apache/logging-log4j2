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
package org.apache.logging.log4j.core;

import static org.junit.Assert.assertTrue;

import java.util.List;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Class Description goes here.
 */
public class PatternVariableResolverTest {

    private static final String CONFIG = "log4j2-pattern-layout.xml";
    private ListAppender listAppender;

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    @Before
    public void before() {
        listAppender = context.getRequiredAppender("list", ListAppender.class);
    }

    @Test
    public void testFileName() {
        final Logger logger = context.getLogger(PatternVariableResolverTest.class);
        logger.info("This is a test");
        final List<String> messages = listAppender.getMessages();
        assertTrue("No messages returned", messages != null && messages.size() > 0);
        final String message = messages.get(0);
        System.out.println(message);
    }
}
