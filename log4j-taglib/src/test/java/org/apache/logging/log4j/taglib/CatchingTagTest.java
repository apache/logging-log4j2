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

import static org.apache.logging.log4j.util.Strings.LINE_SEPARATOR;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import javax.servlet.jsp.tagext.Tag;
import org.apache.logging.log4j.Level;
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
public class CatchingTagTest {

    private final LoggerContext context;
    private final Logger logger;
    private CatchingTag tag;

    public CatchingTagTest(final LoggerContext context) {
        this.context = context;
        this.logger = context.getLogger("LoggingMessageTagSupportTestLogger");
    }

    @BeforeEach
    public void setUp() {
        this.tag = new CatchingTag();
        this.tag.setPageContext(new MockPageContext());
        this.tag.setLogger(this.logger);
    }

    @Test
    public void testDoEndTag() throws Exception {
        this.tag.setException(new Exception("This is a test."));

        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        verify("Catching ERROR M-CATCHING[ EXCEPTION ] E" + LINE_SEPARATOR + "java.lang.Exception: This is a test.");
    }

    @Test
    public void testDoEndTagLevelString() throws Exception {
        this.tag.setLevel("info");
        this.tag.setException(new RuntimeException("This is another test."));

        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        verify("Catching INFO M-CATCHING[ EXCEPTION ] E" + LINE_SEPARATOR
                + "java.lang.RuntimeException: This is another test.");
    }

    @Test
    public void testDoEndTagLevelObject() throws Exception {
        this.tag.setLevel(Level.WARN);
        this.tag.setException(new Error("This is the last test."));

        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        verify("Catching WARN M-CATCHING[ EXCEPTION ] E" + LINE_SEPARATOR + "java.lang.Error: This is the last test.");
    }

    private void verify(final String expected) {
        final ListAppender listApp = context.getConfiguration().getAppender("List");
        final List<String> events = listApp.getMessages();
        try {
            assertEquals(1, events.size(), "Incorrect number of messages.");
            assertEquals("o.a.l.l.t.CatchingTagTest " + expected + LINE_SEPARATOR, events.get(0), "Incorrect message.");
        } finally {
            listApp.clear();
        }
    }
}
