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

import org.apache.logging.log4j.Level;
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
public class CatchingTagTest {
    private static final String CONFIG = "log4j-test1.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private final Logger logger = context.getLogger("LoggingMessageTagSupportTestLogger");
    private CatchingTag tag;

    @Before
    public void setUp() {
        this.tag = new CatchingTag();
        this.tag.setPageContext(new MockPageContext());
        this.tag.setLogger(this.logger);
    }

    @Test
    public void testDoEndTag() throws Exception {
        this.tag.setException(new Exception("This is a test."));

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Catching ERROR M-CATCHING[ EXCEPTION ] E java.lang.Exception: This is a test.");
    }

    @Test
    public void testDoEndTagLevelString() throws Exception {
        this.tag.setLevel("info");
        this.tag.setException(new RuntimeException("This is another test."));

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Catching INFO M-CATCHING[ EXCEPTION ] E java.lang.RuntimeException: This is another test.");
    }

    @Test
    public void testDoEndTagLevelObject() throws Exception {
        this.tag.setLevel(Level.WARN);
        this.tag.setException(new Error("This is the last test."));

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Catching WARN M-CATCHING[ EXCEPTION ] E java.lang.Error: This is the last test.");
    }

    private void verify(final String expected) {
        final ListAppender listApp = context.getListAppender("List");
        final List<String> events = listApp.getMessages();
        try
        {
            assertEquals("Incorrect number of messages.", 1, events.size());
            assertEquals("Incorrect message.", "o.a.l.l.t.CatchingTagTest " + expected, events.get(0));
        }
        finally
        {
            listApp.clear();
        }
    }
}
