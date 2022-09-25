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
package org.apache.logging.log4j.core;

import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

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
        Logger logger = context.getLogger(PatternVariableResolverTest.class);
        logger.info("This is a test");
        List<String> messages = listAppender.getMessages();
        assertTrue("No messages returned", messages != null && messages.size() > 0);
        String message = messages.get(0);
        System.out.println(message);
    }
}
