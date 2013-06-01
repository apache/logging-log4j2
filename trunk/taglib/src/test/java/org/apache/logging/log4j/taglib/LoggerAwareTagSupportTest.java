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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.junit.Test;
import org.springframework.mock.web.MockPageContext;

import static org.junit.Assert.*;

/**
 *
 */
public class LoggerAwareTagSupportTest {
    private MockPageContext context;
    private LoggerAwareTagSupport tag;

    private void setUp(final Object page) {
        this.context = new MockPageContext() {
            @Override
            public Object getPage() {
                return page == null ? this : page;
            }
        };
        this.tag = new LoggerAwareTagSupport() {
            private static final long serialVersionUID = 1L;
        };
        this.tag.setPageContext(this.context);
    }

    @Test
    public void testGetLoggerWithGenericLogger() throws Exception {
        this.setUp(null);

        final Logger logger = LogManager.getLogger("testGetLoggerWithGenericLogger");

        this.tag.setLogger(logger);
        final Log4jTaglibLogger returned = this.tag.getLogger();

        assertNotNull("The first returned logger should not be null.", returned);
        assertNotSame("The first returned logger should not be the same as the set.", logger, returned);
        assertEquals("The name is not correct.", "testGetLoggerWithGenericLogger", returned.getName());

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull("The second returned logger should not be null.", returned2);
        assertSame("The second returned logger should be the same as the first.", returned, returned2);

        this.tag.release();

        final Log4jTaglibLogger returned3 = this.tag.getLogger();

        assertNotNull("The third returned logger should not be null.", returned3);
        assertNotSame("The third returned logger should not be the same as the first.", returned, returned3);
    }

    @Test
    public void testGetLoggerWithTaglibLogger() throws Exception {
        this.setUp(null);

        final AbstractLogger wrapped = (AbstractLogger)LogManager.getLogger("testGetLoggerWithTaglibLogger");
        final Log4jTaglibLogger logger = new Log4jTaglibLogger(wrapped, wrapped.getName(), wrapped.getMessageFactory());

        this.tag.setLogger(logger);
        Log4jTaglibLogger returned = this.tag.getLogger();

        assertNotNull("The first returned logger should not be null.", returned);
        assertSame("The first returned logger should be the same as the set.", logger, returned);
        assertEquals("The name is not correct.", "testGetLoggerWithTaglibLogger", returned.getName());

        returned = this.tag.getLogger();

        assertNotNull("The second returned logger should not be null.", returned);
        assertSame("The second returned logger should be the same as the set.", logger, returned);
    }

    @Test
    public void testGetLoggerWithStringLogger() throws Exception {
        this.setUp(null);

        this.tag.setLogger("testGetLoggerWithStringLogger");
        final Log4jTaglibLogger returned = this.tag.getLogger();

        assertNotNull("The first returned logger should not be null.", returned);
        assertEquals("The name is not correct.", "testGetLoggerWithStringLogger", returned.getName());

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull("The second returned logger should not be null.", returned2);
        assertSame("The second returned logger should be the same as the first.", returned, returned2);
    }

    @Test
    public void testGetDefaultLogger01() throws Exception {
        final Object page = new Object() {};
        this.setUp(page);

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));

        final Log4jTaglibLogger returned = this.tag.getLogger();
        assertNotNull("The first returned logger should not be null.", returned);
        assertEquals("The logger name is not correct.", page.getClass().getName(), returned.getName());

        final Log4jTaglibLogger defaultLogger = TagUtils.getDefaultLogger(this.context);
        assertNotNull("The default logger should not be null anymore.", defaultLogger);
        assertSame("The default logger should be the same as the returned logger.", returned, defaultLogger);

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull("The second returned logger should not be null.", returned2);
        assertSame("The second returned logger should be the same as the first.", returned, returned2);
    }

    @Test
    public void testGetDefaultLogger02() throws Exception {
        final Object page = new Object() {};
        this.setUp(page);

        assertNull("The default logger should be null.", TagUtils.getDefaultLogger(this.context));

        final Log4jTaglibLogger returned = this.tag.getLogger();
        assertNotNull("The first returned logger should not be null.", returned);
        assertEquals("The logger name is not correct.", page.getClass().getName(), returned.getName());

        final Log4jTaglibLogger defaultLogger = TagUtils.getDefaultLogger(this.context);
        assertNotNull("The default logger should not be null anymore.", defaultLogger);
        assertSame("The default logger should be the same as the returned logger.", returned, defaultLogger);

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull("The second returned logger should not be null.", returned2);
        assertSame("The second returned logger should be the same as the first.", returned, returned2);
    }
}
