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
package org.apache.logging.log4j.taglib;

import java.util.List;
import javax.servlet.jsp.tagext.Tag;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.mock.web.MockPageContext;

import static org.junit.Assert.*;

/**
 *
 */
public class EnterTagTest {
    private static final String CONFIG = "log4j-test1.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private final Logger logger = context.getLogger("LoggingMessageTagSupportTestLogger");
    private EntryTag tag;

    @Before
    public void setUp() {
        this.tag = new EntryTag();
        this.tag.setPageContext(new MockPageContext());
        this.tag.setLogger(this.logger);
    }

    @Test
    public void testDoEndTag() throws Exception {
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Enter TRACE M-ENTER[ FLOW ] E");
    }

    @Test
    public void testDoEndTagAttributes() throws Exception {
        this.tag.setDynamicAttribute(null, null, CONFIG);
        this.tag.setDynamicAttribute(null, null, 5792);

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Enter params(log4j-test1.xml, 5792) TRACE M-ENTER[ FLOW ] E");
    }

    private void verify(final String expected) {
        final ListAppender listApp = context.getListAppender("List");
        final List<String> events = listApp.getMessages();
        try
        {
            assertEquals("Incorrect number of messages.", 1, events.size());
            assertEquals("Incorrect message.", "o.a.l.l.t.EnterTagTest " + expected, events.get(0));
        }
        finally
        {
            listApp.clear();
        }
    }
}
