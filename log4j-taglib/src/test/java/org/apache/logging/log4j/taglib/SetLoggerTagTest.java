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

import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFactory;
import org.apache.logging.log4j.message.StringFormatterMessageFactory;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.MessageFactory2Adapter;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockPageContext;

import static org.junit.Assert.*;

/**
 *
 */
public class SetLoggerTagTest {
    private MockPageContext context;
    private SetLoggerTag tag;

    @Before
    public void setUp() {
        this.context = new MockPageContext();
        this.tag = new SetLoggerTag();
        this.tag.setPageContext(this.context);
    }

    @Test
    public void testDoEndTagLoggerVarPageScope() throws Exception {
        this.tag.setLogger(LogManager.getLogger("testDoEndTagLoggerVarPageScope"));

        this.tag.setVar("helloWorld");

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        assertNull("The default logger should still be null.", TagUtils.getDefaultLogger(this.context));

        final Object attribute = this.context.getAttribute("helloWorld", PageContext.PAGE_SCOPE);
        assertNotNull("The attribute should not be null.", attribute);
        assertTrue("The attribute should be a Log4jTaglibLogger.", attribute instanceof Log4jTaglibLogger);

        final Log4jTaglibLogger logger = (Log4jTaglibLogger)attribute;
        assertEquals("The logger name is not correct.", "testDoEndTagLoggerVarPageScope", logger.getName());
    }

    @Test
    public void testDoEndTagStringVarPageScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringVarPageScope");

        this.tag.setVar("goodbyeCruelWorld");

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        assertNull("The default logger should still be null.", TagUtils.getDefaultLogger(this.context));

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.PAGE_SCOPE);
        assertNotNull("The attribute should not be null.", attribute);
        assertTrue("The attribute should be a Log4jTaglibLogger.", attribute instanceof Log4jTaglibLogger);

        final Log4jTaglibLogger logger = (Log4jTaglibLogger)attribute;
        assertEquals("The logger name is not correct.", "testDoEndTagStringVarPageScope", logger.getName());
    }

    @Test
    public void testDoEndTagStringFactoryVarPageScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringFactoryVarPageScope");

        final MessageFactory factory = new StringFormatterMessageFactory();

        this.tag.setFactory(factory);
        this.tag.setVar("goodbyeCruelWorld");

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        assertNull("The default logger should still be null.", TagUtils.getDefaultLogger(this.context));

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.PAGE_SCOPE);
        assertNotNull("The attribute should not be null.", attribute);
        assertTrue("The attribute should be a Log4jTaglibLogger.", attribute instanceof Log4jTaglibLogger);

        final Log4jTaglibLogger logger = (Log4jTaglibLogger)attribute;
        assertEquals("The logger name is not correct.", "testDoEndTagStringFactoryVarPageScope", logger.getName());
        checkMessageFactory("The message factory is not correct.", factory, logger);
    }

    @Test
    public void testDoEndTagLoggerVarSessionScope() throws Exception {
        this.tag.setLogger(LogManager.getLogger("testDoEndTagLoggerVarSessionScope"));

        this.tag.setVar("helloWorld");
        this.tag.setScope("session");

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        assertNull("The default logger should still be null.", TagUtils.getDefaultLogger(this.context));

        final Object attribute = this.context.getAttribute("helloWorld", PageContext.SESSION_SCOPE);
        assertNotNull("The attribute should not be null.", attribute);
        assertTrue("The attribute should be a Log4jTaglibLogger.", attribute instanceof Log4jTaglibLogger);

        final Log4jTaglibLogger logger = (Log4jTaglibLogger)attribute;
        assertEquals("The logger name is not correct.", "testDoEndTagLoggerVarSessionScope", logger.getName());
    }

    @Test
    public void testDoEndTagStringVarRequestScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringVarRequestScope");

        this.tag.setVar("goodbyeCruelWorld");
        this.tag.setScope("request");

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        assertNull("The default logger should still be null.", TagUtils.getDefaultLogger(this.context));

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.REQUEST_SCOPE);
        assertNotNull("The attribute should not be null.", attribute);
        assertTrue("The attribute should be a Log4jTaglibLogger.", attribute instanceof Log4jTaglibLogger);

        final Log4jTaglibLogger logger = (Log4jTaglibLogger)attribute;
        assertEquals("The logger name is not correct.", "testDoEndTagStringVarRequestScope", logger.getName());
    }

    @Test
    public void testDoEndTagStringFactoryVarApplicationScope() throws Exception {
        this.tag.setLogger("testDoEndTagStringFactoryVarApplicationScope");

        final MessageFactory factory = new StringFormatterMessageFactory();

        this.tag.setFactory(factory);
        this.tag.setVar("goodbyeCruelWorld");
        this.tag.setScope("application");

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());
        assertNull("The default logger should still be null.", TagUtils.getDefaultLogger(this.context));

        final Object attribute = this.context.getAttribute("goodbyeCruelWorld", PageContext.APPLICATION_SCOPE);
        assertNotNull("The attribute should not be null.", attribute);
        assertTrue("The attribute should be a Log4jTaglibLogger.", attribute instanceof Log4jTaglibLogger);

        final Log4jTaglibLogger logger = (Log4jTaglibLogger)attribute;
        assertEquals("The logger name is not correct.", "testDoEndTagStringFactoryVarApplicationScope",
                logger.getName());
        checkMessageFactory("The message factory is not correct.", factory, logger);
    }

    private static void checkMessageFactory(final String msg, final MessageFactory messageFactory1,
            final Logger testLogger1) {
        if (messageFactory1 == null) {
            assertEquals(msg, AbstractLogger.DEFAULT_MESSAGE_FACTORY_CLASS,
                    testLogger1.getMessageFactory().getClass());
        } else {
            MessageFactory actual = testLogger1.getMessageFactory();
            if (actual instanceof MessageFactory2Adapter) {
                actual = ((MessageFactory2Adapter) actual).getOriginal();
            }
            assertEquals(msg, messageFactory1, actual);
        }
    }

    @Test
    public void testDoEndTagLoggerDefault() throws Exception {
        this.tag.setLogger(LogManager.getLogger("testDoEndTagLoggerDefault"));

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());

        final Log4jTaglibLogger logger = TagUtils.getDefaultLogger(this.context);
        assertNotNull("The default logger should not be null anymore.", logger);
        assertEquals("The logger name is not correct.", "testDoEndTagLoggerDefault", logger.getName());
    }

    @Test
    public void testDoEndTagStringDefault() throws Exception {
        this.tag.setLogger("testDoEndTagStringDefault");

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());

        final Log4jTaglibLogger logger = TagUtils.getDefaultLogger(this.context);
        assertNotNull("The default logger should not be null anymore.", logger);
        assertEquals("The logger name is not correct.", "testDoEndTagStringDefault", logger.getName());
    }

    @Test
    public void testDoEndTagStringFactoryDefault() throws Exception {
        this.tag.setLogger("testDoEndTagStringFactoryDefault");

        final MessageFactory factory = new StringFormatterMessageFactory();

        this.tag.setFactory(factory);

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, this.tag.doEndTag());

        final Log4jTaglibLogger logger = TagUtils.getDefaultLogger(this.context);
        assertNotNull("The default logger should not be null anymore.", logger);
        assertEquals("The logger name is not correct.", "testDoEndTagStringFactoryDefault", logger.getName());
        checkMessageFactory("The message factory is not correct.", factory, logger);
    }
}
