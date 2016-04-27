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

import java.io.Writer;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.apache.logging.log4j.test.appender.ListAppender;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.mock.web.MockBodyContent;
import org.springframework.mock.web.MockPageContext;

import static org.junit.Assert.*;

/**
 *
 */
public class LoggingMessageTagSupportTest {
    private static final String CONFIG = "log4j-test1.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private final Logger logger = context.getLogger("LoggingMessageTagSupportTestLogger");
    private LoggingMessageTagSupport tag;

    private void setUp(final Level level) {
        this.tag = new LoggingMessageTagSupport() {
            private static final long serialVersionUID = 1L;

            @Override
            protected Level getLevel() {
                return level;
            }
        };
        this.tag.setPageContext(new MockPageContext());
        this.tag.setLogger(this.logger);
    }

    @Test
    public void testDoStartTag() {
        this.setUp(null);

        assertEquals("The return value is not correct.", BodyTag.EVAL_BODY_BUFFERED, this.tag.doStartTag());
    }

    @Test
    public void testMessageString() throws Exception {
        this.setUp(null);

        this.tag.setMessage("This is my message 01.");

        assertEquals("The message is not correct.", "This is my message 01.", this.tag.getMessage());
    }

    @Test
    public void testMessageObject() throws Exception {
        this.setUp(null);

        final Object message = new Object();
        this.tag.setMessage(message);

        assertSame("The message is not correct.", message, this.tag.getMessage());
    }

    @Test
    public void testMessageBody() throws Exception {
        this.setUp(null);

        final MockBodyContent content = new MockBodyContent("This is the body content 01.", (Writer)null);
        this.tag.setBodyContent(content);

        assertEquals("The message is not correct.", "This is the body content 01.", this.tag.getMessage());
    }

    @Test
    public void testMessageStringBodyIgnored() throws Exception {
        this.setUp(null);

        final MockBodyContent content = new MockBodyContent("This is more body content 02.", (Writer)null);
        this.tag.setBodyContent(content);
        this.tag.setMessage("This is another message 02.");

        assertEquals("The message is not correct.", "This is another message 02.", this.tag.getMessage());
    }

    @Test
    public void testDoEndTagStringMessageNoMarkerNoException() throws Exception {
        this.setUp(Level.WARN);

        this.tag.setMessage("Hello message for testDoEndTagStringMessageNoMarkerNoException");

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Hello message for testDoEndTagStringMessageNoMarkerNoException WARN M- E");
    }

    @Test
    public void testDoEndTagStringMessageMarkerNoException() throws Exception {
        this.setUp(Level.INFO);

        this.tag.setMarker(MarkerManager.getMarker("E01"));
        this.tag.setMessage("Goodbye message for testDoEndTagStringMessageMarkerNoException");

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Goodbye message for testDoEndTagStringMessageMarkerNoException INFO M-E01 E");
    }

    @Test
    public void testDoEndTagStringMessageNoMarkerException() throws Exception {
        this.setUp(Level.ERROR);

        this.tag.setException(new Exception("This is a test"));
        this.tag.setMessage("Another message for testDoEndTagStringMessageNoMarkerException");

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Another message for testDoEndTagStringMessageNoMarkerException ERROR M- E java.lang.Exception: This is a test");
    }

    @Test
    public void testDoEndTagStringMessageMarkerException() throws Exception {
        this.setUp(Level.TRACE);

        this.tag.setException(new RuntimeException("This is another test"));
        this.tag.setMarker(MarkerManager.getMarker("F02"));
        this.tag.setMessage("Final message for testDoEndTagStringMessageMarkerException");

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Final message for testDoEndTagStringMessageMarkerException TRACE M-F02 E java.lang.RuntimeException: This is another test");
    }

    @Test
    public void testDoEndTagStringWithParameters() throws Exception {
        this.setUp(Level.FATAL);

        this.tag.setDynamicAttribute(null, null, "A");
        this.tag.setDynamicAttribute(null, null, TimeUnit.HOURS);
        this.tag.setMessage("Test message with [{}] parameter of [{}]");

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Test message with [A] parameter of [HOURS] FATAL M- E");

    }

    @Test
    public void testDoEndTagStringWithParametersMarkerAndException() throws Exception {
        this.setUp(Level.DEBUG);

        this.tag.setException(new Error("This is the last test"));
        this.tag.setMarker(MarkerManager.getMarker("N03"));
        this.tag.setDynamicAttribute(null, null, "Z");
        this.tag.setDynamicAttribute(null, null, TimeUnit.SECONDS);
        this.tag.setMessage("Final message with [{}] parameter of [{}]");

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Final message with [Z] parameter of [SECONDS] DEBUG M-N03 E java.lang.Error: This is the last test");

    }

    @Test
    public void testDoEndTagMessageNoMarkerNoException() throws Exception {
        this.setUp(Level.INFO);

        this.tag.setMessage(
                logger.getMessageFactory().newMessage("First message for testDoEndTagMessageNoMarkerNoException")
        );

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("First message for testDoEndTagMessageNoMarkerNoException INFO M- E");
    }

    @Test
    public void testDoEndTagMessageMarkerNoException() throws Exception {
        this.setUp(Level.WARN);

        this.tag.setMarker(MarkerManager.getMarker("E01"));
        this.tag.setMessage(
                logger.getMessageFactory().newMessage("Another message for testDoEndTagMessageMarkerNoException")
        );

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Another message for testDoEndTagMessageMarkerNoException WARN M-E01 E");
    }

    @Test
    public void testDoEndTagMessageNoMarkerException() throws Exception {
        this.setUp(Level.TRACE);

        this.tag.setException(new Exception("This is a test"));
        this.tag.setMessage(
                logger.getMessageFactory().newMessage("Third message for testDoEndTagMessageNoMarkerException")
        );

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Third message for testDoEndTagMessageNoMarkerException TRACE M- E java.lang.Exception: This is a test");
    }

    @Test
    public void testDoEndTagMessageMarkerException() throws Exception {
        this.setUp(Level.ERROR);

        this.tag.setException(new RuntimeException("This is another test"));
        this.tag.setMarker(MarkerManager.getMarker("F02"));
        this.tag.setMessage(
                logger.getMessageFactory().newMessage("Final message for testDoEndTagMessageMarkerException")
        );

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Final message for testDoEndTagMessageMarkerException ERROR M-F02 E java.lang.RuntimeException: " +
                "This is another test");
    }

    @Test
    public void testDoEndTagObjectNoMarkerNoException() throws Exception {
        this.setUp(Level.INFO);

        this.tag.setMessage(new MyMessage("First message for testDoEndTagObjectNoMarkerNoException"));

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("First message for testDoEndTagObjectNoMarkerNoException INFO M- E");
    }

    @Test
    public void testDoEndTagObjectMarkerNoException() throws Exception {
        this.setUp(Level.WARN);

        this.tag.setMarker(MarkerManager.getMarker("E01"));
        this.tag.setMessage(new MyMessage("Another message for testDoEndTagObjectMarkerNoException"));

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Another message for testDoEndTagObjectMarkerNoException WARN M-E01 E");
    }

    @Test
    public void testDoEndTagObjectNoMarkerException() throws Exception {
        this.setUp(Level.TRACE);

        this.tag.setException(new Exception("This is a test"));
        this.tag.setMessage(new MyMessage("Third message for testDoEndTagObjectNoMarkerException"));

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Third message for testDoEndTagObjectNoMarkerException TRACE M- E java.lang.Exception: This is a test");
    }

    @Test
    public void testDoEndTagObjectMarkerException() throws Exception {
        this.setUp(Level.ERROR);

        this.tag.setException(new RuntimeException("This is another test"));
        this.tag.setMarker(MarkerManager.getMarker("F02"));
        this.tag.setMessage(new MyMessage("Final message for testDoEndTagObjectMarkerException"));

        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        verify("Final message for testDoEndTagObjectMarkerException ERROR M-F02 E java.lang.RuntimeException: " +
                "This is another test");
    }

    private void verify(final String expected) {
        final ListAppender listApp = context.getListAppender("List");
        final List<String> events = listApp.getMessages();
        try
        {
            assertEquals("Incorrect number of messages.", 1, events.size());
            assertEquals("Incorrect message.", "o.a.l.l.t.LoggingMessageTagSupportTest " + expected, events.get(0));
        }
        finally
        {
            listApp.clear();
        }
    }

    private static class MyMessage {
        private final String internal;

        public MyMessage(final String internal) {
            this.internal = internal;
        }

        @Override
        public String toString() {
            return this.internal;
        }
    }
}
