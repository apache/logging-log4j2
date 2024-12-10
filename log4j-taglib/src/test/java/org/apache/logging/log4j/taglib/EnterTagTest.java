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
package org.apache.logging.log4j.taglib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.servlet.jsp.tagext.Tag;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockPageContext;

/**
 *
 */
@LoggerContextSource("log4j-test1.xml")
public class EnterTagTest {

    private final LoggerContext context;
    private final Logger logger;
    private EntryTag tag;
    private static final String CONFIG = "log4j-test1.xml";

    public EnterTagTest(final LoggerContext context) {
        this.context = context;
        this.logger = context.getLogger("LoggingMessageTagSupportTestLogger");
    }

    @BeforeEach
    public void setUp() {
        this.tag = new EntryTag();
        this.tag.setPageContext(new MockPageContext());
        this.tag.setLogger(this.logger);
    }

    @Test
    public void testDoEndTag() throws Exception {
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        verify("Enter TRACE M-ENTER[ FLOW ] E");
    }

    @Test
    public void testDoEndTagAttributes() throws Exception {
        this.tag.setDynamicAttribute(null, null, CONFIG);
        this.tag.setDynamicAttribute(null, null, 5792);

        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        verify("Enter params(log4j-test1.xml, 5792) TRACE M-ENTER[ FLOW ] E");
    }

    private void verify(final String expected) {
        final ListAppender listApp = context.getConfiguration().getAppender("List");
        final List<String> events = listApp.getMessages();
        try {
            assertEquals(1, events.size(), "Incorrect number of messages.");
            assertEquals("o.a.l.l.t.EnterTagTest " + expected, events.get(0), "Incorrect message.");
        } finally {
            listApp.clear();
        }
    }
}
