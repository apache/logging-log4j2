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
package org.apache.log4j;

import junit.framework.TestCase;
import org.apache.log4j.spi.LoggingEvent;

/**
 * Tests for Layout.
 *
 */
public class LayoutTest extends TestCase {

    /**
     * Concrete Layout class for tests.
     */
    private static final class MockLayout extends Layout {
        /**
         * {@inheritDoc}
         */
        public void activateOptions() {}

        /**
         * {@inheritDoc}
         */
        public String format(final LoggingEvent event) {
            return "Mock";
        }

        /**
         * {@inheritDoc}
         */
        public boolean ignoresThrowable() {
            return true;
        }
    }

    /**
     * Expected content type.
     */
    private final String contentType;

    /**
     * Expected value for ignoresThrowable.
     */
    private final boolean ignoresThrowable;

    /**
     * Expected value for header.
     */
    private final String header;

    /**
     * Expected value for footer.
     */
    private final String footer;

    /**
     * Construct a new instance of LayoutTest.
     *
     * @param testName test name.
     */
    public LayoutTest(final String testName) {
        super(testName);
        contentType = "text/plain";
        ignoresThrowable = true;
        header = null;
        footer = null;
    }

    /**
     * Constructor for use by derived tests.
     *
     * @param testName name of test.
     * @param expectedContentType expected value for getContentType().
     * @param expectedIgnoresThrowable expected value for ignoresThrowable().
     * @param expectedHeader expected value for getHeader().
     * @param expectedFooter expected value for getFooter().
     */
    protected LayoutTest(
            final String testName,
            final String expectedContentType,
            final boolean expectedIgnoresThrowable,
            final String expectedHeader,
            final String expectedFooter) {
        super(testName);
        contentType = expectedContentType;
        ignoresThrowable = expectedIgnoresThrowable;
        header = expectedHeader;
        footer = expectedFooter;
    }

    /**
     * Creates layout for test.
     *
     * @return new instance of Layout.
     */
    protected Layout createLayout() {
        return new MockLayout();
    }

    /**
     * Tests format.
     *
     * @throws Exception derived tests, particular XMLLayoutTest, may throw exceptions.
     */
    public void testFormat() throws Exception {
        final Logger logger = Logger.getLogger("org.apache.log4j.LayoutTest");
        final LoggingEvent event =
                new LoggingEvent("org.apache.log4j.Logger", logger, Level.INFO, "Hello, World", null);
        final String result = createLayout().format(event);
        assertEquals("Mock", result);
    }

    /**
     * Tests getContentType.
     */
    public void testGetContentType() {
        assertEquals(contentType, createLayout().getContentType());
    }

    /**
     * Tests getFooter.
     */
    public void testGetFooter() {
        assertEquals(footer, createLayout().getFooter());
    }

    /**
     * Tests getHeader.
     */
    public void testGetHeader() {
        assertEquals(header, createLayout().getHeader());
    }

    /**
     * Tests ignoresThrowable.
     */
    public void testIgnoresThrowable() {
        assertEquals(ignoresThrowable, createLayout().ignoresThrowable());
    }

    /**
     * Tests Layout.LINE_SEP.
     */
    public void testLineSep() {
        assertEquals(System.getProperty("line.separator"), Layout.LINE_SEP);
    }

    /**
     * Tests Layout.LINE_SEP.
     */
    public void testLineSepLen() {
        assertEquals(Layout.LINE_SEP.length(), Layout.LINE_SEP_LEN);
    }
}
