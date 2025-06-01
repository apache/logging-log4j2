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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.apache.logging.log4j.core.test.junit.Named;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Class Description goes here.
 */
@LoggerContextSource("log4j2-pattern-layout.xml")
public class PatternVariableResolverTest {

    private ListAppender listAppender;

    @BeforeEach
    public void beforeEach(@Named("list") final ListAppender appender) {
        listAppender = appender;
    }

    @Test
    public void testFileName(LoggerContext context) {
        final Logger logger = context.getLogger(PatternVariableResolverTest.class);
        logger.info("This is a test");
        final List<String> messages = listAppender.getMessages();
        assertTrue(messages != null && !messages.isEmpty(), "No messages returned");
        final String message = messages.get(0);
        System.out.println(message);
    }
}
