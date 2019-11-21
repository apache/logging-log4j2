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

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockJspWriter;
import org.springframework.mock.web.MockPageContext;

import static org.junit.Assert.*;

/**
 *
 */
public class DumpTagTest {
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private Writer writer;
    private ByteArrayOutputStream output;
    private MockPageContext context;
    private DumpTag tag;

    @Before
    public void setUp() {
        this.output = new ByteArrayOutputStream();
        this.writer = new OutputStreamWriter(this.output, UTF8);

        this.context = new MockPageContext() {
            private final MockJspWriter jspWriter = new MockJspWriter(writer);
            @Override
            public JspWriter getOut() {
                return this.jspWriter;
            }
        };

        this.tag = new DumpTag();
        this.tag.setPageContext(this.context);
    }

    @Test
    public void testDoEndTagDefaultPageScopeNoAttributes() throws Exception {
        final int returnValue = this.tag.doEndTag();
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, returnValue);

        this.writer.flush();
        final String output = new String(this.output.toByteArray(), UTF8);
        assertEquals("The output is not correct.", "<dl></dl>", output);
    }

    @Test
    public void testDoEndTagDefaultPageScope() throws Exception {
        this.context.setAttribute("testAttribute01", "testValue01", PageContext.PAGE_SCOPE);
        this.context.setAttribute("anotherAttribute02", "finalValue02", PageContext.PAGE_SCOPE);
        this.context.setAttribute("badAttribute03", "skippedValue03", PageContext.SESSION_SCOPE);

        final int returnValue = this.tag.doEndTag();
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, returnValue);

        this.writer.flush();
        final String output = new String(this.output.toByteArray(), UTF8);
        assertEquals("The output is not correct.",
                "<dl>" +
                        "<dt><code>testAttribute01</code></dt><dd><code>testValue01</code></dd>" +
                        "<dt><code>anotherAttribute02</code></dt><dd><code>finalValue02</code></dd>" +
                        "</dl>", output);
    }

    @Test
    public void testDoEndTagSessionScopeNoAttributes() throws Exception {
        this.context.setAttribute("badAttribute01", "skippedValue01", PageContext.PAGE_SCOPE);

        this.tag.setScope("session");
        final int returnValue = this.tag.doEndTag();
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, returnValue);

        this.writer.flush();
        final String output = new String(this.output.toByteArray(), UTF8);
        assertEquals("The output is not correct.", "<dl></dl>", output);
    }

    @Test
    public void testDoEndTagSessionScope() throws Exception {
        this.context.setAttribute("otherAttribute03", "lostValue03", PageContext.PAGE_SCOPE);
        this.context.setAttribute("coolAttribute01", "weirdValue01", PageContext.SESSION_SCOPE);
        this.context.setAttribute("testAttribute02", "testValue02", PageContext.SESSION_SCOPE);

        this.tag.setScope("session");
        final int returnValue = this.tag.doEndTag();
        assertEquals("The return value is not correct.", Tag.EVAL_PAGE, returnValue);

        this.writer.flush();
        final String output = new String(this.output.toByteArray(), UTF8);
        assertEquals("The output is not correct.",
                "<dl>" +
                        "<dt><code>coolAttribute01</code></dt><dd><code>weirdValue01</code></dd>" +
                        "<dt><code>testAttribute02</code></dt><dd><code>testValue02</code></dd>" +
                        "</dl>", output);
    }
}
