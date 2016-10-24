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

package org.apache.logging.log4j.core.layout;

import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.lookup.MainMapLookup;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests LOG4j2-962.
 */
public class PatternLayoutMainMapLookupTest {

    static {
        // Must be set before Log4j writes the header to the appenders.
        MainMapLookup.setMainArguments("value0", "value1", "value2");
    }

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule("log4j2-962.xml");

    @Test
    public void testFileName() {
        final FileAppender fileApp = (FileAppender) context.getRequiredAppender("File");
        final String name = fileApp.getFileName();
        Assert.assertEquals("target/value0.log", name);
    }

    @Test
    public void testHeader() {
        final ListAppender listApp = context.getListAppender("List");
        final Logger logger = context.getLogger(this.getClass().getName());
        logger.info("Hello World");
        final List<String> messages = listApp.getMessages();
        Assert.assertFalse(messages.isEmpty());
        final String messagesStr = messages.toString();
        Assert.assertEquals(messagesStr, "Header: value0", messages.get(0));
        listApp.stop();
        Assert.assertEquals(messagesStr, "Footer: value1", messages.get(2));
    }

}
