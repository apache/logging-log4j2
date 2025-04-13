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

import javax.servlet.jsp.tagext.Tag;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.test.junit.LoggerContextSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockPageContext;

/**
 *
 */
@LoggerContextSource("log4j-test1.xml")
public class IfEnabledTagTest {

    private final Logger logger;
    private IfEnabledTag tag;

    public IfEnabledTagTest(final LoggerContext context) {
        this.logger = context.getLogger("IfEnabledTagTest");
    }

    @BeforeEach
    public void setUp() {
        this.tag = new IfEnabledTag();
        this.tag.setPageContext(new MockPageContext());
        this.tag.setLogger(this.logger);
    }

    @Test
    public void testDoStartTagEnabledString() throws Exception {
        this.tag.setLevel("warn");

        assertEquals(Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag(), "The return value is not correct.");
    }

    @Test
    public void testDoStartTagEnabledLevel() throws Exception {
        this.tag.setLevel(Level.WARN);

        assertEquals(Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag(), "The return value is not correct.");
    }

    @Test
    public void testDoStartTagEnabledStringMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("E01"));
        this.tag.setLevel("error");

        assertEquals(Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag(), "The return value is not correct.");
    }

    @Test
    public void testDoStartTagEnabledLevelMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("F02"));
        this.tag.setLevel(Level.ERROR);

        assertEquals(Tag.EVAL_BODY_INCLUDE, this.tag.doStartTag(), "The return value is not correct.");
    }

    @Test
    public void testDoStartTagDisabledString() throws Exception {
        this.tag.setLevel("info");

        assertEquals(Tag.SKIP_BODY, this.tag.doStartTag(), "The return value is not correct.");
    }

    @Test
    public void testDoStartTagDisabledLevel() throws Exception {
        this.tag.setLevel(Level.INFO);

        assertEquals(Tag.SKIP_BODY, this.tag.doStartTag(), "The return value is not correct.");
    }

    @Test
    public void testDoStartTagDisabledStringMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("E01"));
        this.tag.setLevel("trace");

        assertEquals(Tag.SKIP_BODY, this.tag.doStartTag(), "The return value is not correct.");
    }

    @Test
    public void testDoStartTagDisabledLevelMarker() throws Exception {
        this.tag.setMarker(MarkerManager.getMarker("F02"));
        this.tag.setLevel(Level.TRACE);

        assertEquals(Tag.SKIP_BODY, this.tag.doStartTag(), "The return value is not correct.");
    }
}
