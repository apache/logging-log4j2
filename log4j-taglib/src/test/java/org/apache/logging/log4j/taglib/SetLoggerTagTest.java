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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockPageContext;

/**
 *
 */
class SetLoggerTagTest {
    private MockPageContext context;
    private SetLoggerTag tag;

    @BeforeEach
    void setUp() {
        this.context = new MockPageContext();
        this.tag = new SetLoggerTag();
        this.tag.setPageContext(this.context);
    }

    @Test
    void testDoEndTagLoggerVarPageScope() throws Exception {
        this.tag.setLogger(LogManager.getLogger("testDoEndTagLoggerVarPageScope"));

        this.tag.setVar("helloWorld");

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should still be null.");

        final Object attribute = this.context.getAttribute("helloWorld", PageContext.PAGE_SCOPE);
        assertNotNull(attribute, "The attribute should not be null.");
        assertInstanceOf(Log4jTaglibLogger.class, attribute, "The attribute should be a Log4jTaglibLogger.");

        final Log4jTaglibLogger logger = (Log4jTaglibLogger) attribute;
        assertEquals("testDoEndTagLoggerVarPageScope", logger.getName(), "The logger name is not correct.");
    }

    @Test
    void testDoEndTagStringVarPageScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringVarPageScope");

        this.tag.setVar("goodbyeCruelWorld");

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should still be null.");

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.PAGE_SCOPE);
        assertNotNull(attribute, "The attribute should not be null.");
        assertInstanceOf(Log4jTaglibLogger.class, attribute, "The attribute should be a Log4jTaglibLogger.");

        final Log4jTaglibLogger logger = (Log4jTaglibLogger) attribute;
        assertEquals("testDoEndTagStringVarPageScope", logger.getName(), "The logger name is not correct.");
    }

    @Test
    void testDoEndTagStringFactoryVarPageScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringFactoryVarPageScope");

        final MessageFactory factory = new StringFormatterMessageFactory();

        this.tag.setFactory(factory);
        this.tag.setVar("goodbyeCruelWorld");

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should still be null.");

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.PAGE_SCOPE);
        assertNotNull(attribute, "The attribute should not be null.");
        assertInstanceOf(Log4jTaglibLogger.class, attribute, "The attribute should be a Log4jTaglibLogger.");

        final Log4jTaglibLogger logger = (Log4jTaglibLogger) attribute;
        assertEquals("testDoEndTagStringFactoryVarPageScope", logger.getName(), "The logger name is not correct.");
        checkMessageFactory("The message factory is not correct.", factory, logger);
    }

    @Test
    void testDoEndTagLoggerVarSessionScope() throws Exception {
        this.tag.setLogger(LogManager.getLogger("testDoEndTagLoggerVarSessionScope"));

        this.tag.setVar("helloWorld");
        this.tag.setScope("session");

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should still be null.");

        final Object attribute = this.context.getAttribute("helloWorld", PageContext.SESSION_SCOPE);
        assertNotNull(attribute, "The attribute should not be null.");
        assertInstanceOf(Log4jTaglibLogger.class, attribute, "The attribute should be a Log4jTaglibLogger.");

        final Log4jTaglibLogger logger = (Log4jTaglibLogger) attribute;
        assertEquals("testDoEndTagLoggerVarSessionScope", logger.getName(), "The logger name is not correct.");
    }

    @Test
    void testDoEndTagStringVarRequestScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringVarRequestScope");

        this.tag.setVar("goodbyeCruelWorld");
        this.tag.setScope("request");

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should still be null.");

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.REQUEST_SCOPE);
        assertNotNull(attribute, "The attribute should not be null.");
        assertInstanceOf(Log4jTaglibLogger.class, attribute, "The attribute should be a Log4jTaglibLogger.");

        final Log4jTaglibLogger logger = (Log4jTaglibLogger) attribute;
        assertEquals("testDoEndTagStringVarRequestScope", logger.getName(), "The logger name is not correct.");
    }

    @Test
    void testDoEndTagStringFactoryVarApplicationScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringFactoryVarApplicationScope");

        final MessageFactory factory = new StringFormatterMessageFactory();

        this.tag.setFactory(factory);
        this.tag.setVar("goodbyeCruelWorld");
        this.tag.setScope("application");

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");
        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should still be null.");

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.APPLICATION_SCOPE);
        assertNotNull(attribute, "The attribute should not be null.");
        assertInstanceOf(Log4jTaglibLogger.class, attribute, "The attribute should be a Log4jTaglibLogger.");

        final Log4jTaglibLogger logger = (Log4jTaglibLogger) attribute;
        assertEquals(
                "testDoEndTagStringFactoryVarApplicationScope", logger.getName(), "The logger name is not correct.");
        checkMessageFactory("The message factory is not correct.", factory, logger);
    }

    private static void checkMessageFactory(
            final String msg, final MessageFactory messageFactory1, final Logger testLogger1) {
        if (messageFactory1 == null) {
            assertEquals(
                    AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS,
                    testLogger1.getMessageFactory().getClass(),
                    msg);
        } else {
            MessageFactory actual = testLogger1.getMessageFactory();
            if (actual instanceof MessageFactory2Adapter) {
                actual = ((MessageFactory2Adapter) actual).getOriginal();
            }
            assertEquals(messageFactory1, actual, msg);
        }
    }

    @Test
    void testDoEndTagLoggerDefault() throws Exception {
        this.tag.setLogger(LogManager.getLogger("testDoEndTagLoggerDefault"));

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");

        final Log4jTaglibLogger logger = TagUtils.getDefaultLogger(this.context);
        assertNotNull(logger, "The default logger should not be null anymore.");
        assertEquals("testDoEndTagLoggerDefault", logger.getName(), "The logger name is not correct.");
    }

    @Test
    void testDoEndTagStringDefault() throws Exception {
        this.tag.setLogger("testDoEndTagStringDefault");

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");

        final Log4jTaglibLogger logger = TagUtils.getDefaultLogger(this.context);
        assertNotNull(logger, "The default logger should not be null anymore.");
        assertEquals("testDoEndTagStringDefault", logger.getName(), "The logger name is not correct.");
    }

    @Test
    void testDoEndTagStringFactoryDefault() throws Exception {
        this.tag.setLogger("testDoEndTagStringFactoryDefault");

        final MessageFactory factory = new StringFormatterMessageFactory();

        this.tag.setFactory(factory);

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");
        assertEquals(Tag.EVAL_PAGE, this.tag.doEndTag(), "The return value is not correct.");

        final Log4jTaglibLogger logger = TagUtils.getDefaultLogger(this.context);
        assertNotNull(logger, "The default logger should not be null anymore.");
        assertEquals("testDoEndTagStringFactoryDefault", logger.getName(), "The logger name is not correct.");
        checkMessageFactory("The message factory is not correct.", factory, logger);
    }
}
