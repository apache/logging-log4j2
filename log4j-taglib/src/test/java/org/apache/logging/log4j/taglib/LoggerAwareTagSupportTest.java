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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockPageContext;

/**
 *
 */
class LoggerAwareTagSupportTest {
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
    void testGetLoggerWithGenericLogger() throws Exception {
        this.setUp(null);

        final Logger logger = LogManager.getLogger("testGetLoggerWithGenericLogger");

        this.tag.setLogger(logger);
        final Log4jTaglibLogger returned = this.tag.getLogger();

        assertNotNull(returned, "The first returned logger should not be null.");
        assertNotSame(logger, returned, "The first returned logger should not be the same as the set.");
        assertEquals("testGetLoggerWithGenericLogger", returned.getName(), "The name is not correct.");

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull(returned2, "The second returned logger should not be null.");
        assertSame(returned, returned2, "The second returned logger should be the same as the first.");

        this.tag.release();

        final Log4jTaglibLogger returned3 = this.tag.getLogger();

        assertNotNull(returned3, "The third returned logger should not be null.");
        assertNotSame(returned, returned3, "The third returned logger should not be the same as the first.");
    }

    @Test
    void testGetLoggerWithTaglibLogger() throws Exception {
        this.setUp(null);

        final AbstractLogger wrapped = (AbstractLogger) LogManager.getLogger("testGetLoggerWithTaglibLogger");
        final Log4jTaglibLogger logger = new Log4jTaglibLogger(wrapped, wrapped.getName(), wrapped.getMessageFactory());

        this.tag.setLogger(logger);
        Log4jTaglibLogger returned = this.tag.getLogger();

        assertNotNull(returned, "The first returned logger should not be null.");
        assertSame(logger, returned, "The first returned logger should be the same as the set.");
        assertEquals("testGetLoggerWithTaglibLogger", returned.getName(), "The name is not correct.");

        returned = this.tag.getLogger();

        assertNotNull(returned, "The second returned logger should not be null.");
        assertSame(logger, returned, "The second returned logger should be the same as the set.");
    }

    @Test
    void testGetLoggerWithStringLogger() throws Exception {
        this.setUp(null);

        this.tag.setLogger("testGetLoggerWithStringLogger");
        final Log4jTaglibLogger returned = this.tag.getLogger();

        assertNotNull(returned, "The first returned logger should not be null.");
        assertEquals("testGetLoggerWithStringLogger", returned.getName(), "The name is not correct.");

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull(returned2, "The second returned logger should not be null.");
        assertSame(returned, returned2, "The second returned logger should be the same as the first.");
    }

    @Test
    void testGetDefaultLogger01() throws Exception {
        final Object page = new Object() {};
        this.setUp(page);

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");

        final Log4jTaglibLogger returned = this.tag.getLogger();
        assertNotNull(returned, "The first returned logger should not be null.");
        assertEquals(page.getClass().getName(), returned.getName(), "The logger name is not correct.");

        final Log4jTaglibLogger defaultLogger = TagUtils.getDefaultLogger(this.context);
        assertNotNull(defaultLogger, "The default logger should not be null anymore.");
        assertSame(returned, defaultLogger, "The default logger should be the same as the returned logger.");

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull(returned2, "The second returned logger should not be null.");
        assertSame(returned, returned2, "The second returned logger should be the same as the first.");
    }

    @Test
    void testGetDefaultLogger02() throws Exception {
        final Object page = new Object() {};
        this.setUp(page);

        assertNull(TagUtils.getDefaultLogger(this.context), "The default logger should be null.");

        final Log4jTaglibLogger returned = this.tag.getLogger();
        assertNotNull(returned, "The first returned logger should not be null.");
        assertEquals(page.getClass().getName(), returned.getName(), "The logger name is not correct.");

        final Log4jTaglibLogger defaultLogger = TagUtils.getDefaultLogger(this.context);
        assertNotNull(defaultLogger, "The default logger should not be null anymore.");
        assertSame(returned, defaultLogger, "The default logger should be the same as the returned logger.");

        final Log4jTaglibLogger returned2 = this.tag.getLogger();

        assertNotNull(returned2, "The second returned logger should not be null.");
        assertSame(returned, returned2, "The second returned logger should be the same as the first.");
    }
}
