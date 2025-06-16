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
package org.apache.logging.log4j.core.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.management.ObjectName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the Server class.
 */
class ServerTest {

    @Test
    void testEscapeQuotesButDoesNotEscapeEquals() throws Exception {
        final String ctx = "WebAppClassLoader=1320771902@4eb9613e"; // LOG4J2-492
        final String ctxName = Server.escape(ctx);
        assertEquals("\"WebAppClassLoader=1320771902@4eb9613e\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeQuotesButDoesNotEscapeComma() throws Exception {
        final String ctx = "a,b,c";
        final String ctxName = Server.escape(ctx);
        assertEquals("\"a,b,c\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeQuotesButDoesNotEscapeColon() throws Exception {
        final String ctx = "a:b:c";
        final String ctxName = Server.escape(ctx);
        assertEquals("\"a:b:c\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeQuotesAndEscapesQuestion() throws Exception {
        final String ctx = "a?c";
        final String ctxName = Server.escape(ctx);
        assertEquals("\"a\\?c\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeQuotesAndEscapesStar() throws Exception {
        final String ctx = "a*c";
        final String ctxName = Server.escape(ctx);
        assertEquals("\"a\\*c\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeQuotesAndEscapesBackslash() throws Exception {
        final String ctx = "a\\c";
        final String ctxName = Server.escape(ctx);
        assertEquals("\"a\\\\c\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeQuotesAndEscapesQuote() throws Exception {
        final String ctx = "a\"c";
        final String ctxName = Server.escape(ctx);
        assertEquals("\"a\\\"c\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeIgnoresSpaces() throws Exception {
        final String ctx = "a c";
        final String ctxName = Server.escape(ctx);
        assertEquals("a c", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeEscapesLineFeed() throws Exception {
        final String ctx = "a\rc";
        final String ctxName = Server.escape(ctx);
        assertEquals("ac", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }

    @Test
    void testEscapeEscapesCarriageReturn() throws Exception {
        final String ctx = "a\nc";
        final String ctxName = Server.escape(ctx);
        assertEquals("\"a\\nc\"", ctxName);
        new ObjectName(String.format(LoggerContextAdminMBean.PATTERN, ctxName));
        // no MalformedObjectNameException = success
    }
}
