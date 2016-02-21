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

import javax.servlet.jsp.tagext.Tag;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.junit.LoggerContextRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.mock.web.MockPageContext;

import static org.junit.Assert.*;

/**
 *
 */
public class IfEnabledTagTest {
    private static final String CONFIG = "log4j-test1.xml";

    @ClassRule
    public static LoggerContextRule context = new LoggerContextRule(CONFIG);

    private final Logger logger = context.getLogger("IfEnabledTagTest");
    private IfEnabledTag tag;

    @Before
    public void setUp() {
        this.tag = new IfEnabledTag();
        this.tag.setPageContext(new MockPageContext());
        this.tag.setLogger(this.logger);
    }

    @Test
    public void testDoStartTagEnabledString() throws Exception {
        this.tag.setLevel("warn");

        assertEquals("The return value is not correct.", Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag());
    }

    @Test
    public void testDoStartTagEnabledLevel() throws Exception {
        this.tag.setLevel(Level.WARN);

        assertEquals("The return value is not correct.", Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag());
    }

    @Test
    public void testDoStartTagEnabledStringMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("E01"));
        this.tag.setLevel("error");

        assertEquals("The return value is not correct.", Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag());
    }

    @Test
    public void testDoStartTagEnabledLevelMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("F02"));
        this.tag.setLevel(Level.ERROR);

        assertEquals("The return value is not correct.", Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag());
    }

    @Test
    public void testDoStartTagDisabledString() throws Exception {
        this.tag.setLevel("info");

        assertEquals("The return value is not correct.", Tag.SKIP_BODY, this.tag.doStartTag());
    }

    @Test
    public void testDoStartTagDisabledLevel() throws Exception {
        this.tag.setLevel(Level.INFO);

        assertEquals("The return value is not correct.", Tag.SKIP_BODY, this.tag.doStartTag());
    }

    @Test
    public void testDoStartTagDisabledStringMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("E01"));
        this.tag.setLevel("trace");

        assertEquals("The return value is not correct.", Tag.SKIP_BODY, this.tag.doStartTag());
    }

    @Test
    public void testDoStartTagDisabledLevelMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("F02"));
        this.tag.setLevel(Level.TRACE);

        assertEquals("The return value is not correct.", Tag.SKIP_BODY, this.tag.doStartTag());
    }
}
