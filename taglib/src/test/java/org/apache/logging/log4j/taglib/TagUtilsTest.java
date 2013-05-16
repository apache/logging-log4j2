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

import org.apache.logging.log4j.Level;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TagUtilsTest {
    @Test
    public void testGetScopeSession() {
        assertEquals("The scope is not correct.", PageContext.SESSION_SCOPE, TagUtils.getScope("session"));
    }

    @Test
    public void testGetScopeRequest() {
        assertEquals("The scope is not correct.", PageContext.REQUEST_SCOPE, TagUtils.getScope("request"));
    }

    @Test
    public void testGetScopeApplication() {
        assertEquals("The scope is not correct.", PageContext.APPLICATION_SCOPE, TagUtils.getScope("application"));
    }

    @Test
    public void testGetScopePage() {
        assertEquals("The scope is not correct.", PageContext.PAGE_SCOPE, TagUtils.getScope("page"));
    }

    @Test
    public void testGetScopeUnknown() {
        assertEquals("The scope is not correct.", PageContext.PAGE_SCOPE, TagUtils.getScope("oeu1209"));
    }

    @Test
    public void testResolveLevelTrace01() {
        assertEquals("The value is not correct.", Level.TRACE, TagUtils.resolveLevel("trace"));
    }

    @Test
    public void testResolveLevelTrace02() {
        assertEquals("The value is not correct.", Level.TRACE, TagUtils.resolveLevel(Level.TRACE));
    }

    @Test
    public void testResolveLevelDebug01() {
        assertEquals("The value is not correct.", Level.DEBUG, TagUtils.resolveLevel("debug"));
    }

    @Test
    public void testResolveLevelDebug02() {
        assertEquals("The value is not correct.", Level.DEBUG, TagUtils.resolveLevel(Level.DEBUG));
    }

    @Test
    public void testResolveLevelInfo01() {
        assertEquals("The value is not correct.", Level.INFO, TagUtils.resolveLevel("info"));
    }

    @Test
    public void testResolveLevelInfo02() {
        assertEquals("The value is not correct.", Level.INFO, TagUtils.resolveLevel(Level.INFO));
    }

    @Test
    public void testResolveLevelWarn01() {
        assertEquals("The value is not correct.", Level.WARN, TagUtils.resolveLevel("warn"));
    }

    @Test
    public void testResolveLevelWarn02() {
        assertEquals("The value is not correct.", Level.WARN, TagUtils.resolveLevel(Level.WARN));
    }

    @Test
    public void testResolveLevelError01() {
        assertEquals("The value is not correct.", Level.ERROR, TagUtils.resolveLevel("error"));
    }

    @Test
    public void testResolveLevelError02() {
        assertEquals("The value is not correct.", Level.ERROR, TagUtils.resolveLevel(Level.ERROR));
    }

    @Test
    public void testResolveLevelFatal01() {
        assertEquals("The value is not correct.", Level.FATAL, TagUtils.resolveLevel("fatal"));
    }

    @Test
    public void testResolveLevelFatal02() {
        assertEquals("The value is not correct.", Level.FATAL, TagUtils.resolveLevel(Level.FATAL));
    }
}
